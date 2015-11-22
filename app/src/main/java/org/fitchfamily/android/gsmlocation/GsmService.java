package org.fitchfamily.android.gsmlocation;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

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
        th = new TelephonyHelper(tm);

        try {
            if (worker != null && worker.isAlive()) worker.interrupt();

            worker = new Thread() {

                public void run() {
                    if (DEBUG) Log.d(TAG, "Starting reporter thread");
                    Looper.prepare();

                    final PhoneStateListener listener = new PhoneStateListener() {

                        private boolean sameLoc(Location l1, Location l2) {

                            if (l1 == null && l2 == null)
                                return true;

                            if (l1 == null && l2 != null)
                                return false;

                            if(l1 != null && l2 == null)
                                return false;

                            return (l1.getLatitude() == l2.getLatitude()) &&
                                   (l1.getLongitude() == l2.getLongitude()) &&
                                   (l1.getAccuracy() == l2.getAccuracy());
                        }

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

                                if (!sameLoc(lastLocation, rslt)) {
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
        super.onOpen();

        start();

        if (DEBUG)
            Log.d(TAG, "Binder OPEN called");
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
