package org.fitchfamily.android.gsmlocation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

public class prefsFragment extends PreferenceFragment {

    protected String TAG = appConstants.TAG_PREFIX+"settings";
    private static boolean DEBUG = appConstants.DEBUG;

    public prefsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        EditTextPreference ociKeyPreference = (EditTextPreference) this.findPreference("oci_key_preference");
        if (ociKeyPreference != null) {
            if (DEBUG) Log.d(TAG, "prefsFragment.onCreate(): ociKeyPreference is "+ociKeyPreference.toString());
            if(ociKeyPreference.getText()==null) {
                // to ensure we don't get a null value
                // set first value by default
                ociKeyPreference.setText("");
            }
            ociKeyPreference.setSummary(ociKeyPreference.getText());
            ociKeyPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            });
        } else {
            if (DEBUG) Log.d(TAG, "prefsFragment.onCreate(): ociKeyPreference is null");
        }

        EditTextPreference mccFilterPreference = (EditTextPreference) this.findPreference("mcc_filter_preference");
        if (mccFilterPreference != null) {
            if (DEBUG) Log.d(TAG, "prefsFragment.onCreate(): mccFilterPreference is "+mccFilterPreference.toString());
            if(mccFilterPreference.getText()==null) {
                // to ensure we don't get a null value
                // set first value by default
                mccFilterPreference.setText("");
            }
            mccFilterPreference.setSummary(mccFilterPreference.getText());
            mccFilterPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            });
        } else {
            if (DEBUG) Log.d(TAG, "prefsFragment.onCreate(): mccFilterPreference is null");
        }

        EditTextPreference currentDbPreference = (EditTextPreference) this.findPreference("db_date_preference");
        if (currentDbPreference != null) {
            if(currentDbPreference.getText()==null) {
                // to ensure we don't get a null value
                // set first value by default
                mccFilterPreference.setText("");
            }

            String currentDbInfo = "";
            if (appConstants.DB_FILE.exists()) {
                if (appConstants.DB_FILE.canRead()) {
                    DateFormat dateTimeInstance = SimpleDateFormat.getDateTimeInstance();
                    currentDbInfo = "Last Modified: " + dateTimeInstance.format(appConstants.DB_FILE.lastModified());
                } else {
                    currentDbInfo = "No read permission on database.";
                }
            } else {
                currentDbInfo = "No database file found.";
            }
            currentDbPreference.setSummary(currentDbInfo);
        } else {
            if (DEBUG) Log.d(TAG, "prefsFragment.onCreate(): currentDbPreference is null");
        }

    }

}
