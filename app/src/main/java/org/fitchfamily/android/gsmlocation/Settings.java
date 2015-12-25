package org.fitchfamily.android.gsmlocation;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {
    private static final Object lock = new Object();
    private static Settings instance;

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

    private final SharedPreferences preferences;

    private Settings(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public String mccFilters() {
        return preferences.getString("mcc_filter_preference", "");
    }

    public String mncFilters() {
        return preferences.getString("mnc_filter_preference", "");
    }

    public String openCellIdApiKey() {
        return preferences.getString("oci_key_preference", "");
    }

    public boolean useOpenCellId() {
        return preferences.getBoolean("oci_preference", false);
    }

    public boolean useMozillaLocationService() {
        return preferences.getBoolean("mls_preference", false);
    }
}
