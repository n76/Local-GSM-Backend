package org.fitchfamily.android.gsmlocation;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.telephony.CellIdentityGsm;
import android.telephony.CellInfoGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfoLte;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import org.fitchfamily.android.gsmlocation.database.CellLocationDatabase;
import org.microg.nlp.api.LocationHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

public class TelephonyHelper {
    private static final String TAG = makeLogTag(TelephonyHelper.class);
    private static final boolean DEBUG = Config.DEBUG;

    /* Reflection-based shims to use CellInfoWcdma and stay compatible with API level 17 */
    private static class CellIdentityWcdma {
        private static Class<?> mCls;
        private static Method mGetCid;
        private static Method mGetLac;
        private static Method mGetMcc;
        private static Method mGetMnc;
        private static Method mGetPsc;
        static {
            try {
                mCls = Class.forName("android.telephony.CellIdentityWcdma");
                mGetCid = mCls.getMethod("getCid");
                mGetLac = mCls.getMethod("getLac");
                mGetMcc = mCls.getMethod("getMcc");
                mGetMnc = mCls.getMethod("getMnc");
                mGetPsc = mCls.getMethod("getPsc");
            } catch (final ClassNotFoundException e) {
            } catch (final NoSuchMethodException e) {
            }
        }
        private final Object mObj;
        public CellIdentityWcdma(final Object obj) {
            mObj = obj;
        }
        public int getCid() throws IllegalAccessException, InvocationTargetException {
            return ((Integer)mGetCid.invoke(mObj)).intValue();
        }
        public int getLac() throws IllegalAccessException, InvocationTargetException {
            return ((Integer)mGetLac.invoke(mObj)).intValue();
        }
        public int getMcc() throws IllegalAccessException, InvocationTargetException {
            return ((Integer)mGetMcc.invoke(mObj)).intValue();
        }
        public int getMnc() throws IllegalAccessException, InvocationTargetException {
            return ((Integer)mGetMnc.invoke(mObj)).intValue();
        }
        public int getPsc() throws IllegalAccessException, InvocationTargetException {
            return ((Integer)mGetPsc.invoke(mObj)).intValue();
        }
    }

    private static class CellSignalStrengthWcdma {
        private static Class<?> mCls;
        private static Method mGetAsuLevel;
        static {
            try {
                mCls = Class.forName("android.telephony.CellSignalStrengthWcdma");
                mGetAsuLevel = mCls.getMethod("getAsuLevel");
            } catch (final ClassNotFoundException e) {
            } catch (final NoSuchMethodException e) {
            }
        }
        private final Object mObj;
        public CellSignalStrengthWcdma(final Object obj) {
            mObj = obj;
        }
        public int getAsuLevel() throws IllegalAccessException, InvocationTargetException {
            return ((Integer)mGetAsuLevel.invoke(mObj)).intValue();
        }
    }

    private static class CellInfoWcdma {
        private static Class<?> mCls;
        private static Method mGetCellIdentity;
        private static Method mGetCellSignalStrength;
        static {
            try {
                mCls = Class.forName("android.telephony.CellInfoWcdma");
                mGetCellIdentity = mCls.getMethod("getCellIdentity");
                mGetCellSignalStrength = mCls.getMethod("getCellSignalStrength");
            } catch (final ClassNotFoundException e) {
            } catch (final NoSuchMethodException e) {
            }
        }
        private final Object mObj;
        public CellInfoWcdma(final Object obj) {
            mObj = obj;
        }
        public static boolean isInstance(final Object obj) {
            return null != mCls && mCls.isInstance(obj);
        }
        public CellIdentityWcdma getCellIdentity()
                throws IllegalAccessException, InvocationTargetException {
            return new CellIdentityWcdma(mGetCellIdentity.invoke(mObj));
        }
        public CellSignalStrengthWcdma getCellSignalStrength()
                throws IllegalAccessException, InvocationTargetException {
            return new CellSignalStrengthWcdma(mGetCellSignalStrength.invoke(mObj));
        }
    }

