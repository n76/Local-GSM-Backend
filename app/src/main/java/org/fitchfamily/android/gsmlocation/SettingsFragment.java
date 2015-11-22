package org.fitchfamily.android.gsmlocation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
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

public class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = makeLogTag(SettingsFragment.class);
    private static final boolean DEBUG = Config.DEBUG;

    private SharedPreferences sp;
    private EditTextPreference ociKeyPreference;
    private Preference genDbPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_prefs);
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        ociKeyPreference = (EditTextPreference) findPreference("oci_key_preference");
        genDbPreference = findPreference("generate_database");

        bindPreferenceSummaryToValue(ociKeyPreference);
        bindPreferenceSummaryToValue(findPreference("mcc_filter_preference"));
        bindPreferenceSummaryToValue(findPreference("mnc_filter_preference"));
        bindPreferenceSummaryToValue(genDbPreference);

        findPreference("request_new_api_key").setOnPreferenceClickListener(this);
        genDbPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.d(TAG, "onResume(): setting summary with current database info");
        genDbPreference.setSummary(createCurrentDatabaseSummary());
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
        onPreferenceChange(preference, sp.getString(preference.getKey(), ""));
    }

    private String createCurrentDatabaseSummary() {
        DateFormat df = SimpleDateFormat.getDateTimeInstance();
        String summary;

        if (Config.DB_NEW_FILE.exists()) {
            if (Config.DB_NEW_FILE.canRead()) {
                summary = getString(R.string.db_file_last_modified,
                        df.format(Config.DB_NEW_FILE.lastModified()));
            } else {
                summary = getString(R.string.db_file_unreadable, Config.DB_NEW_NAME);
            }
        } else if (Config.DB_FILE.exists()) {
            if (Config.DB_FILE.canRead()) {
                summary = getString(R.string.db_file_last_modified,
                        df.format(Config.DB_FILE.lastModified()));
            } else {
                summary = getString(R.string.db_file_unreadable, Config.DB_NAME);
            }
        } else {
            summary = getString(R.string.db_file_not_found);
        }

        return summary;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String prefKey = preference.getKey();

        if (prefKey.equals("oci_key_preference")) {
            String newKey = newValue.toString();
            if (!newKey.isEmpty() && !isOpenCellIdKeyValid(newKey)) {
                new AlertDialog.Builder(getActivity()).setMessage(R.string.invalid_oci_api_key)
                        .setCancelable(false).setPositiveButton(android.R.string.ok, null).show();
                return false;
            } else {
                preference.setSummary(newValue.toString());
            }
        } else if (prefKey.equals("generate_database")) {
            preference.setSummary(createCurrentDatabaseSummary());
        } else {
            preference.setSummary(newValue.toString());
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String prefKey = preference.getKey();

        if (prefKey.equals("request_new_api_key")) {
            if (DEBUG) Log.i(TAG, "Requesting new OpenCellID API key");
            if (sp.getString("oci_key_preference", "").isEmpty()) {
                new RequestOpenCellIdKeyTask().execute();
            } else {
                new AlertDialog.Builder(getActivity()).setMessage(R.string.oci_key_already_entered)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new RequestOpenCellIdKeyTask().execute();
                            }
                        }).show();
            }
            return true;
        } else if (prefKey.equals("generate_database")) {
            validatePrefsAndLaunchDownload();
            return true;
        }

        return false;
    }

    private void validatePrefsAndLaunchDownload() {
        boolean downloadOci = sp.getBoolean("oci_preference", false);
        boolean downloadMls = sp.getBoolean("mls_preference", false);

        if (!downloadOci && !downloadMls) {
            new AlertDialog.Builder(getActivity()).setMessage(R.string.no_data_requested)
                    .setCancelable(false).setPositiveButton(android.R.string.ok, null).show();
        } else if (downloadOci && sp.getString("oci_key_preference", "").isEmpty()) {
            new AlertDialog.Builder(getActivity()).setMessage(R.string.no_api_key)
                    .setCancelable(false).setPositiveButton(android.R.string.ok, null).show();
        } else {
            startActivity(new Intent(getActivity(), DownloadActivity.class));
        }
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
