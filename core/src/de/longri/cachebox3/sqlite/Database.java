/* 
 * Copyright (C) 2016 team-cachebox.de
 *
 * Licensed under the : GNU General Public License (GPL);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.longri.cachebox3.sqlite;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.sql.SQLiteGdxDatabase;
import com.badlogic.gdx.sql.SQLiteGdxDatabaseCursor;
import com.badlogic.gdx.sql.SQLiteGdxDatabaseFactory;
import com.badlogic.gdx.sql.SQLiteGdxException;
import de.longri.cachebox3.types.Categories;
import de.longri.cachebox3.types.Category;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;


public class Database {
    protected final org.slf4j.Logger log;
    public static Database Data;
    public static Database FieldNotes;
    public static Database Settings;
    private SQLiteGdxDatabase myDB;
//	public CacheList Query;

    public enum DatabaseType {
        CacheBox, FieldNotes, Settings
    }

    protected DatabaseType databaseType;

    public Database(DatabaseType databaseType) {
        super();
        this.databaseType = databaseType;

        log = LoggerFactory.getLogger("Database." + databaseType);

        switch (databaseType) {
            case CacheBox:
                latestDatabaseChange = DatabaseVersions.LatestDatabaseChange;
//			Query = new CacheList();
                break;
            case FieldNotes:
                latestDatabaseChange = DatabaseVersions.LatestDatabaseFieldNoteChange;
                break;
            case Settings:
                latestDatabaseChange = DatabaseVersions.LatestDatabaseSettingsChange;
        }
    }


    protected FileHandle databasePath;

    protected boolean newDB = false;

    /***
     * Wenn die DB neu erstellt wurde ist der Return Wert bei der ersten Abfrage True
     *
     * @return
     */
    public boolean isDbNew() {
        return newDB;
    }


    public long DatabaseId = 0; // for Database replication with WinCachebox
    public long MasterDatabaseId = 0;
    protected int latestDatabaseChange = 0;


    public boolean StartUp(FileHandle databasePath) {
        this.databasePath = databasePath;

        Initialize();

        int databaseSchemeVersion = GetDatabaseSchemeVersion();
        log.debug("DatabaseSchemeVersion: " + databaseSchemeVersion);
        if (databaseSchemeVersion < latestDatabaseChange) {
            log.debug("Alter Database to SchemeVersion: " + latestDatabaseChange);
            AlterDatabase(databaseSchemeVersion);
            SetDatabaseSchemeVersion();
        }


        if (databaseType == DatabaseType.CacheBox) { // create or load DatabaseId for each
            DatabaseId = ReadConfigLong("DatabaseId");
            if (DatabaseId <= 0) {
                DatabaseId = new Date().getTime();
                WriteConfigLong("DatabaseId", DatabaseId);
            }
            // Read MasterDatabaseId. If MasterDatabaseId > 0 -> This database
            // is connected to the Replications Master of WinCB
            // In this case changes of Waypoints, Solvertext, Notes must be
            // noted in the Table Replication...
            MasterDatabaseId = ReadConfigLong("MasterDatabaseId");
        }
        return true;
    }


    private int GetDatabaseSchemeVersion() {
        int result = -1;
        SQLiteGdxDatabaseCursor c = null;
        try {
            c = rawQuery("select Value from Config where [Key] like ?", new String[]{"DatabaseSchemeVersionWin"});
        } catch (Exception exc) {
            return -1;
        }
        try {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                String databaseSchemeVersion = c.getString(0);
                result = Integer.parseInt(databaseSchemeVersion);
                c.moveToNext();
            }
        } catch (Exception exc) {
            result = -1;
        }
        if (c != null) {
            c.close();
        }

        return result;
    }

    private void SetDatabaseSchemeVersion() {
        Parameters val = new Parameters();
        val.put("Value", latestDatabaseChange);
        long anz = update("Config", val, "[Key] like 'DatabaseSchemeVersionWin'", null);
        if (anz <= 0) {
            // Update not possible because Key does not exist
            val.put("Key", "DatabaseSchemeVersionWin");
            insert("Config", val);
        }
        // for Compatibility with WinCB
        val.put("Value", latestDatabaseChange);
        anz = update("Config", val, "[Key] like 'DatabaseSchemeVersion'", null);
        if (anz <= 0) {
            // Update not possible because Key does not exist
            val.put("Key", "DatabaseSchemeVersion");
            insert("Config", val);
        }
    }

    public void WriteConfigString(String key, String value) {
        Parameters val = new Parameters();
        val.put("Value", value);
        long anz = update("Config", val, "[Key] like '" + key + "'", null);
        if (anz <= 0) {
            // Update not possible because Key does not exist
            val.put("Key", key);
            insert("Config", val);
        }
    }

    public void WriteConfigLongString(String key, String value) {
        Parameters val = new Parameters();
        val.put("LongString", value);
        long anz = update("Config", val, "[Key] like '" + key + "'", null);
        if (anz <= 0) {
            // Update not possible because Key does not exist
            val.put("Key", key);
            insert("Config", val);
        }
    }

    public String ReadConfigString(String key) throws Exception {
        String result = "";
        SQLiteGdxDatabaseCursor c = null;
        boolean found = false;
        try {
            c = rawQuery("select Value from Config where [Key] like ?", new String[]{key});
        } catch (Exception exc) {
            throw new Exception("not in DB");
        }
        try {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                result = c.getString(0);
                found = true;
                c.moveToNext();
            }
        } catch (Exception exc) {
            throw new Exception("not in DB");
        } finally {
            c.close();
        }

        if (!found)
            throw new Exception("not in DB");

        return result;
    }

    public String ReadConfigLongString(String key) throws Exception {
        String result = "";
        SQLiteGdxDatabaseCursor c = null;
        boolean found = false;
        try {
            c = rawQuery("select LongString from Config where [Key] like ?", new String[]{key});
        } catch (Exception exc) {
            throw new Exception("not in DB");
        }
        try {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                result = c.getString(0);
                found = true;
                c.moveToNext();
            }
        } catch (Exception exc) {
            throw new Exception("not in DB");
        }
        c.close();

        if (!found)
            throw new Exception("not in DB");

        return result;
    }

    public void WriteConfigLong(String key, long value) {
        WriteConfigString(key, String.valueOf(value));
    }

    public long ReadConfigLong(String key) {
        try {
            String value = ReadConfigString(key);
            return Long.valueOf(value);
        } catch (Exception ex) {
            return 0;
        }
    }

    // Zur Parameter �bergabe and die DB
    public static class Parameters extends HashMap<String, Object> {

        /**
         *
         */
        private static final long serialVersionUID = 6506158947781669528L;
    }


    protected void AlterDatabase(int lastDatabaseSchemeVersion) {


        switch (databaseType) {
            case CacheBox:

                beginTransaction();
                try {
                    if (lastDatabaseSchemeVersion <= 0) {
                        // First Initialization of the Database
                        execSQL("CREATE TABLE [Caches] ([Id] bigint NOT NULL primary key,[GcCode] nvarchar (12) NULL,[GcId] nvarchar (255) NULL,[Latitude] float NULL,[Longitude] float NULL,[Name] nchar (255) NULL,[Size] int NULL,[Difficulty] smallint NULL,[Terrain] smallint NULL,[Archived] bit NULL,[Available] bit NULL,[Found] bit NULL,[Type] smallint NULL,[PlacedBy] nvarchar (255) NULL,[Owner] nvarchar (255) NULL,[DateHidden] datetime NULL,[Hint] ntext NULL,[Description] ntext NULL,[Url] nchar (255) NULL,[NumTravelbugs] smallint NULL,[Rating] smallint NULL,[Vote] smallint NULL,[VotePending] bit NULL,[Notes] ntext NULL,[Solver] ntext NULL,[Favorit] bit NULL,[AttributesPositive] bigint NULL,[AttributesNegative] bigint NULL,[TourName] nchar (255) NULL,[GPXFilename_Id] bigint NULL,[HasUserData] bit NULL,[ListingCheckSum] int NULL DEFAULT 0,[ListingChanged] bit NULL,[ImagesUpdated] bit NULL,[DescriptionImagesUpdated] bit NULL,[CorrectedCoordinates] bit NULL);");
                        execSQL("CREATE INDEX [archived_idx] ON [Caches] ([Archived] ASC);");
                        execSQL("CREATE INDEX [AttributesNegative_idx] ON [Caches] ([AttributesNegative] ASC);");
                        execSQL("CREATE INDEX [AttributesPositive_idx] ON [Caches] ([AttributesPositive] ASC);");
                        execSQL("CREATE INDEX [available_idx] ON [Caches] ([Available] ASC);");
                        execSQL("CREATE INDEX [Difficulty_idx] ON [Caches] ([Difficulty] ASC);");
                        execSQL("CREATE INDEX [Favorit_idx] ON [Caches] ([Favorit] ASC);");
                        execSQL("CREATE INDEX [found_idx] ON [Caches] ([Found] ASC);");
                        execSQL("CREATE INDEX [GPXFilename_Id_idx] ON [Caches] ([GPXFilename_Id] ASC);");
                        execSQL("CREATE INDEX [HasUserData_idx] ON [Caches] ([HasUserData] ASC);");
                        execSQL("CREATE INDEX [ListingChanged_idx] ON [Caches] ([ListingChanged] ASC);");
                        execSQL("CREATE INDEX [NumTravelbugs_idx] ON [Caches] ([NumTravelbugs] ASC);");
                        execSQL("CREATE INDEX [placedby_idx] ON [Caches] ([PlacedBy] ASC);");
                        execSQL("CREATE INDEX [Rating_idx] ON [Caches] ([Rating] ASC);");
                        execSQL("CREATE INDEX [Size_idx] ON [Caches] ([Size] ASC);");
                        execSQL("CREATE INDEX [Terrain_idx] ON [Caches] ([Terrain] ASC);");
                        execSQL("CREATE INDEX [Type_idx] ON [Caches] ([Type] ASC);");

                        execSQL("CREATE TABLE [CelltowerLocation] ([CellId] nvarchar (20) NOT NULL primary key,[Latitude] float NULL,[Longitude] float NULL);");

                        execSQL("CREATE TABLE [GPXFilenames] ([Id] integer not null primary key autoincrement,[GPXFilename] nvarchar (255) NULL,[Imported] datetime NULL, [Name] nvarchar (255) NULL,[CacheCount] int NULL);");

                        execSQL("CREATE TABLE [Logs] ([Id] bigint NOT NULL primary key, [CacheId] bigint NULL,[Timestamp] datetime NULL,[Finder] nvarchar (128) NULL,[Type] smallint NULL,[Comment] ntext NULL);");
                        execSQL("CREATE INDEX [log_idx] ON [Logs] ([CacheId] ASC);");
                        execSQL("CREATE INDEX [timestamp_idx] ON [Logs] ([Timestamp] ASC);");

                        execSQL("CREATE TABLE [PocketQueries] ([Id] integer not null primary key autoincrement,[PQName] nvarchar (255) NULL,[CreationTimeOfPQ] datetime NULL);");

                        execSQL("CREATE TABLE [Waypoint] ([GcCode] nvarchar (12) NOT NULL primary key,[CacheId] bigint NULL,[Latitude] float NULL,[Longitude] float NULL,[Description] ntext NULL,[Clue] ntext NULL,[Type] smallint NULL,[SyncExclude] bit NULL,[UserWaypoint] bit NULL,[Title] ntext NULL);");
                        execSQL("CREATE INDEX [UserWaypoint_idx] ON [Waypoint] ([UserWaypoint] ASC);");

                        execSQL("CREATE TABLE [Config] ([Key] nvarchar (30) NOT NULL, [Value] nvarchar (255) NULL);");
                        execSQL("CREATE INDEX [Key_idx] ON [Config] ([Key] ASC);");

                        execSQL("CREATE TABLE [Replication] ([Id] integer not null primary key autoincrement, [ChangeType] int NOT NULL, [CacheId] bigint NOT NULL, [WpGcCode] nvarchar (12) NULL, [SolverCheckSum] int NULL, [NotesCheckSum] int NULL, [WpCoordCheckSum] int NULL);");
                        execSQL("CREATE INDEX [Replication_idx] ON [Replication] ([Id] ASC);");
                        execSQL("CREATE INDEX [ReplicationCache_idx] ON [Replication] ([CacheId] ASC);");
                    }

                    if (lastDatabaseSchemeVersion < 1003) {
                        execSQL("CREATE TABLE [Locations] ([Id] integer not null primary key autoincrement, [Name] nvarchar (255) NULL, [Latitude] float NULL, [Longitude] float NULL);");
                        execSQL("CREATE INDEX [Locatioins_idx] ON [Locations] ([Id] ASC);");

                        execSQL("CREATE TABLE [SdfExport] ([Id]  integer not null primary key autoincrement, [Description] nvarchar(255) NULL, [ExportPath] nvarchar(255) NULL, [MaxDistance] float NULL, [LocationID] Bigint NULL, [Filter] ntext NULL, [Update] bit NULL, [ExportImages] bit NULL, [ExportSpoilers] bit NULL, [ExportMaps] bit NULL, [OwnRepository] bit NULL, [ExportMapPacks] bit NULL, [MaxLogs] int NULL);");
                        execSQL("CREATE INDEX [SdfExport_idx] ON [SdfExport] ([Id] ASC);");

                        execSQL("ALTER TABLE [CACHES] ADD [FirstImported] datetime NULL;");

                        execSQL("CREATE TABLE [Category] ([Id]  integer not null primary key autoincrement, [GpxFilename] nvarchar(255) NULL, [Pinned] bit NULL default 0, [CacheCount] int NULL);");
                        execSQL("CREATE INDEX [Category_idx] ON [Category] ([Id] ASC);");

                        execSQL("ALTER TABLE [GpxFilenames] ADD [CategoryId] bigint NULL;");

                        execSQL("ALTER TABLE [Caches] add [state] nvarchar(50) NULL;");
                        execSQL("ALTER TABLE [Caches] add [country] nvarchar(50) NULL;");
                    }
                    if (lastDatabaseSchemeVersion < 1015) {
                        // GpxFilenames mit Kategorien verknüpfen

                        // alte Category Tabelle löschen
                        delete("Category", "", null);
                        HashMap<Long, String> gpxFilenames = new HashMap<Long, String>();
                        HashMap<String, Long> categories = new HashMap<String, Long>();

                        try {
                            SQLiteGdxDatabaseCursor reader = rawQuery("select ID, GPXFilename from GPXFilenames", null);
                            reader.moveToFirst();
                            while (!reader.isAfterLast()) {
                                long id = reader.getLong(0);
                                String gpxFilename = reader.getString(1);
                                gpxFilenames.put(id, gpxFilename);
                                reader.moveToNext();
                            }
                            reader.close();
                        } catch (Exception e) {
                            //no GPXFilenames stored
                        }
                        for (Entry<Long, String> entry : gpxFilenames.entrySet()) {
                            if (!categories.containsKey(entry.getValue())) {
                                // add new Category
                                Categories cs = new Categories();
                                Category category = cs.createNewCategory(entry.getValue());
                                // and store
                                categories.put(entry.getValue(), category.Id);
                            }
                            if (categories.containsKey(entry.getValue())) {
                                // and store CategoryId in GPXFilenames
                                Parameters args = new Parameters();
                                args.put("CategoryId", categories.get(entry.getValue()));
                                try {
                                    Database.Data.update("GpxFilenames", args, "Id=" + entry.getKey(), null);
                                } catch (Exception exc) {
                                    log.error("Database", "Update_CategoryId", exc);
                                }
                            }
                        }

                    }
                    if (lastDatabaseSchemeVersion < 1016) {
                        execSQL("ALTER TABLE [CACHES] ADD [ApiStatus] smallint NULL default 0;");
                    }
                    if (lastDatabaseSchemeVersion < 1017) {
                        execSQL("CREATE TABLE [Trackable] ([Id] integer not null primary key autoincrement, [Archived] bit NULL, [GcCode] nvarchar (12) NULL, [CacheId] bigint NULL, [CurrentGoal] ntext, [CurrentOwnerName] nvarchar (255) NULL, [DateCreated] datetime NULL, [Description] ntext, [IconUrl] nvarchar (255) NULL, [ImageUrl] nvarchar (255) NULL, [name] nvarchar (255) NULL, [OwnerName] nvarchar (255), [Url] nvarchar (255) NULL);");
                        execSQL("CREATE INDEX [cacheid_idx] ON [Trackable] ([CacheId] ASC);");
                        execSQL("CREATE TABLE [TbLogs] ([Id] integer not null primary key autoincrement, [TrackableId] integer not NULL, [CacheID] bigint NULL, [GcCode] nvarchar (12) NULL, [LogIsEncoded] bit NULL DEFAULT 0, [LogText] ntext, [LogTypeId] bigint NULL, [LoggedByName] nvarchar (255) NULL, [Visited] datetime NULL);");
                        execSQL("CREATE INDEX [trackableid_idx] ON [TbLogs] ([TrackableId] ASC);");
                        execSQL("CREATE INDEX [trackablecacheid_idx] ON [TBLOGS] ([CacheId] ASC);");
                    }
                    if (lastDatabaseSchemeVersion < 1018) {
                        execSQL("ALTER TABLE [SdfExport] ADD [MapPacks] nvarchar(512) NULL;");

                    }
                    if (lastDatabaseSchemeVersion < 1019) {
                        // neue Felder für die erweiterten Attribute einfügen
                        execSQL("ALTER TABLE [CACHES] ADD [AttributesPositiveHigh] bigint NULL default 0");
                        execSQL("ALTER TABLE [CACHES] ADD [AttributesNegativeHigh] bigint NULL default 0");

                        // Die Nummerierung der Attribute stimmte nicht mit der von
                        // Groundspeak überein. Bei 16 und 45 wurde jeweils eine
                        // Nummber übersprungen
                        try {
                            SQLiteGdxDatabaseCursor reader = rawQuery("select Id, AttributesPositive, AttributesNegative from Caches", new String[]{});
                            reader.moveToFirst();
                            while (!reader.isAfterLast()) {
                                long id = reader.getLong(0);
                                long attributesPositive = reader.getLong(1);
                                long attributesNegative = reader.getLong(2);

                                attributesPositive = convertAttribute(attributesPositive);
                                attributesNegative = convertAttribute(attributesNegative);

                                Parameters val = new Parameters();
                                val.put("AttributesPositive", attributesPositive);
                                val.put("AttributesNegative", attributesNegative);
                                String whereClause = "[Id]=" + id;
                                update("Caches", val, whereClause, null);
                                reader.moveToNext();
                            }
                            reader.close();
                        } catch (Exception e) {
                            // no attributes stored
                        }

                    }
                    if (lastDatabaseSchemeVersion < 1020) {
                        // for long Settings
                        execSQL("ALTER TABLE [Config] ADD [LongString] ntext NULL;");

                    }
                    if (lastDatabaseSchemeVersion < 1021) {
                        // Image Table
                        execSQL("CREATE TABLE [Images] ([Id] integer not null primary key autoincrement, [CacheId] bigint NULL, [GcCode] nvarchar (12) NULL, [Description] ntext, [Name] nvarchar (255) NULL, [ImageUrl] nvarchar (255) NULL, [IsCacheImage] bit NULL);");
                        execSQL("CREATE INDEX [images_cacheid_idx] ON [Images] ([CacheId] ASC);");
                        execSQL("CREATE INDEX [images_gccode_idx] ON [Images] ([GcCode] ASC);");
                        execSQL("CREATE INDEX [images_iscacheimage_idx] ON [Images] ([IsCacheImage] ASC);");
                        execSQL("CREATE UNIQUE INDEX [images_imageurl_idx] ON [Images] ([ImageUrl] ASC);");
                    }
                    if (lastDatabaseSchemeVersion < 1022) {
                        execSQL("ALTER TABLE [Caches] ALTER COLUMN [GcCode] nvarchar(15) NOT NULL; ");

                        execSQL("ALTER TABLE [Waypoint] DROP CONSTRAINT Waypoint_PK ");
                        execSQL("ALTER TABLE [Waypoint] ALTER COLUMN [GcCode] nvarchar(15) NOT NULL; ");
                        execSQL("ALTER TABLE [Waypoint] ADD CONSTRAINT  [Waypoint_PK] PRIMARY KEY ([GcCode]); ");

                        execSQL("ALTER TABLE [Replication] ALTER COLUMN [WpGcCode] nvarchar(15) NOT NULL; ");
                        execSQL("ALTER TABLE [Trackable] ALTER COLUMN [GcCode] nvarchar(15) NOT NULL; ");
                        execSQL("ALTER TABLE [TbLogs] ALTER COLUMN [GcCode] nvarchar(15) NOT NULL; ");
                        execSQL("ALTER TABLE [Images] ALTER COLUMN [GcCode] nvarchar(15) NOT NULL; ");
                    }
                    if (lastDatabaseSchemeVersion < 1024) {
                        execSQL("ALTER TABLE [Waypoint] ADD COLUMN [IsStart] BOOLEAN DEFAULT 'false' NULL");
                    }
                    if (lastDatabaseSchemeVersion < 1025) {
                        // nicht mehr benötigt execSQL("ALTER TABLE [Waypoint] ADD COLUMN [UserNote] ntext NULL");
                    }

                    if (lastDatabaseSchemeVersion < 1026) {
                        // add one column for short description
                        // [ShortDescription] ntext NULL
                        execSQL("ALTER TABLE [Caches] ADD [ShortDescription] ntext NULL;");
                    }

                    setTransactionSuccessful();
                } catch (Exception exc) {
                    log.error("AlterDatabase", "", exc);
                } finally {
                    endTransaction();
                }

                break;
            case FieldNotes:
                beginTransaction();
                try {

                    if (lastDatabaseSchemeVersion <= 0) {
                        // First Initialization of the Database
                        // FieldNotes Table
                        execSQL("CREATE TABLE [FieldNotes] ([Id] integer not null primary key autoincrement, [CacheId] bigint NULL, [GcCode] nvarchar (12) NULL, [GcId] nvarchar (255) NULL, [Name] nchar (255) NULL, [CacheType] smallint NULL, [Url] nchar (255) NULL, [Timestamp] datetime NULL, [Type] smallint NULL, [FoundNumber] int NULL, [Comment] ntext NULL);");

                        // Config Table
                        execSQL("CREATE TABLE [Config] ([Key] nvarchar (30) NOT NULL, [Value] nvarchar (255) NULL);");
                        execSQL("CREATE INDEX [Key_idx] ON [Config] ([Key] ASC);");
                    }
                    if (lastDatabaseSchemeVersion < 1002) {
                        execSQL("ALTER TABLE [FieldNotes] ADD COLUMN [Uploaded] BOOLEAN DEFAULT 'false' NULL");
                    }
                    if (lastDatabaseSchemeVersion < 1003) {
                        execSQL("ALTER TABLE [FieldNotes] ADD COLUMN [GC_Vote] integer default 0");
                    }
                    if (lastDatabaseSchemeVersion < 1004) {
                        execSQL("CREATE TABLE [Trackable] ([Id] integer not null primary key autoincrement, [Archived] bit NULL, [GcCode] nvarchar (15) NULL, [CacheId] bigint NULL, [CurrentGoal] ntext, [CurrentOwnerName] nvarchar (255) NULL, [DateCreated] datetime NULL, [Description] ntext, [IconUrl] nvarchar (255) NULL, [ImageUrl] nvarchar (255) NULL, [name] nvarchar (255) NULL, [OwnerName] nvarchar (255), [Url] nvarchar (255) NULL);");
                        execSQL("CREATE INDEX [cacheid_idx] ON [Trackable] ([CacheId] ASC);");
                        execSQL("CREATE TABLE [TbLogs] ([Id] integer not null primary key autoincrement, [TrackableId] integer not NULL, [CacheID] bigint NULL, [GcCode] nvarchar (15) NULL, [LogIsEncoded] bit NULL DEFAULT 0, [LogText] ntext, [LogTypeId] bigint NULL, [LoggedByName] nvarchar (255) NULL, [Visited] datetime NULL);");
                        execSQL("CREATE INDEX [trackableid_idx] ON [TbLogs] ([TrackableId] ASC);");
                        execSQL("CREATE INDEX [trackablecacheid_idx] ON [TBLOGS] ([CacheId] ASC);");
                    }
                    if (lastDatabaseSchemeVersion < 1005) {
                        execSQL("ALTER TABLE [Trackable] ADD COLUMN [TypeName] ntext NULL");
                        execSQL("ALTER TABLE [Trackable] ADD COLUMN [LastVisit] datetime NULL");
                        execSQL("ALTER TABLE [Trackable] ADD COLUMN [Home] ntext NULL");
                        execSQL("ALTER TABLE [Trackable] ADD COLUMN [TravelDistance] integer default 0");
                    }
                    if (lastDatabaseSchemeVersion < 1006) {
                        execSQL("ALTER TABLE [FieldNotes] ADD COLUMN [TbFieldNote] BOOLEAN DEFAULT 'false' NULL");
                        execSQL("ALTER TABLE [FieldNotes] ADD COLUMN [TbName] nvarchar (255)  NULL");
                        execSQL("ALTER TABLE [FieldNotes] ADD COLUMN [TbIconUrl] nvarchar (255)  NULL");
                        execSQL("ALTER TABLE [FieldNotes] ADD COLUMN [TravelBugCode] nvarchar (15)  NULL");
                        execSQL("ALTER TABLE [FieldNotes] ADD COLUMN [TrackingNumber] nvarchar (15)  NULL");
                    }
                    if (lastDatabaseSchemeVersion < 1007) {
                        execSQL("ALTER TABLE [FieldNotes] ADD COLUMN [directLog] BOOLEAN DEFAULT 'false' NULL");
                    }
                    setTransactionSuccessful();
                } catch (Exception exc) {
                    log.error("AlterDatabase", "", exc);
                } finally {
                    endTransaction();
                }
                break;
            case Settings:
                beginTransaction();
                try {
                    if (lastDatabaseSchemeVersion <= 0) {
                        // First Initialization of the Database
                        execSQL("CREATE TABLE [Config] ([Key] nvarchar (30) NOT NULL, [Value] nvarchar (255) NULL);");
                        execSQL("CREATE INDEX [Key_idx] ON [Config] ([Key] ASC);");
                    }
                    if (lastDatabaseSchemeVersion < 1002) {
                        // Long Text Field for long Strings
                        execSQL("ALTER TABLE [Config] ADD [LongString] ntext NULL;");
                    }
                    setTransactionSuccessful();
                } catch (Exception exc) {
                    log.error("AlterDatabase", "", exc);
                } finally {
                    endTransaction();
                }
                break;
        }
    }

    private long convertAttribute(long att) {
        // Die Nummerierung der Attribute stimmte nicht mit der von Groundspeak
        // überein. Bei 16 und 45 wurde jeweils eine Nummber übersprungen
        long result = 0;
        // Maske für die untersten 15 bit
        long mask = 0;
        for (int i = 0; i < 16; i++)
            mask += (long) 1 << i;
        // unterste 15 bit ohne Verschiebung kopieren
        result = att & mask;
        // Maske für die Bits 16-45
        mask = 0;
        for (int i = 16; i < 45; i++)
            mask += (long) 1 << i;
        long tmp = att & mask;
        // Bits 16-44 um eins verschieben
        tmp = tmp << 1;
        // und zum Result kopieren
        result += tmp;
        // Maske für die Bits 45-45
        mask = 0;
        for (int i = 45; i < 63; i++)
            mask += (long) 1 << i;
        tmp = att & mask;
        // Bits 45-63 um 2 verschieben
        tmp = tmp << 2;
        // und zum Result kopieren
        result += tmp;

        return result;
    }

    public static boolean WaypointExists(String gcCode) {
        SQLiteGdxDatabaseCursor c = Database.Data.rawQuery("select GcCode from Waypoint where GcCode=@gccode", new String[]{gcCode});
        {
            c.moveToFirst();
            while (!c.isAfterLast()) {

                try {
                    c.close();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            c.close();

            return false;
        }
    }

    public static String CreateFreeGcCode(String cacheGcCode) throws Exception {
        String suffix = cacheGcCode.substring(2);
        String firstCharCandidates = "CBXADEFGHIJKLMNOPQRSTUVWYZ0123456789";
        String secondCharCandidates = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int i = 0; i < firstCharCandidates.length(); i++)
            for (int j = 0; j < secondCharCandidates.length(); j++) {
                String gcCode = firstCharCandidates.substring(i, i + 1) + secondCharCandidates.substring(j, j + 1) + suffix;
                if (!WaypointExists(gcCode))
                    return gcCode;
            }
        throw new Exception("Alle GcCodes sind bereits vergeben! Dies sollte eigentlich nie vorkommen!");
    }


    public static String GetNote(long cacheId) {
        String resultString = "";
        SQLiteGdxDatabaseCursor c = Database.Data.rawQuery("select Notes from Caches where Id=?", new String[]{String.valueOf(cacheId)});
        c.moveToFirst();
        while (!c.isAfterLast()) {
            resultString = c.getString(0);
            break;
        }
        return resultString;
    }

    /**
     * geänderte Note nur in die DB schreiben
     *
     * @param cacheId
     * @param value
     */
    public static void SetNote(long cacheId, String value) {
        Parameters args = new Parameters();
        args.put("Notes", value);
        args.put("HasUserData", true);

        Database.Data.update("Caches", args, "id=" + cacheId, null);
    }

    public static void SetFound(long cacheId, boolean value) {
        Parameters args = new Parameters();
        args.put("found", value);
        Database.Data.update("Caches", args, "id=" + cacheId, null);
    }

    public static String GetSolver(long cacheId) {
        try {
            String resultString = "";
            SQLiteGdxDatabaseCursor c = Database.Data.rawQuery("select Solver from Caches where Id=?", new String[]{String.valueOf(cacheId)});
            c.moveToFirst();
            while (!c.isAfterLast()) {
                resultString = c.getString(0);
                break;
            }
            return resultString;
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * geänderten Solver nur in die DB schreiben
     *
     * @param cacheId
     * @param value
     */
    public static void SetSolver(long cacheId, String value) {
        Parameters args = new Parameters();
        args.put("Solver", value);
        args.put("HasUserData", true);

        Database.Data.update("Caches", args, "id=" + cacheId, null);
    }

    /**
     * @param minToKeep      Config.settings.LogMinCount.getValue()
     * @param LogMaxMonthAge Config.settings.LogMaxMonthAge.getValue()
     */
    public void DeleteOldLogs(int minToKeep, int LogMaxMonthAge) {

        log.debug("DeleteOldLogs but keep " + minToKeep + " and not older than " + LogMaxMonthAge);
        if (LogMaxMonthAge == 0) {
            // Setting are 'immediately'
            // Delete all Logs and return
            // TODO implement this
        }

        ArrayList<Long> oldLogCaches = new ArrayList<Long>();
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MONTH, -LogMaxMonthAge);
        // hint:
        // months are numbered from 0 onwards in Calendar
        // and month and day have leading zeroes in logs Timestamp
        String TimeStamp = (now.get(Calendar.YEAR)) + "-" + String.format("%02d", (now.get(Calendar.MONTH) + 1)) + "-" + String.format("%02d", now.get(Calendar.DATE));

        // #############################################################################
        // Get CacheId's from Caches with older logs and having more logs than minToKeep
        // #############################################################################
        {
            try {
                String command = "SELECT cacheid FROM logs WHERE Timestamp < '" + TimeStamp + "' GROUP BY CacheId HAVING COUNT(Id) > " + String.valueOf(minToKeep);
                log.debug(command);
                SQLiteGdxDatabaseCursor reader = Database.Data.rawQuery(command, null);
                reader.moveToFirst();
                while (!reader.isAfterLast()) {
                    long tmp = reader.getLong(0);
                    if (!oldLogCaches.contains(tmp))
                        oldLogCaches.add(reader.getLong(0));
                    reader.moveToNext();
                }
                reader.close();
            } catch (Exception ex) {
                log.error("DeleteOldLogs", ex);
            }
        }

        // ###################################################
        // Get Logs
        // ###################################################
        {
            try {
                beginTransaction();
                for (long oldLogCache : oldLogCaches) {
                    ArrayList<Long> minLogIds = new ArrayList<Long>();
                    String command = "select id from logs where cacheid = " + String.valueOf(oldLogCache) + " order by Timestamp desc";
                    log.debug(command);
                    int count = 0;
                    SQLiteGdxDatabaseCursor reader = Database.Data.rawQuery(command, null);
                    reader.moveToFirst();
                    while (!reader.isAfterLast()) {
                        if (count == minToKeep)
                            break;
                        minLogIds.add(reader.getLong(0));
                        reader.moveToNext();
                        count++;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (long id : minLogIds)
                        sb.append(id).append(",");
                    // now delete all Logs out of Date but keep the ones in minLogIds
                    String delCommand;
                    if (sb.length() > 0)
                        delCommand = "DELETE FROM Logs WHERE Timestamp<'" + TimeStamp + "' AND cacheid = " + String.valueOf(oldLogCache) + " AND id NOT IN (" + sb.toString().substring(0, sb.length() - 1) + ")";
                    else
                        delCommand = "DELETE FROM Logs WHERE Timestamp<'" + TimeStamp + "' AND cacheid = " + String.valueOf(oldLogCache);
                    log.debug(delCommand);
                    Database.Data.execSQL(delCommand);
                }
                setTransactionSuccessful();
            } catch (Exception ex) {
                log.error("DeleteOldLogs", ex);
            } finally {
                endTransaction();
            }
        }
    }


    // DB Funktionen

    public void Initialize() {
        if (myDB == null) {
            if (!databasePath.exists())
                Reset();

            try {
                log.debug("open data base: " + databasePath);
                myDB = SQLiteGdxDatabaseFactory.getNewDatabase(databasePath);
                myDB.openOrCreateDatabase();
            } catch (Exception exc) {
                return;
            }
        }
    }

    public void Reset() {
        // if exists, delete old database file
        if (databasePath.exists()) {
            log.debug("RESET DB, delete file: " + databasePath);
            databasePath.delete();
        }

        try {
            log.debug("create data base: " + databasePath);
            myDB = SQLiteGdxDatabaseFactory.getNewDatabase(databasePath);
            myDB.openOrCreateDatabase();
            myDB.setTransactionSuccessful();
            myDB.closeDatabase();

        } catch (Exception exc) {
            log.error("createDB", exc);
        }
    }


    public SQLiteGdxDatabaseCursor rawQuery(String sql, String[] args) {
        try {
            return myDB.rawQuery(sql, args);
        } catch (SQLiteGdxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void execSQL(String sql) {
        try {
            myDB.execSQL(sql);
        } catch (SQLiteGdxException e) {
            e.printStackTrace();
        }
    }


    public long update(String tablename, Parameters val, String whereClause, String[] whereArgs) {
        return myDB.update(tablename, val, whereClause, whereArgs);
    }

    public long insert(String tablename, Parameters val) {
        return myDB.insert(tablename, val);
    }

    public long delete(String tablename, String whereClause, String[] whereArgs) {
        return myDB.delete(tablename, whereClause, whereArgs);
    }

    public void beginTransaction() {
        log.trace("begin transaction");
        if (myDB != null)
            myDB.setAutoCommit(false);
    }

    public void setTransactionSuccessful() {
        log.trace("begin transaction");
        if (myDB != null)
            myDB.setTransactionSuccessful();
    }

    public void endTransaction() {
        log.trace("begin transaction");
        if (myDB != null)
            myDB.endTransaction();
    }

    public long insertWithConflictReplace(String tablename, Parameters val) {
        return myDB.insertWithConflictReplace(tablename, val);
    }

    public long insertWithConflictIgnore(String tablename, Parameters val) {
        return myDB.insertWithConflictIgnore(tablename, val);
    }

    public void Close() {
        try {
            myDB.closeDatabase();
        } catch (SQLiteGdxException e) {
            e.printStackTrace();
        }
    }
}
