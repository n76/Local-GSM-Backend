package org.fitchfamily.android.gsmlocation;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.Context;
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

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = makeLogTag(SettingsFragment.class);
    private static final boolean DEBUG = Config.DEBUG;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_prefs);

        final EditTextPreference ociKeyPreference = (EditTextPreference) this.findPreference("oci_key_preference");
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
                try {
                    new GetOpenCellIDKeyTask().execute(mContext.getString(R.string.opencellid_api_get_key));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
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
    public void onAttach(Activity activity) {
        mContext = activity.getApplicationContext();
        super.onAttach(activity);
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
     * This might be extended in the future.
     * Two keys I started started with `dev-usr`, not sure if that's a rule.
     */
    private boolean isKeyValid(String key) {
        return key.startsWith("dev-");
    }

    private class GetOpenCellIDKeyTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(mContext.getString(R.string.opencellid_api_get_key));
                OcidResponse result;

                result = new OcidResponse(httpclient.execute(httpGet));

                if (result.getStatusCode() == 200) {
                    String responseFromServer = result.getResponseFromServer();
                    return responseFromServer;
                } else if (result.getStatusCode() == 503) {
                    // Check for HTTP error code 503 which is returned when user is trying to request
                    String responseFromServer = result.getResponseFromServer();
                    Log.d(TAG, "CellTracker: OpenCellID reached 24hrs API key limit: " + responseFromServer);
                    return responseFromServer;
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String newKey) {
            if (isKeyValid(newKey)) {
                Log.i(TAG, "New key is valid");
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
                EditTextPreference ociKeyPreference = (EditTextPreference) findPreference("oci_key_preference");
                sp.edit().putString("oci_key_preference", newKey).commit();
                ociKeyPreference.setSummary(newKey); //refresh summary
                Toast.makeText(mContext, mContext.getString(R.string.new_key_saved), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Invalid key: " + newKey);
                Toast.makeText(mContext, mContext.getString(R.string.one_request_per_24h), Toast.LENGTH_SHORT).show();
            }

        }
    }

}
