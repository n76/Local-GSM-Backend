package org.fitchfamily.android.gsmlocation;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

@TargetApi(23)
public class ReqLocationPermActivity extends Activity {
    private static final String TAG = makeLogTag(ReqLocationPermActivity.class);
    private static final boolean DEBUG = Config.DEBUG;

    public static final int NOTIFICATION_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "onCreate() called");
        super.onCreate(savedInstanceState);

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (DEBUG) Log.d(TAG, "onRequestPermissionsResult() called");

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.app_name)
                .setCancelable(false).setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            builder.setMessage(R.string.dialog_location_perm_granted).show();
        } else {
            builder.setMessage(R.string.dialog_location_perm_denied).show();
        }

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
    }
}
