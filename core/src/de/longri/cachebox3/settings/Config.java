package de.longri.cachebox3.settings;

import de.longri.cachebox3.sqlite.Database;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Created by Longri on 02.08.16.
 */
public class Config extends Settings {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(Config.class);

    public static void AcceptChanges() {
        WriteToDB();
    }

    /**
     * Return true, if setting changes need restart
     *
     * @return
     */
    public static boolean WriteToDB() {
        // Write into DB
        SettingsDAO dao = new SettingsDAO();

        Database Data = Database.Data;
        Database SettingsDB = Database.Settings;

        try {
            if (Data != null)
                Data.beginTransaction();
        } catch (Exception ex) {
            // do not change Data now!
            Data = null;
        }

        SettingsDB.beginTransaction();

        boolean needRestart = false;

        try {
            for (Iterator<SettingBase<?>> it = SettingsList.that.iterator(); it.hasNext(); ) {
                SettingBase<?> setting = it.next();
                if (!setting.isDirty())
                    continue; // is not changed -> do not

                if (SettingStoreType.Local == setting.getStoreType()) {
                    if (Data != null)
                        dao.WriteToDatabase(Data, setting);
                } else if (SettingStoreType.Global == setting.getStoreType() || (!PlatformSettings.canUsePlatformSettings() && SettingStoreType.Platform == setting.getStoreType())) {
                    dao.WriteToDatabase(SettingsDB, setting);
                } else if (SettingStoreType.Platform == setting.getStoreType()) {
                    dao.WriteToPlatformSettings(setting);
                    dao.WriteToDatabase(SettingsDB, setting);
                }

                if (setting.needRestart) {
                    needRestart = true;
                }

                setting.clearDirty();

            }
            if (Data != null)
                Data.setTransactionSuccessful();
            SettingsDB.setTransactionSuccessful();

            return needRestart;
        } finally {
            SettingsDB.endTransaction();
            if (Data != null)
                Data.endTransaction();
        }

    }

    public static void ReadFromDB() {
        Database Data = Database.Data;
        Database SettingsDB = Database.Settings;
        // Read from DB

        SettingsDAO dao = new SettingsDAO();
        for (Iterator<SettingBase<?>> it = SettingsList.that.iterator(); it.hasNext(); ) {
            SettingBase<?> setting = it.next();
            String debugString;

            boolean isPlatform = false;
            boolean isPlattformoverride = false;

            if (SettingStoreType.Local == setting.getStoreType()) {
                if (Data == null)
                    setting.loadDefault();
                else
                    setting = dao.ReadFromDatabase(Data, setting);
            } else if (SettingStoreType.Global == setting.getStoreType() || (!PlatformSettings.canUsePlatformSettings() && SettingStoreType.Platform == setting.getStoreType())) {
                setting = dao.ReadFromDatabase(SettingsDB, setting);
            } else if (SettingStoreType.Platform == setting.getStoreType()) {
                isPlatform = true;
                SettingBase<?> cpy = setting.copy();
                cpy = dao.ReadFromDatabase(SettingsDB, cpy);
                setting = dao.ReadFromPlatformSetting(setting);

                // chk for Value on User.db3 and cleared Platform Value

                if (setting instanceof SettingString) {
                    SettingString st = (SettingString) setting;

                    if (st.value.length() == 0) {
                        // Platform Settings are empty use db3 value or default
                        setting = dao.ReadFromDatabase(SettingsDB, setting);
                        dao.WriteToPlatformSettings(setting);
                    }
                } else if (!cpy.value.equals(setting.value)) {
                    if (setting.value.equals(setting.defaultValue)) {
                        // override Platformsettings with UserDBSettings
                        setting.setValueFrom(cpy);
                        dao.WriteToPlatformSettings(setting);
                        setting.clearDirty();
                        isPlattformoverride = true;
                    } else {
                        // override UserDBSettings with Platformsettings
                        cpy.setValueFrom(setting);
                        dao.WriteToDatabase(SettingsDB, cpy);
                        cpy.clearDirty();
                    }
                }
            }

            if (setting instanceof SettingEncryptedString) {// Don't write encrypted settings in to a log file
                debugString = "*******";
            } else {
                debugString = setting.value.toString();
            }

            if (isPlatform) {
                if (isPlattformoverride) {
                    log.debug("Override Platform setting [" + setting.name + "] from DB to: " + debugString);
                } else {
                    log.debug("Override PlatformDB setting [" + setting.name + "] from Platform to: " + debugString);
                }
            } else {
                if (!setting.value.equals(setting.defaultValue)) {
                    log.debug("Change " + setting.getStoreType() + " setting [" + setting.name + "] to: " + debugString);
                } else {
                    log.debug("Default " + setting.getStoreType() + " setting [" + setting.name + "] to: " + debugString);
                }
            }
        }
        log.debug("Settings are loaded");
    }

    public static void LoadFromLastValue() {
        for (Iterator<SettingBase<?>> it = SettingsList.that.iterator(); it.hasNext(); ) {
            SettingBase<?> setting = it.next();
            setting.loadFromLastValue();
        }
    }

    public static void SaveToLastValue() {
        for (Iterator<SettingBase<?>> it = SettingsList.that.iterator(); it.hasNext(); ) {
            SettingBase<?> setting = it.next();
            setting.saveToLastValue();
        }
    }

    public static void LoadAllDefaultValues() {
        for (Iterator<SettingBase<?>> it = SettingsList.that.iterator(); it.hasNext(); ) {
            SettingBase<?> setting = it.next();
            setting.loadDefault();
        }
    }


}
