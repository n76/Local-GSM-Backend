package org.fitchfamily.android.gsmlocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.fitchfamily.android.gsmlocation.model.CellInfo;

import android.content.Context;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.LruCache;

/**
 * Keep track of the current location, dropping old cells over time. This class works by having a
 * "measurement" counter and a timestamp. The measurement counter is increased whenever the signal
 * strength and cell reaches a new value. This means that the measurement will stay rather static
 * if you are stationary.
 * The time counter is used to drop towers that are stalled. This is a
 * protective tool to avoid completely broken data.
 */
public class CellbasedLocationProvider {

    // The key purpose of this class is pulling all the APIs and trying to turn it into s.th.
    // remotely consistent...
    // Oh, and tracking the state and everything turns out to be rather easy _if_ you know how to
    // do it. Basically: Never discard towers unless they've aged beyond recognition. Advance an
    // internal time scale based on the stability of the gsm signal. That's about it.

    // About the "magic" number (or magic constants)

    // RSSI (receive signal strength indicator) is 0..31.
    // The internal cache accepts 16 different measurements. The external cache accepts a max age
    // of 16 measurements. This means that going from poorest to best reception will not kill any
    // seen towers.

    // Time based age: 6h should be enough between cell tower scans (at least for scans that we
    // catch!).

    // // // // // // // // // // // // // Singleton methods // // // // // // // // // // // // //

    // Singletons, while often regarded the bad taste of OOP, solve a single problem:
    // make sure that never ever there's a second instance running. Great for stuff like a listener
    // that might be resource hungry :-)

    /**
     * Internal instance for singleton lookup.
     */
    private static CellbasedLocationProvider ourInstance = new CellbasedLocationProvider();

    /**
     * Retrieve the singleton CellbasedLocationProvider instance.
     * @return CellbasedLocationProvider the instance.
     */
    public static CellbasedLocationProvider getInstance() {
        return ourInstance;
    }

    /**
     * Private constructor to disallow foreign instantiation.
     */
    private CellbasedLocationProvider() { }

    /**
     * The measurement clock, incremented whenever the signal state changes significantly.
     */
    private AtomicLong measurement = new AtomicLong(0);
    /**
     * Maximum age of a cell based on the measurement clock.
     */
    private long MAX_MEASUREMENT_AGE = 16;
    /**
     * Maximum age of a cell based on the timestamp age.
     */
    private long MAX_TIME_AGE = 6 * 60 * 60 * 1000;

    /**
     * Reference to the cell tower db.
     */
//    private CellTowerDatabase db = CellTowerDatabase.getInstance();
    private CellLocationFile db = new CellLocationFile();

    /**
     * List of cells that were recently available and can be resolved (aka long/lat is set)
     */
    private HashSet<CellInfo> recentCells = new HashSet<CellInfo>(17);
    /**
     * List of recent cells that were available but could not be resolved.
     */
    private HashSet<CellInfo> unusedCells = new HashSet<CellInfo>(17);

    /**
     * The current cell location, needed on signal strength change.
     */
    private GsmCellLocation location = null;

    /**
     * Update the internal list of unused (unresolved) cells.
     * @param ci The cell information.
     */
    private final void pushUnusedCells(CellInfo ci) {
        ci.sanitize();
        if (ci.isInvalid()) return ;
        synchronized (unusedCells) {
            boolean isNew = !unusedCells.remove(ci);
            ci.seen = System.currentTimeMillis();
            ci.measurement = measurement.get();
            unusedCells.add(ci);
            if (isNew)
                android.util.Log.d("LNLP/Cell/Unresolved", ci.toString());
        }
    }

    /**
     * Update the internal list of resolved cell information.
     * @param ci The cell information.
     */
    private final void pushRecentCells(CellInfo ci) {
        synchronized (recentCells) {
            boolean isNew = !recentCells.remove(ci);
            ci.seen = System.currentTimeMillis();
            ci.measurement = measurement.get();
            recentCells.add(ci);
            if (isNew)
                android.util.Log.d("LNLP/Cell", ci.toString());
        }
    }

    /**
     * Retrieve the list of recently resolved cells.
     * @return Array copy of CellInfo instances.
     */
    public CellInfo[] getAll() {
        synchronized (recentCells) {
            handle(false);
            cleanup();
            return recentCells.toArray(new CellInfo[recentCells.size()]);
        }
    }

    /**
     * All cells that are currently unused (can not resolved).
     * @return Array copy of CellInfo instances.
     */
    public CellInfo[] getAllUnused() {
        synchronized (unusedCells) {
            handle(false);
            cleanup();
            return unusedCells.toArray(new CellInfo[unusedCells.size()]);
        }
    }

