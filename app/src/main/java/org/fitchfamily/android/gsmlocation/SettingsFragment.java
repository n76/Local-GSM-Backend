package org.fitchfamily.android.gsmlocation;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = makeLogTag(SettingsFragment.class);
    private static final boolean DEBUG = Config.DEBUG;

    private SharedPreferences sp;
    private EditTextPreference ociKeyPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_prefs);
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        ociKeyPreference = (EditTextPreference) this.findPreference("oci_key_preference");
        if (ociKeyPreference != null) {
            if (DEBUG)
                Log.d(TAG, "onCreate(): ociKeyPreference is " + ociKeyPreference.toString());

            if (ociKeyPreference.getText() == null ||
                    ociKeyPreference.getText().isEmpty()){
                // to ensure we don't get a null value
                // set first value by default
                ociKeyPreference.setText("");
            }

            ociKeyPreference.setSummary(ociKeyPreference.getText());
            ociKeyPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String value = newValue.toString();
                    preference.setSummary(value);
                    return true;
                }
            });
        } else {
            if (DEBUG)
                Log.d(TAG, "onCreate(): ociKeyPreference is null");
        }

        Preference preference = this.findPreference("request_new_api_key");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference arg0) {
                Log.i(TAG, "requesting new key");
                new RequestOpenCellIdKeyTask().execute();
                return true;
            }
        });


        EditTextPreference mccFilterPreference = (EditTextPreference) this.findPreference("mcc_filter_preference");
        if (mccFilterPreference != null) {
            if (DEBUG)
                Log.d(TAG, "onCreate(): mccFilterPreference is " + mccFilterPreference.toString());

            if (mccFilterPreference.getText() == null) {
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
            if (DEBUG)
                Log.d(TAG, "onCreate(): mccFilterPreference is null");
        }

        EditTextPreference mncFilterPreference = (EditTextPreference) this.findPreference("mnc_filter_preference");
        if (mncFilterPreference != null) {
            if (DEBUG)
                Log.d(TAG, "onCreate(): mncFilterPreference is " + mncFilterPreference.toString());

            if (mncFilterPreference.getText() == null) {
                // to ensure we don't get a null value
                // set first value by default
                mncFilterPreference.setText("");
            }

            mncFilterPreference.setSummary(mncFilterPreference.getText());
            mncFilterPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            });
        } else {
            if (DEBUG)
                Log.d(TAG, "onCreate(): mncFilterPreference is null");
        }

        EditTextPreference currentDbPreference = (EditTextPreference) this.findPreference("db_date_preference");

        if (currentDbPreference != null) {
            if (currentDbPreference.getText() == null) {
                // to ensure we don't get a null value
                // set first value by default
                currentDbPreference.setText("");
            }
            currentDbPreference.setSummary(currentDbTimeStamp());
        } else {
            if (DEBUG)
                Log.d(TAG, "onCreate(): currentDbPreference is null");
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (DEBUG)
            Log.d(TAG, "onResume()");

        EditTextPreference currentDbPreference = (EditTextPreference) this.findPreference("db_date_preference");
        if (currentDbPreference != null) {
            if (currentDbPreference.getText() == null) {
                // to ensure we don't get a null value
                // set first value by default
                currentDbPreference.setText("");
            }
            currentDbPreference.setSummary(currentDbTimeStamp());
        } else {
            if (DEBUG)
                Log.d(TAG, "onResume(): currentDbPreference is null");
        }
    }

    private String currentDbTimeStamp() {
        String currentDbInfo;
        if (Config.DB_NEW_FILE.exists() &&
                Config.DB_NEW_FILE.canRead()) {
            DateFormat dateTimeInstance = SimpleDateFormat.getDateTimeInstance();
            currentDbInfo = "Last Modified: " + dateTimeInstance.format(Config.DB_NEW_FILE.lastModified());
        } else if (Config.DB_FILE.exists()) {
            if (Config.DB_FILE.canRead()) {
                DateFormat dateTimeInstance = SimpleDateFormat.getDateTimeInstance();
                currentDbInfo = "Last Modified: " + dateTimeInstance.format(Config.DB_FILE.lastModified());
            } else {
                currentDbInfo = "No read permission on database.";
            }
        } else {
            currentDbInfo = "No database file found.";
        }
        return currentDbInfo;
    }

    /**
     * OpenCellID API keys appear to be canonical form version 4 UUIDs, except keys
     * generated with http://opencellid.org/gsmCell/user/generateApiKey have
     * "dev-usr-" instead of hexadecimal digits before the first hyphen.
     */
    private boolean isOpenCellIdKeyValid(String key) {
        return key.matches(
                "(?:[0-9a-f]{8}|dev-usr-)-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }

    private class RequestOpenCellIdKeyTask extends AsyncTask<Void, Void, Integer> {
        private String newKey;

        @Override
        protected void onPreExecute() {
            Toast.makeText(getActivity(), R.string.oci_key_req_request_start, Toast.LENGTH_SHORT)
                    .show();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            HttpURLConnection conn = null;
            int toastTextResId;

            try {
                conn = (HttpURLConnection) new URL(Config.OCI_API_GET_KEY_URL).openConnection();
                conn.connect();
                int statusCode = conn.getResponseCode();

                if (statusCode == 200) {
                    byte[] buf = new byte[36];
                    if (conn.getInputStream().read(buf, 0, 36) != 36) {
                        toastTextResId = R.string.oci_key_req_failed_read;
                    } else {
                        newKey = new String(buf, "UTF-8");

                        if (isOpenCellIdKeyValid(newKey)) {
                            sp.edit().putString("oci_key_preference", newKey).commit();
                            toastTextResId = R.string.oci_key_req_new_key_success;
                        } else {
                            if (DEBUG) Log.e(TAG, "Invalid OpenCellID API key received: " + newKey);
                            toastTextResId = R.string.oci_key_req_key_invalid;
                        }
                    }
                } else if (statusCode == 503) {
                    toastTextResId = R.string.oci_key_req_only_one_req_per_day;
                } else {
                    if (DEBUG) {
                        Log.e(TAG, "OpenCellID API key request response code: " + statusCode);
                    }
                    toastTextResId = R.string.oci_key_req_unexpected_status_code;
                }
            } catch (IOException e) {
                if (DEBUG) Log.e(TAG, "Error requesting OpenCellID API key", e);
                toastTextResId = R.string.oci_key_req_connection_error;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            return toastTextResId;
        }

        @Override
        protected void onPostExecute(Integer stringResId) {
            if (newKey != null) {
                ociKeyPreference.setSummary(newKey);
            }
            Toast.makeText(getActivity(), stringResId, Toast.LENGTH_LONG).show();
        }
    }
}
