package org.fitchfamily.android.gsmlocation;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.microg.nlp.api.LocationBackendService;
import org.microg.nlp.api.LocationHelper;

public class GSMService extends LocationBackendService {

    private TelephonyManager tm = null;
    private telephonyHelper th = null;

    protected String TAG = appConstants.TAG_PREFIX+"service";
    private static boolean DEBUG = appConstants.DEBUG;

    protected Thread worker = null;

    private Location lastLocation = null;

    public synchronized void start() {
        if (worker != null && worker.isAlive()) return;

        if (DEBUG) Log.d(TAG, "Starting location backend");
        Handler handler = new Handler(Looper.getMainLooper());
        final Context ctx = getApplicationContext();
        tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        th = new telephonyHelper(tm);

        try {
            if (worker != null && worker.isAlive()) worker.interrupt();
            worker = new Thread() {
                public void run() {
                    if (DEBUG) Log.d(TAG, "Starting reporter thread");
                    Looper.prepare();

                    final PhoneStateListener listener = new PhoneStateListener() {

                        Location lastLocation = null;

                        private boolean sameLoc(Location l1, Location l2) {
                            if ((l1 == null) && (l2 == null)) {
                                return true;
                            }
                            if ((l1 == null) && (l2 != null)) {
                                return false;
                            }
                            if ((l1 != null) && (l2 == null)) {
                                return false;
                            }
                            return (l1.getLatitude() == l2.getLatitude()) &&
                                   (l1.getLongitude() == l2.getLongitude()) &&
                                   (l1.getAccuracy() == l2.getAccuracy());
                        }

                        private synchronized void doIt(String from) {
                            if (isConnected()) {
//                                if (DEBUG) Log.d(TAG,"doIt() entry");
                                long entryTime = System.currentTimeMillis();
                                Location rslt = th.getLocationEstimate();
                                String logString = "";
                                if (rslt != null)
                                    logString = from + rslt.toString();
                                else
                                    logString = from + " null position";
                                if (!sameLoc(lastLocation, rslt)) {
//                                    if (DEBUG) Log.d(TAG, logString);
                                    if (rslt != null)
                                        report(rslt);
                                }
                                lastLocation = rslt;
//                                if (DEBUG) Log.d(TAG,"doIt() exit - "+(System.currentTimeMillis()-entryTime)+"ms");
                            }
                        }

//                         public void onSignalStrengthsChanged(SignalStrength signalStrength) {
//                             doIt("onSignalStrengthsChanged: ");
//                         }
                        public void onServiceStateChanged(ServiceState serviceState) {
                            doIt("onServiceStateChanged: ");
                        }
                        public void onCellLocationChanged(CellLocation location) {
                            doIt("onCellLocationChanged: ");
                        }
//                         public void onDataConnectionStateChanged(int state) {
//                             doIt("onDataConnectionStateChanged: ");
//                         }
                        public void onCellInfoChanged(List<android.telephony.CellInfo> cellInfo) {
                            doIt("onCellInfoChanged: ");
                        }
                    };
                    tm.listen(
                        listener,
//                        PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
                        PhoneStateListener.LISTEN_CELL_INFO |
                        PhoneStateListener.LISTEN_CELL_LOCATION |
//                        PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                        PhoneStateListener.LISTEN_SERVICE_STATE
                    );
                }
            };
            worker.start();
        } catch (Exception e) {
            Log.e(TAG, "Start failed", e);
            worker = null;
        }
    }

//     @Override
//     protected synchronized Location update() {
//         start();
//         Location rslt = th.getLocationEstimate();
//         Log.e(TAG, rslt.toString());
//         return rslt;
//     }

    @Override
    protected synchronized void onOpen() {
        super.onOpen();

        start();

        if (DEBUG) Log.d(TAG, "Binder OPEN called");
    }

    protected synchronized void onClose() {
        if (DEBUG) Log.d(TAG, "Binder CLOSE called");
        super.onClose();
        try {
            if (worker != null && worker.isAlive()) worker.interrupt();
            if (worker != null) worker = null;
        } finally {
            worker = null;
        }
    }

}