    /**
     * Clean stalled entries within the recent/unused cell list.
     */
    public void cleanup() {
        ArrayList<CellInfo> dead = null;
        long mThreshold = measurement.get() - MAX_MEASUREMENT_AGE;
        long timeThreshold = System.currentTimeMillis() - MAX_TIME_AGE;
        for (CellInfo ci : recentCells) {
            boolean outdatedByAge = ci.seen <= timeThreshold;
            boolean outdatedByMeasurement = ci.measurement <= mThreshold;
            if (!outdatedByAge && !outdatedByMeasurement) continue;
            if (dead == null) {
                dead = new ArrayList<CellInfo>(recentCells.size() + 1);
            }
            String reason = "Cell outdated ";
            if (outdatedByMeasurement && !outdatedByAge) {
                reason = "Measurements reached ";
            }
            if (outdatedByAge && !outdatedByMeasurement) {
                reason = "Timeout reached ";
            }
            android.util.Log.d("LNLP/Cell/Died", reason + ci.toString());
            dead.add(ci);
        }
        if (dead != null) {
            recentCells.removeAll(dead);
        }
        if (dead != null) dead.clear();
        for (CellInfo ci : unusedCells) {
            boolean outdatedByAge = ci.seen <= timeThreshold;
            boolean outdatedByMeasurement = ci.measurement <= mThreshold;
            if (!outdatedByAge && !outdatedByMeasurement) continue;
            if (dead == null) {
                dead = new ArrayList<CellInfo>(unusedCells.size() + 1);
            }
            String reason = "Cell outdated ";
            if (outdatedByMeasurement && !outdatedByAge) {
                reason = "Measurements reached ";
            }
            if (outdatedByAge && !outdatedByMeasurement) {
                reason = "Timeout reached ";
            }
            android.util.Log.d("LNLP/Cell/Unused/Died", reason + ci.toString());
            dead.add(ci);
        }
        if (dead != null) {
            unusedCells.removeAll(dead);
        }
    }

    /**
     * Add a CellLocation, usually called if the phone switched to a new tower.
     * @param icell The new cell location.
     */
    public void add(CellLocation icell) {
        if (icell == null) {
            return;
        }
        if (!(icell instanceof GsmCellLocation)) {
            return;
        }
        GsmCellLocation cell = (GsmCellLocation) icell;
        List<CellInfo> cellInfos = db.query(null, null, cell.getCid(), cell.getLac());
        if (cellInfos != null && !cellInfos.isEmpty()) {
            long measurement = this.measurement.get();
            for (CellInfo cellInfo : cellInfos) {
                cellInfo.measurement = measurement;
                cellInfo.seen = System.currentTimeMillis();
                pushRecentCells(cellInfo);
            }
        } else {
            CellInfo ci = new CellInfo(-1,  // int mcc
                                       -1,  // int mnc
                                       ((GsmCellLocation) icell).getLac(), // int lac
                                       ((GsmCellLocation) icell).getCid(), // int cid
                                       0d,  // double latV
                                       0d); //  double lon
            ci.setMeasurement(this.measurement.get());
            pushUnusedCells(ci);
        }
    }

    /**
     * Add neighbouring cells as generated by the getNeighboringCells API.
     * @param neighbours The list of neighbouring cells.
     */
    public void addNeighbours(List<NeighboringCellInfo> neighbours) {
        if (neighbours == null || neighbours.isEmpty()) return;
        for (NeighboringCellInfo neighbour : neighbours) {
            List<CellInfo> cellInfos = db.query(null, null, neighbour.getCid(), neighbour.getLac());
            if (cellInfos != null && !cellInfos.isEmpty()) {
                for (CellInfo cellInfo : cellInfos) {
                    pushRecentCells(cellInfo);
                }
            } else {
                CellInfo ci = new CellInfo(-1,  // int mcc
                                           -1,  // int mnc
                                           neighbour.getLac(), // int lac
                                           neighbour.getCid(), // int cid
                                           0d,  // double latV
                                           0d); //  double lon
                pushUnusedCells(ci);
            }
        }
    }

    /**
     * Add a list of cells.
     * @param inputCellInfos
     */
    public void addCells(List<android.telephony.CellInfo> inputCellInfos) {
        if (inputCellInfos == null || inputCellInfos.isEmpty()) return;
        for (android.telephony.CellInfo inputCellInfo : inputCellInfos) {
            List<CellInfo> cellInfos = null;
            if (inputCellInfo instanceof CellInfoGsm) {
                CellInfoGsm gsm = (CellInfoGsm) inputCellInfo;
                CellIdentityGsm id = gsm.getCellIdentity();
                cellInfos = db.query(id.getMcc(), id.getMnc(), id.getCid(), id.getLac());
                if (cellInfos == null) {
                    CellInfo ci = new CellInfo(id.getMcc(),  // int mcc
                                               id.getMnc(),  // int mnc
                                               id.getLac(), // int lac
                                               id.getCid(), // int cid
                                               0d,  // double latV
                                               0d); //  double lon
                    pushUnusedCells(ci);
                }
            }
            if (inputCellInfo instanceof CellInfoWcdma) {
                CellInfoWcdma wcdma = (CellInfoWcdma) inputCellInfo;
                CellIdentityWcdma id = wcdma.getCellIdentity();
                cellInfos = db.query(id.getMcc(), id.getMnc(), id.getCid(), id.getLac());
                if (cellInfos == null) {
                    CellInfo ci = new CellInfo(id.getMcc(),  // int mcc
                                               id.getMnc(),  // int mnc
                                               id.getLac(), // int lac
                                               id.getCid(), // int cid
                                               0d,  // double latV
                                               0d); //  double lon
                    pushUnusedCells(ci);
                }
            }
            if (cellInfos == null) continue;

            if (!cellInfos.isEmpty()) {
                for (CellInfo cellInfo : cellInfos) {
                    pushRecentCells(cellInfo);
                }
            }
        }
    }

