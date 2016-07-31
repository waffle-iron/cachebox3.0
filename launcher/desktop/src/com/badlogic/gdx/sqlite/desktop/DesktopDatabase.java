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
package com.badlogic.gdx.sqlite.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.sql.SQLiteGdxDatabase;
import com.badlogic.gdx.sql.SQLiteGdxDatabaseCursor;
import com.badlogic.gdx.sql.SQLiteGdxDatabaseFactory;
import com.badlogic.gdx.sql.SQLiteGdxException;
import com.badlogic.gdx.utils.GdxRuntimeException;
import de.longri.cachebox3.CB;
import de.longri.cachebox3.sqlite.Database;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;

/**
 * @author M Rafay Aleem (2014)-(https://github.com/mrafayaleem/gdx-sqlite)
 * @author Longri (2016)
 */
public class DesktopDatabase implements SQLiteGdxDatabase {

    final static org.slf4j.Logger log = LoggerFactory.getLogger(DesktopDatabase.class);

    private SQLiteDatabaseHelper helper = null;

    private final FileHandle dbFileHandle;
    private final int dbVersion;
    private final String dbOnCreateQuery;
    private final String dbOnUpgradeQuery;

    private Connection connection = null;
    private Statement stmt = null;

    public DesktopDatabase(FileHandle dbFileHandle, int dbVersion, String dbOnCreateQuery, String dbOnUpgradeQuery) {
        this.dbFileHandle = dbFileHandle;
        this.dbVersion = dbVersion;
        this.dbOnCreateQuery = dbOnCreateQuery;
        this.dbOnUpgradeQuery = dbOnUpgradeQuery;
    }

