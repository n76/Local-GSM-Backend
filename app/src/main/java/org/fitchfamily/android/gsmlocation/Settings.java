package org.fitchfamily.android.gsmlocation;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Settings {
    private static final String DB_NAME = "lacells.db";
    private static final String DB_BAK_NAME = DB_NAME + ".bak";
    private static final String DB_NEW_NAME = DB_NAME + ".new";

    private static final File DATABASE_DIRECTORY_OLD = new File(Environment.getExternalStorageDirectory(), ".nogapps");

    private static final boolean USE_LACELLS_DEFAULT = false;

    private static final String USE_LACELLS = "lacells_preference";

    private static final String USE_MOZILLA_LOCATION_SERVICE = "mls_preference";

    private static final String USE_OPEN_CELL_ID = "oci_preference";

    private static final String OPEN_CELL_ID_API_KEY = "oci_key_preference";

    private static final String MNC_FILTER = "mnc_filter_preference";

    private static final String MCC_FILTER = "mcc_filter_preference";

    private static final Object lock = new Object();
    private static Settings instance;

    private final SharedPreferences preferences;
    private final Context context;

    private Settings(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        // move directory to the new location
        if(DATABASE_DIRECTORY_OLD.exists() && DATABASE_DIRECTORY_OLD.canWrite()) {
            File[] files = DATABASE_DIRECTORY_OLD.listFiles();

            if(files != null) {
                for(File file : files) {
                    file.renameTo(new File(databaseDirectory(), file.getName()));
                }
            }

            DATABASE_DIRECTORY_OLD.delete();
        }
    }

    public static Settings with(Fragment fragment) {
        return with(fragment.getContext());
    }

    public static Settings with(Context context) {
        if(context == null) {
            throw new NullPointerException();
        }

        if(instance == null) {
            synchronized (lock) {
                if(instance == null) {
                    instance = new Settings(context);
                }
            }
        }

        return instance;
    }

    public String mccFilters() {
        return preferences.getString(MCC_FILTER, "");
    }

    public String mncFilters() {
        return preferences.getString(MNC_FILTER, "");
    }

    public String openCellIdApiKey() {
        return preferences.getString(OPEN_CELL_ID_API_KEY, "");
    }

    public boolean useLacells() {
        return preferences.getBoolean(USE_LACELLS, USE_LACELLS_DEFAULT);
    }

    public boolean useOpenCellId() {
        return preferences.getBoolean(USE_OPEN_CELL_ID, false);
    }

    public boolean useMozillaLocationService() {
        return preferences.getBoolean(USE_MOZILLA_LOCATION_SERVICE, false);
    }

    public File databaseDirectory() {
        return context.getExternalFilesDir(null);
    }

    public File newDatabaseFile() {
        return new File(databaseDirectory(), DB_NEW_NAME);
    }

    public File currentDatabaseFile() {
        return new File(databaseDirectory(), DB_NAME);
    }

    public File bakDatabaseFile() {
        return new File(databaseDirectory(), DB_BAK_NAME);
    }

    public File logfile() {
        return new File(databaseDirectory(), "lacells_gen.log");
    }

    /**
     * Use this function to get the current database file
     *
     * @return the current database file or null if not found
     */
    public File databaseFile() {
        if (newDatabaseFile().exists()) {
            return newDatabaseFile();
        } else if (currentDatabaseFile().exists()) {
            return currentDatabaseFile();
        } else {
            return null;
        }
    }

    /**
     * Use this function to get the time of the last update of the database
     *
     * @return unix timestamp in milliseconds or 0
     */
    public long databaseLastModified() {
        File databaseFile = databaseFile();
        return (databaseFile != null && databaseFile.canRead()) ? databaseFile.lastModified() : 0;
    }

    public Settings useLacells(boolean enable) {
        if (enable != useLacells()) {
            preferences.edit()
                    .putBoolean(USE_LACELLS, enable)
                    .commit();
        }

        return this;
    }

    public Settings useOpenCellId(boolean enable) {
        if (enable != useOpenCellId()) {
            preferences.edit()
                    .putBoolean(USE_OPEN_CELL_ID, enable)
                    .commit();
        }

        return this;
    }

    public Settings useMozillaLocationService(boolean enable) {
        if (enable != useMozillaLocationService()) {
            preferences.edit()
                    .putBoolean(USE_MOZILLA_LOCATION_SERVICE, enable)
                    .commit();
        }

        return this;
    }

    public Settings openCellIdApiKey(String key) {
        if (!TextUtils.equals(key, openCellIdApiKey())) {
            preferences.edit()
                    .putString(OPEN_CELL_ID_API_KEY, key)
                    .commit();
        }

        return this;
    }

    /**
     * Use this function to get the entered MMC numbers.
     * If there is nothing chosen, an empty list is returned. This should be handled as all areas (when downloading).
     *
     * @return the selected MCC numbers
     */
    public Set<Integer> mccFilterSet() {
        Set<Integer> result = new HashSet<>();

        String mccList = mccFilters();

        if (!TextUtils.isEmpty(mccList)) {
            for (String number : mccList.split(",")) {
                try {
                    result.add(Integer.valueOf(number));
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
        }

        return Collections.unmodifiableSet(result);
    }

    public Settings mccFilterSet(Set<Integer> numbers) {
        preferences.edit()
                .putString(MCC_FILTER, TextUtils.join(",", numbers.toArray()))
                .commit();

        return this;
    }
}
