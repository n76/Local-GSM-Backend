package org.fitchfamily.android.gsmlocation;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.fitchfamily.android.gsmlocation.util.LocationUtil;
import org.microg.nlp.api.LocationBackendService;

import java.util.List;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

public class GsmService extends LocationBackendService {
    private static final String TAG = makeLogTag("service");
    private static final boolean DEBUG = Config.DEBUG;

    private TelephonyManager tm;
    private TelephonyHelper th;

    protected Thread worker = null;

    private Location lastLocation = null;

    public synchronized void start() {

        if (worker != null && worker.isAlive())
            return;

        if (DEBUG)
            Log.d(TAG, "Starting location backend");

        final Context ctx = getApplicationContext();
        tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        th = new TelephonyHelper(ctx);

        try {
            if (worker != null && worker.isAlive()) worker.interrupt();

            worker = new Thread() {

                public void run() {
                    if (DEBUG) Log.d(TAG, "Starting reporter thread");
                    Looper.prepare();

                    final PhoneStateListener listener = new PhoneStateListener() {
                        private synchronized void doIt(String from) {
                            if (isConnected()) {
                                Location rslt = th.getLocationEstimate();
                                String logString;

                                if (rslt != null)
                                    logString = from + rslt.toString();
                                else
                                    logString = from + " null position";

                                if (DEBUG)
                                    Log.d(TAG, logString);

                                if (!LocationUtil.equals(lastLocation, rslt)) {
                                    if (DEBUG)
                                        Log.d(TAG, "Location Changed.");

                                    report(rslt);
                                }
                                lastLocation = rslt;
                            }
                        }

                        public void onServiceStateChanged(ServiceState serviceState) {
                            doIt("onServiceStateChanged: ");
                        }

                        public void onCellLocationChanged(CellLocation location) {
                            doIt("onCellLocationChanged: ");
                        }

                        public void onCellInfoChanged(List<android.telephony.CellInfo> cellInfo) {
                            doIt("onCellInfoChanged: ");
                        }
                    };
                    tm.listen(
                        listener,
                        PhoneStateListener.LISTEN_CELL_INFO |
                        PhoneStateListener.LISTEN_CELL_LOCATION |
                        PhoneStateListener.LISTEN_SERVICE_STATE
                    );
                    Looper.loop();
                }
            };
            worker.start();
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "Start failed: " + e.getMessage());
            e.printStackTrace();
            worker = null;
        }
    }

     @Override
     protected synchronized Location update() {
         return lastLocation;
     }

    @Override
    protected synchronized void onOpen() {
        if (DEBUG) Log.d(TAG, "Binder OPEN called");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context ctx = getApplicationContext();

            if (ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(ctx, ReqLocationPermActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

                Notification notification = new Notification.Builder(ctx)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.notification_location_permission))
                        .setSmallIcon(R.drawable.icon)
                        .setContentIntent(pendingIntent)
                        .build();

                ((NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE))
                        .notify(ReqLocationPermActivity.NOTIFICATION_ID, notification);
                return;
            }
        }

        start();
    }

    protected synchronized void onClose() {
        if (DEBUG) Log.d(TAG, "Binder CLOSE called");
        super.onClose();

        try {
            if (worker != null && worker.isAlive())
                worker.interrupt();

            if (worker != null)
                worker = null;

        } finally {
            worker = null;
        }
    }
}