    private TelephonyManager tm;
    private CellLocationDatabase db;

    public TelephonyHelper(Context context) {
        tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        db = new CellLocationDatabase(context);
    }

    // call getAllCellInfo() in a way that is safe for many
    // versions of Android. If does not exist, returns null or
    // returns an empty list then return null otherwise return
    // list of cells.
    public synchronized List<Location> getAllCellInfoWrapper() {
        if (tm == null)
            return null;

        List<android.telephony.CellInfo> allCells;

        try {
            allCells = tm.getAllCellInfo();
        } catch (NoSuchMethodError e) {
            allCells = null;
            if (DEBUG) Log.i(TAG, "no such method: getAllCellInfo().");
        }
        if ((allCells == null) || allCells.isEmpty()) {
            if (DEBUG) Log.i(TAG, "getAllCellInfo()  returned null or empty set");
            return null;
        }

        List<Location> rslt = new ArrayList<Location>();
        for (android.telephony.CellInfo inputCellInfo : allCells) {
            Location cellLocation = null;
            if (inputCellInfo instanceof CellInfoGsm) {
                CellInfoGsm gsm = (CellInfoGsm) inputCellInfo;
                CellIdentityGsm id = gsm.getCellIdentity();
                cellLocation = db.query(id.getMcc(), id.getMnc(), id.getCid(), id.getLac());
            } else if (inputCellInfo instanceof CellInfoLte) {
                CellInfoLte lte = (CellInfoLte) inputCellInfo;
                CellIdentityLte id = lte.getCellIdentity();
                cellLocation = db.query(id.getMcc(), id.getMnc(), id.getCi(), id.getTac());
            } else if (CellInfoWcdma.isInstance(inputCellInfo)) {
                try {
                    CellInfoWcdma wcdma = new CellInfoWcdma(inputCellInfo);
                    CellIdentityWcdma id = wcdma.getCellIdentity();
                    cellLocation = db.query(id.getMcc(), id.getMnc(), id.getCid(), id.getLac());
                } catch(IllegalAccessException e) {
                    if (DEBUG)
                        Log.i(TAG, "getAllCellInfoWrapper(), Wcdma: " + e.toString());
                } catch(InvocationTargetException e) {
                    if (DEBUG)
                        Log.i(TAG, "getAllCellInfoWrapper(), Wcdma: " + e.toString());
                }
            }

            if ((cellLocation != null)) {
                rslt.add(cellLocation);
            }
        }
        if (rslt.isEmpty())
            return null;
        return rslt;
    }

    public synchronized List<Location> legacyGetCellTowers() {
        if (tm == null)
            return null;

        List<Location> rslt = new ArrayList<Location>();
        String mncString = tm.getNetworkOperator();

        if ((mncString == null) || (mncString.length() < 5) || (mncString.length() > 6)) {
            if (DEBUG)
                Log.i(TAG, "legacyGetCellTowers(): mncString is NULL or not recognized.");
            return null;
        }
        int mcc = 0;
        int mnc = 0;
        try {
            mcc = Integer.parseInt(mncString.substring(0, 3));
            mnc = Integer.parseInt(mncString.substring(3));
        } catch (NumberFormatException e) {
            if (DEBUG)
                Log.i(TAG, "legacyGetCellTowers(), Unable to parse mncString: " + e.toString());
            return null;
        }
        final CellLocation cellLocation = tm.getCellLocation();

        if ((cellLocation != null) && (cellLocation instanceof GsmCellLocation)) {
            GsmCellLocation cell = (GsmCellLocation) cellLocation;
            Location cellLocInfo = db.query(mcc, mnc, cell.getCid(), cell.getLac());
            if (cellLocInfo != null)
                rslt.add(cellLocInfo);
            else if (DEBUG)
                Log.i(TAG, "Unknown cell tower detected: mcc="+mcc+
                        ", mnc="+mcc+", cid="+cell.getCid()+", lac="+cell.getLac());
        } else {
            if (DEBUG)
                Log.i(TAG, "getCellLocation() returned null or no GsmCellLocation.");
        }

        try {
            final List<NeighboringCellInfo> neighbours = tm.getNeighboringCellInfo();
            if ((neighbours != null) && !neighbours.isEmpty()) {
                for (NeighboringCellInfo neighbour : neighbours) {
                    Location cellLocInfo = db.query(mcc, mnc, neighbour.getCid(), neighbour.getLac());
                    if (cellLocInfo != null) {
                        rslt.add(cellLocInfo);
                    }
                }
            } else {
                if (DEBUG) Log.i(TAG, "getNeighboringCellInfo() returned null or empty set.");
            }
        } catch (NoSuchMethodError e) {
            if (DEBUG) Log.i(TAG, "no such method: getNeighboringCellInfo().");
        }
        if (rslt.isEmpty() && DEBUG) {
            Log.i(TAG, "No known cell towers found.");
        }
        if (rslt.isEmpty())
            return null;
        return rslt;
    }

