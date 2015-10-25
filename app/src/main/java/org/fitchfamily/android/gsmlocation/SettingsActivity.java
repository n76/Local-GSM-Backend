package org.fitchfamily.android.gsmlocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.content.Intent;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

public class SettingsActivity extends Activity {
    private static final String TAG = makeLogTag(SettingsActivity.class);
    private static final boolean DEBUG = Config.DEBUG;

    private boolean oci_preference;
    private boolean mls_preference;
    private String oci_key_preference;
    private String mcc_filter_preference;
    private String mnc_filter_preference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public void doCheckParameters(View theButton) {

        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        oci_preference = mySharedPreferences.getBoolean("oci_preference", false);
        mls_preference = mySharedPreferences.getBoolean("mls_preference", false);
        oci_key_preference = mySharedPreferences.getString("oci_key_preference", "");
        mcc_filter_preference = mySharedPreferences.getString("mcc_filter_preference", "");
        mnc_filter_preference = mySharedPreferences.getString("mnc_filter_preference", "");

        if (DEBUG) {
            Log.d(TAG, "Use OpenCellID data = " + String.valueOf(oci_preference));
            Log.d(TAG, "Use Mozilla data = " + String.valueOf(mls_preference));
            Log.d(TAG, "OpenCellId API Key = " + oci_key_preference);
            Log.d(TAG, "MCC filtering = " + mcc_filter_preference);
            Log.d(TAG, "MNC filtering = " + mnc_filter_preference);
        }

        if (oci_preference && oci_key_preference.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.no_api_key));
            builder.setCancelable(false);

            builder.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, "onClick = yes, OCI = " + String.valueOf(oci_preference));
                    oci_preference = false;
                    checkDataSources();
                    Log.d(TAG, "onClick = yes, OCI = " + String.valueOf(oci_preference));
                }
            });

            builder.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
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
        if (!oci_preference && !mls_preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.no_data_requested));
            builder.setCancelable(false);

            builder.setNeutralButton(getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();

            if (DEBUG)
                Log.d(TAG, "Neither OpenCellId nor Mozilla Location data selected");

            return;
        }
        genDatabase(oci_preference, mls_preference, oci_key_preference, mcc_filter_preference, mnc_filter_preference);
    }

    public void genDatabase(boolean useOCI, boolean useMLS, String OciKey, String MccFilter, String MncFilter) {
        if (DEBUG) {
            Log.d(TAG, "Inputs validated: Start background processing...");
            Log.d(TAG, "Use OpenCellID data = " + String.valueOf(useOCI));
            Log.d(TAG, "Use Mozilla data = " + String.valueOf(useMLS));
            Log.d(TAG, "OpenCellId API Key = " + OciKey);
            Log.d(TAG, "MCC filtering = " + MccFilter);
            Log.d(TAG, "MNC filtering = " + MncFilter);
        }

        Intent myIntent = new Intent(this, DownloadActivity.class);
        myIntent.putExtra("doOCI", useOCI);
        myIntent.putExtra("doMLS", useMLS);
        myIntent.putExtra("ociAPI", OciKey);
        myIntent.putExtra("mccFilter", MccFilter);
        myIntent.putExtra("mncFilter", MncFilter);
        startActivity(myIntent);
    }

}