    @Override
    public void setupDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            Gdx.app.log(SQLiteGdxDatabaseFactory.ERROR_TAG,
                    "Unable to load the SQLite JDBC driver. Their might be a problem with your build path or project setup.", e);
            throw new GdxRuntimeException(e);
        }
    }

    @Override
    public void openOrCreateDatabase() throws SQLiteGdxException {
        String DB_URL = this.dbFileHandle.file().getAbsolutePath();

        if (helper == null) helper = new SQLiteDatabaseHelper(DB_URL, dbVersion, dbOnCreateQuery, dbOnUpgradeQuery);

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_URL);
            stmt = connection.createStatement();
            helper.onCreate(stmt);
        } catch (SQLException e) {
            throw new SQLiteGdxException(e);
        }
    }

    @Override
    public void closeDatabase() throws SQLiteGdxException {
        try {
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            throw new SQLiteGdxException(e);
        }
    }

    @Override
    public void execSQL(String sql) throws SQLiteGdxException {
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new SQLiteGdxException(e);
        }
    }

    @Override
    public SQLiteGdxDatabaseCursor rawQuery(String sql) throws SQLiteGdxException {
        DesktopCursor lCursor = new DesktopCursor();
        try {
            ResultSet resultSetRef = stmt.executeQuery(sql);
            lCursor.setNativeCursor(resultSetRef);
            return lCursor;
        } catch (SQLException e) {
            throw new SQLiteGdxException(e);
        }
    }

    @Override
    public SQLiteGdxDatabaseCursor rawQuery(SQLiteGdxDatabaseCursor cursor, String sql) throws SQLiteGdxException {
        DesktopCursor lCursor = (DesktopCursor) cursor;
        try {
            ResultSet resultSetRef = stmt.executeQuery(sql);
            lCursor.setNativeCursor(resultSetRef);
            return lCursor;
        } catch (SQLException e) {
            throw new SQLiteGdxException(e);
        }
    }

    @Override
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PreparedStatement prepareStatement(String sql) {
        try {
            return connection.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void endTransaction() {
        try {
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setTransactionSuccessful() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long insert(String tablename, Database.Parameters val) {
        if (connection == null)
            return 0;

        StringBuilder sql = new StringBuilder();

        sql.append("insert into ");
        sql.append(tablename);
        sql.append(" (");

        int i = 0;
        for (Map.Entry<String, Object> entry : val.entrySet()) {
            i++;
            sql.append(" ");
            sql.append(entry.getKey());
            if (i != val.size()) {
                sql.append(",");
            }
        }

        sql.append(" ) Values(");

        for (int k = 1; k <= val.size(); k++) {
            sql.append(" ");
            sql.append("?");
            if (k < val.size()) {
                sql.append(",");
            }
        }

        sql.append(" )");
        PreparedStatement st = null;
        try {
            st = connection.prepareStatement(sql.toString());

            int j = 0;
            for (Map.Entry<String, Object> entry : val.entrySet()) {
                j++;
                st.setObject(j, entry.getValue());
            }

            return st.execute() ? 0 : 1;

        } catch (SQLException e) {
            return 0;
        } finally {
            try {
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public long update(String tablename, Database.Parameters val, String whereClause, String[] whereArgs) {
        if (CB.isLogLevel(CB.LOG_LEVEL_DEBUG)) {
            StringBuilder sb = new StringBuilder("Update Table:" + tablename);
            sb.append("Parameters:" + val.toString());
            sb.append("WHERECLAUSE:" + whereClause);

            if (whereArgs != null) {
                for (String arg : whereArgs) {
                    sb.append(arg + ", ");
                }
            }

            log.debug(sb.toString());
        }

        if (connection == null)
            return 0;

        StringBuilder sql = new StringBuilder();

        sql.append("update ");
        sql.append(tablename);
        sql.append(" set");

        int i = 0;
        for (Map.Entry<String, Object> entry : val.entrySet()) {
            i++;
            sql.append(" ");
            sql.append(entry.getKey());
            sql.append("=?");
            if (i != val.size()) {
                sql.append(",");
            }
        }

        if (!whereClause.isEmpty()) {
            sql.append(" where ");
            sql.append(whereClause);
        }
        PreparedStatement st = null;
        try {
            st = connection.prepareStatement(sql.toString());

            int j = 0;
            for (Map.Entry<String, Object> entry : val.entrySet()) {
                j++;
                st.setObject(j, entry.getValue());
            }

            if (whereArgs != null) {
                for (int k = 0; k < whereArgs.length; k++) {
                    st.setString(j + k + 1, whereArgs[k]);
                }
            }
            return st.executeUpdate();

        } catch (SQLException e) {
            return 0;
        } finally {
            try {
                st.close();
            } catch (SQLException e) {

                e.printStackTrace();
            }
        }
    }

    @Override
    public long delete(String tablename, String whereClause, String[] whereArgs) {
        if (CB.isLogLevel(CB.LOG_LEVEL_DEBUG)) {
            StringBuilder sb = new StringBuilder("Delete@ Table:" + tablename);
            sb.append("WHERECLAUSE:" + whereClause);

            if (whereArgs != null) {
                for (String arg : whereArgs) {
                    sb.append(arg + ", ");
                }
            }

            log.debug(sb.toString());
        }

        if (connection == null)
            return 0;
        StringBuilder sql = new StringBuilder();

        sql.append("delete from ");
        sql.append(tablename);

        if (!whereClause.isEmpty()) {
            sql.append(" where ");
            sql.append(whereClause);
        }
        PreparedStatement st = null;
        try {
            st = connection.prepareStatement(sql.toString());

            if (whereArgs != null) {
                for (int i = 0; i < whereArgs.length; i++) {
                    st.setString(i + 1, whereArgs[i]);
                }
            }

            return st.executeUpdate();

        } catch (SQLException e) {
            return 0;
        } finally {
            try {
                st.close();
            } catch (SQLException e) {

                e.printStackTrace();
            }
        }
    }

    @Override
    public long insertWithConflictReplace(String tablename, Database.Parameters val) {
        if (connection == null)
            return 0;

        log.debug("insertWithConflictReplace @Table:" + tablename + "Parameters: " + val.toString());
        StringBuilder sql = new StringBuilder();

        sql.append("insert OR REPLACE into ");
        sql.append(tablename);
        sql.append(" (");

        int i = 0;
        for (Map.Entry<String, Object> entry : val.entrySet()) {
            i++;
            sql.append(" ");
            sql.append(entry.getKey());
            if (i != val.size()) {
                sql.append(",");
            }
        }

        sql.append(" ) Values(");

        for (int k = 1; k <= val.size(); k++) {
            sql.append(" ");
            sql.append("?");
            if (k < val.size()) {
                sql.append(",");
            }
        }

        sql.append(" )");
        PreparedStatement st = null;
        try {
            st = connection.prepareStatement(sql.toString());

            int j = 0;
            for (Map.Entry<String, Object> entry : val.entrySet()) {
                j++;
                st.setObject(j, entry.getValue());
            }

            return st.executeUpdate();

        } catch (SQLException e) {
            return 0;
        } finally {
            try {
                st.close();
            } catch (SQLException e) {

                e.printStackTrace();
            }
        }
    }

    @Override
    public long insertWithConflictIgnore(String tablename, Database.Parameters val) {
        if (connection == null)
            return 0;

        log.debug("insertWithConflictIgnore @Table:" + tablename + "Parameters: " + val.toString());

        StringBuilder sql = new StringBuilder();

        sql.append("insert OR IGNORE into ");
        sql.append(tablename);
        sql.append(" (");

        int i = 0;
        for (Map.Entry<String, Object> entry : val.entrySet()) {
            i++;
            sql.append(" ");
            sql.append(entry.getKey());
            if (i != val.size()) {
                sql.append(",");
            }
        }

        sql.append(" ) Values(");

        for (int k = 1; k <= val.size(); k++) {
            sql.append(" ");
            sql.append("?");
            if (k < val.size()) {
                sql.append(",");
            }
        }

        sql.append(" )");
        PreparedStatement st = null;
        try {
            st = connection.prepareStatement(sql.toString());

            int j = 0;
            for (Map.Entry<String, Object> entry : val.entrySet()) {
                j++;
                st.setObject(j, entry.getValue());
            }

            return st.executeUpdate();

        } catch (SQLException e) {
            return 0;
        } finally {
            try {
                st.close();
            } catch (SQLException e) {

                e.printStackTrace();
            }
        }
    }
}