    /**
     * Handle a modem event by trying to pull all information. The parameter inc defines if the
     * measurement counter should be increased on success.
     * @param inc True if the measurement counter should be increased.
     */
    private void handle(boolean inc) {
        if (telephonyManager == null) return;
        final List<android.telephony.CellInfo> cellInfos = telephonyManager.getAllCellInfo();
        final List<NeighboringCellInfo> neighbours = telephonyManager.getNeighboringCellInfo();
        final CellLocation cellLocation = telephonyManager.getCellLocation();
        if (cellInfos == null || cellInfos.isEmpty()) {
            if (neighbours == null || neighbours.isEmpty()) {
                if (cellLocation == null || !(cellLocation instanceof GsmCellLocation)) return;
            }
        }
        if (inc) measurement.getAndIncrement();
        add(cellLocation);
        addNeighbours(neighbours);
        addCells(cellInfos);
        synchronized (recentCells) {
            cleanup();
        }
    }

    /**
     * The telephony manager used for modem queries.
     */
    private TelephonyManager telephonyManager;

    /**
     * SignalStringthInfo represents a single CID/LAC with a rssi. Used for lookups / caching.
     */
    private static class SignalStringthInfo {
        public int CID;
        public int LAC;
        public int rssi;

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SignalStringthInfo that = (SignalStringthInfo) o;

            if (CID != that.CID) return false;
            if (LAC != that.LAC) return false;
            if (rssi != that.rssi) return false;

            return true;
        }

        public int hashCode() {
            int result = CID;
            result = 31 * result + LAC;
            result = 31 * result + rssi;
            return result;
        }

        public String toString() {
            return "SignalStringthInfo{" +
                    "CID=" + CID +
                    ", LAC=" + LAC +
                    ", rssi=" + rssi +
                    '}';
        }
    }

    /**
     * Initialize the location provider.
     * @param ctx The application context.
     */
    public void init(Context ctx) {

        telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

        final Context fctx = ctx;
        new Thread() {
            public void run() {
                db.init(fctx);
            }
        }.start();

        /**
         * The <b>actual</b> phone listener, handling new modem based events.
         */
        final PhoneStateListener listener = new PhoneStateListener() {

            /**
             * A cache for the last few cell/strength combinations we've seen. This helps to
             * determine if we should count a measurement as a "new" measurement or if we should
             * simply add whatever we got without incrementing the cell based time.
             */
            private LruCache<SignalStringthInfo,SignalStringthInfo> recentSignals =
                    new LruCache<SignalStringthInfo, SignalStringthInfo>(16);

            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                if (location != null && location instanceof GsmCellLocation) {
                    SignalStringthInfo ssi = new SignalStringthInfo();
                    ssi.rssi = signalStrength.getGsmSignalStrength();
                    ssi.CID = ((GsmCellLocation) location).getCid();
                    ssi.LAC = ((GsmCellLocation) location).getLac();
                    boolean inc = false;
                    synchronized (recentSignals) {
                        inc = recentSignals.remove(ssi) == null;
                        recentSignals.put(ssi,ssi);
                    }
                    if (inc) {
                        android.util.Log.d("LNLP/Signal/Measurement", ssi.toString());
                        handle(true);
                        return;
                    }
                }
                handle(false);
            }
            public void onServiceStateChanged(ServiceState serviceState) {
                handle(true);
            }
            public void onCellLocationChanged(CellLocation location) {
                if (!(location instanceof GsmCellLocation)) return;
                CellbasedLocationProvider.this.location = (GsmCellLocation) location;
                measurement.getAndIncrement();
                add(location);
                handle(false);
            }
            public void onDataConnectionStateChanged(int state) {
                handle(false);
            }
            public void onCellInfoChanged(List<android.telephony.CellInfo> cellInfo) {
                measurement.getAndIncrement();
                addCells(cellInfo);
                handle(false);
            }
        };
        telephonyManager.listen(
            listener,
            PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
            PhoneStateListener.LISTEN_CELL_INFO |
            PhoneStateListener.LISTEN_CELL_LOCATION |
            PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
            PhoneStateListener.LISTEN_SERVICE_STATE
        );
    }

}