    public synchronized List<Location> getTowerLocations() {
        if (tm == null)
            return null;

        db.checkForNewDatabase();
        List<Location> rslt = getAllCellInfoWrapper();
        if (rslt == null) {
            if (DEBUG)
                Log.i(TAG, "getAllCellInfoWrapper() returned nothing, trying legacyGetCellTowers().");
            rslt = legacyGetCellTowers();
        }
        if ((rslt == null) || rslt.isEmpty()) {
            if (DEBUG) Log.i(TAG, "getTowerLocations(): No tower information.");
            return null;
        }
        return rslt;
    }

    public Location weightedAverage(String source, Collection<Location> locations) {
        Location rslt;

        if (locations == null || locations.size() == 0) {
            return null;
        }

        int num = locations.size();
        int totalWeight = 0;
        double latitude = 0;
        double longitude = 0;
        float accuracy = 0;
        int altitudes = 0;
        double altitude = 0;

        for (Location value : locations) {
            if (value != null) {
                // Create weight value based on accuracy. Higher accuracy
                // (lower tower radius/range) towers get higher weight.
                float thisAcc = value.getAccuracy();
                if (thisAcc < 1f)
                    thisAcc = 1f;

                int wgt = (int) (100000f / thisAcc);
                if (wgt < 1)
                    wgt = 1;

                latitude += (value.getLatitude() * wgt);
                longitude += (value.getLongitude() * wgt);
                accuracy += (value.getAccuracy() * wgt);
                totalWeight += wgt;

//                if (DEBUG) Log.i(TAG, "(lat="+ latitude + ", lng=" + longitude + ", acc=" + accuracy + ") / wgt=" + totalWeight );

                if (value.hasAltitude()) {
                    altitude += value.getAltitude();
                    altitudes++;
                }
            }
        }
        latitude = latitude / totalWeight;
        longitude = longitude / totalWeight;
        accuracy = accuracy / totalWeight;
        altitude = altitude / altitudes;
        Bundle extras = new Bundle();
        extras.putInt("AVERAGED_OF", num);
//        if (DEBUG) Log.i(TAG, "Location est (lat="+ latitude + ", lng=" + longitude + ", acc=" + accuracy);

        if (altitudes > 0) {
            rslt = LocationHelper.create(source,
                    latitude,
                    longitude,
                    altitude,
                    accuracy,
                    extras);
        } else {
            rslt = LocationHelper.create(source,
                    latitude,
                    longitude,
                    accuracy,
                    extras);
        }
        rslt.setTime(System.currentTimeMillis());
        return rslt;
    }

    public synchronized Location getLocationEstimate() {
        if (tm == null)
            return null;
        return weightedAverage("gsm", getTowerLocations());
    }
}


