package org.fitchfamily.android.gsmlocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

public class settings extends Activity {
    protected String TAG = appConstants.TAG_PREFIX+"settings";
    private static boolean DEBUG = appConstants.DEBUG;

    private boolean oci_preference;
    private boolean mls_preference;
    private String oci_key_preference;
    private String mcc_filter_preference;

    class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                                                        new PrefsFragment()).commit();
    }

    public synchronized void setOCI(boolean val) {
        oci_preference = val;
    }

    public synchronized boolean getOCI() {
        return oci_preference;
    }

    public void doCheckParameters(View theButton) {

        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setOCI(mySharedPreferences.getBoolean("oci_preference", false));
        mls_preference = mySharedPreferences.getBoolean("mls_preference", false);
        oci_key_preference = mySharedPreferences.getString("oci_key_preference", "");
        mcc_filter_preference = mySharedPreferences.getString("mcc_filter_preference", "");

        if (DEBUG) {
            Log.d(TAG, "Use OpenCellID data = " + String.valueOf(getOCI()));
            Log.d(TAG, "Use Mozilla data = " + String.valueOf(mls_preference));
            Log.d(TAG, "OpenCellId API Key = " + oci_key_preference);
            Log.d(TAG, "MCC filtering = " + mcc_filter_preference);
        }
        if (getOCI() && oci_key_preference.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.no_api_key));
            builder.setCancelable(false);
            builder.setPositiveButton(getString(R.string.yes_string),
                                      new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, "onClick = yes, OCI = " + String.valueOf(getOCI()));
                    setOCI(false);
                    checkDataSources();
                    Log.d(TAG, "onClick = yes, OCI = " + String.valueOf(getOCI()));
                }
            });
            builder.setNegativeButton(getString(R.string.no_string),
                                      new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            checkDataSources();
        }
    }

    public void checkDataSources() {
        if (!getOCI() && !mls_preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.no_data_requested));
            builder.setCancelable(false);
            builder.setNeutralButton(getString(R.string.okay_string),
                                      new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            if (DEBUG) Log.d(TAG, "Neither OpenCellId nor Mozilla Location data selected");
            return;
        }
        genDatabase(getOCI(), mls_preference, oci_key_preference, mcc_filter_preference );
    }

    public void genDatabase(boolean useOCI, boolean useMLS, String OciKey, String MccFilter) {
        if (DEBUG) Log.d(TAG, "Inputs validated: Start background processing...");
        if (DEBUG) {
            Log.d(TAG, "Use OpenCellID data = " + String.valueOf(getOCI()));
            Log.d(TAG, "Use Mozilla data = " + String.valueOf(mls_preference));
            Log.d(TAG, "OpenCellId API Key = " + oci_key_preference);
            Log.d(TAG, "MCC filtering = " + mcc_filter_preference);
        }
    }
}
