package org.fitchfamily.android.gsmlocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.location.Location;
import android.os.Bundle;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.microg.nlp.api.LocationHelper;

class telephonyHelper {
    protected String TAG = appConstants.TAG_PREFIX + "telephonyHelper";
    private static boolean DEBUG = appConstants.DEBUG;

    private TelephonyManager tm = null;
    private CellLocationFile db = new CellLocationFile();


    public telephonyHelper( TelephonyManager teleMgr ) {
        tm = teleMgr;
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
            if (DEBUG) Log.d(TAG, "no such member: getAllCellInfo().");
        }
        if ((allCells == null) || allCells.isEmpty()) {
            if (DEBUG) Log.d(TAG, "getAllCellInfo()  returned null or empty set");
            return null;
        }

        List<Location> rslt = new ArrayList<Location>();
        for (android.telephony.CellInfo inputCellInfo : allCells) {
            Location cellLocation = null;
            if (inputCellInfo instanceof CellInfoGsm) {
                CellInfoGsm gsm = (CellInfoGsm) inputCellInfo;
                CellIdentityGsm id = gsm.getCellIdentity();
                cellLocation = db.query(id.getMcc(), id.getMnc(), id.getCid(), id.getLac());
            } else if (inputCellInfo instanceof CellInfoWcdma) {
                CellInfoWcdma wcdma = (CellInfoWcdma) inputCellInfo;
                CellIdentityWcdma id = wcdma.getCellIdentity();
                cellLocation = db.query(id.getMcc(), id.getMnc(), id.getCid(), id.getLac());
            }

            if ((cellLocation != null)) {
                rslt.add(cellLocation);
            }
        }
        if ((rslt != null) && rslt.isEmpty())
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
                Log.d(TAG, "legacyGetCellTowers(): mncString is NULL or not recognized.");
            return null;
        }
        int mcc = Integer.parseInt(mncString.substring(0,3));
        int mnc = Integer.parseInt(mncString.substring(3));
        final CellLocation cellLocation = tm.getCellLocation();

        if ((cellLocation != null) && (cellLocation instanceof GsmCellLocation)) {
            GsmCellLocation cell = (GsmCellLocation) cellLocation;
            Location cellLocInfo = db.query(mcc, mnc, cell.getCid(), cell.getLac());
            if (cellLocInfo != null)
                rslt.add(cellLocInfo);
        } else {
            if (DEBUG)
                Log.d(TAG, "getCellLocation() returned null or no GsmCellLocation.");
        }

        final List<NeighboringCellInfo> neighbours = tm.getNeighboringCellInfo();
        if ((neighbours != null) && !neighbours.isEmpty()) {
            for (NeighboringCellInfo neighbour : neighbours) {
                Location cellLocInfo = db.query(mcc, mnc, neighbour.getCid(), neighbour.getLac());
                if (cellLocInfo != null) {
                    rslt.add(cellLocInfo);
                }
            }
        } else {
            if (DEBUG) Log.d(TAG, "getNeighboringCellInfo() returned null or empty set.");
        }
        if ((rslt != null) && rslt.isEmpty())
            return null;
        return rslt;
    }

    public synchronized List<Location> getTowerLocations() {
        if (tm == null)
            return null;

        db.checkForNewDatabase();
        List<Location> rslt = getAllCellInfoWrapper();
        if (rslt == null) {
            if (DEBUG) Log.d(TAG, "getAllCellInfoWrapper() returned nothing, trying legacyGetCellTowers().");
            rslt = legacyGetCellTowers();
        }
        if ((rslt == null) || rslt.isEmpty()) {
            if (DEBUG) Log.d(TAG, "getTowerLocations(): No tower information.");
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
                float thisAcc = (float) value.getAccuracy();
                if (thisAcc < 1f)
                    thisAcc = 1f;

                int wgt = (int) (100000f / thisAcc);
                if (wgt < 1)
                    wgt = 1;

                latitude += (value.getLatitude() * wgt);
                longitude += (value.getLongitude() * wgt);
                accuracy += (value.getAccuracy() * wgt);
                totalWeight += wgt;

//                if (DEBUG) Log.d(TAG, "(lat="+ latitude + ", lng=" + longitude + ", acc=" + accuracy + ") / wgt=" + totalWeight );

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
//        if (DEBUG) Log.d(TAG, "Location est (lat="+ latitude + ", lng=" + longitude + ", acc=" + accuracy);

        if (altitudes > 0) {
            rslt = LocationHelper.create(source,
                          latitude,
                          longitude ,
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
        return weightedAverage("gsm",getTowerLocations());
    }
}


