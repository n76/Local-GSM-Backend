package org.fitchfamily.android.gsmlocation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;

import org.microg.nlp.api.LocationHelper;

class CellLocationFile {
    private static final String TABLE_CELLS = "cells";
    private static final String COL_LATITUDE = "latitude";
    private static final String COL_LONGITUDE = "longitude";
    private static final String COL_ACCURACY = "accuracy";
    private static final String COL_SAMPLES = "samples";
    private static final String COL_MCC = "mcc";
    private static final String COL_MNC = "mnc";
    private static final String COL_LAC = "lac";
    private static final String COL_CID = "cid";
    private static File file;
    private SQLiteDatabase database;

    protected String TAG = appConstants.TAG_PREFIX+"database";
    private static boolean DEBUG = appConstants.DEBUG;

    /**
     * Used internally for caching. HashMap compatible entity class.
     */
    private static class QueryArgs {
        Integer mcc;
        Integer mnc;
        int cid;
        int lac;

        private QueryArgs(Integer mcc, Integer mnc, int cid, int lac) {
            this.mcc = mcc;
            this.mnc = mnc;
            this.cid = cid;
            this.lac = lac;
        }
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            QueryArgs queryArgs = (QueryArgs) o;

            if (cid != queryArgs.cid) return false;
            if (lac != queryArgs.lac) return false;
            if (mcc != null ? !mcc.equals(queryArgs.mcc) : queryArgs.mcc != null) return false;
            if (mnc != null ? !mnc.equals(queryArgs.mnc) : queryArgs.mnc != null) return false;

            return true;
        }
        public int hashCode() {
            int result = mcc != null ? mcc.hashCode() : (1 << 16);
            result = 31 * result + (mnc != null ? mnc.hashCode() : (1 << 16));
            result = 31 * result + cid;
            result = 31 * result + lac;
            return result;
        }

        public String toString() {
            return "mcc=" + mcc + ", mnc=" + mnc + ", lac=" + lac +", cid=" + cid;
        }
    }

    /**
     * DB negative query cache (not found in db).
     */
    private LruCache<QueryArgs, Boolean> queryResultNegativeCache =
            new LruCache<QueryArgs, Boolean>(10000);
    /**
     * DB positive query cache (found in the db).
     */
    private LruCache<QueryArgs, Location> queryResultCache =
            new LruCache<QueryArgs, Location>(10000);

    public void init(Context ctx) {
        openDatabase();
    }

    public void checkForNewDatabase() {
        if (appConstants.DB_NEW_FILE.exists() && appConstants.DB_NEW_FILE.canRead()) {
            if (DEBUG) Log.d(TAG, "New database file detected.");
            if (database != null)
                database.close();
            database = null;
            queryResultCache = new LruCache<QueryArgs, Location>(10000);
            queryResultNegativeCache = new LruCache<QueryArgs, Boolean>(10000);

            appConstants.DB_FILE.renameTo(appConstants.DB_BAK_FILE);
            appConstants.DB_NEW_FILE.renameTo(appConstants.DB_FILE);
        }
    }

    private void openDatabase() {
        if (database == null) {
            if (DEBUG) Log.d(TAG, "Attempting to open database.");
            this.file = appConstants.DB_FILE;
            if (file.exists() && file.canRead()) {
                database = SQLiteDatabase.openDatabase(file.getAbsolutePath(),
                                                       null,
                                                       SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            } else {
                Log.i(TAG, "Unable to open database "+appConstants.DB_FILE);
                database = null;
            }
        }
    }

    public void close() {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    public boolean exists() {
        return file.exists() && file.canRead();
    }

    public String getPath() {
        return file.getAbsolutePath();
    }

    public synchronized Location query(final Integer mcc, final Integer mnc, final int cid, final int lac) {
        List<String> specArgs = new ArrayList<String>();
        String delim = "";
        String bySpec = "";

        // short circuit duplicate calls
        QueryArgs args = new QueryArgs(mcc, mnc, cid, lac);
        Boolean negative = queryResultNegativeCache.get(args);
        if (negative != null && negative.booleanValue()) return null;

        Location cached = queryResultCache.get(args);
        if (cached != null) return cached;

        openDatabase();
        if (database == null) {
            if (DEBUG) Log.d(TAG, "Unable to open cell tower database file.");
            return null;
        }

        // Build up where clause and arguments based on what we were passed
        if (mcc != null) {
            bySpec = bySpec + delim + "mcc=?";
            delim = " AND ";
            specArgs.add(Integer.toString(mcc));
        }
        if (mnc != null) {
            bySpec = bySpec + delim + "mnc=?";
            delim = " AND ";
            specArgs.add(Integer.toString(mnc));
        }
        bySpec = bySpec + delim + "lac=?";
        delim = " AND ";
        specArgs.add(Integer.toString(lac));

        bySpec = bySpec + delim + "cid=?";
        specArgs.add(Integer.toString(cid));

        String[] specArgArry = new String[ specArgs.size() ];
        specArgs.toArray( specArgArry );

        Location cellLocInfo = null;

        Cursor cursor =
                database.query(TABLE_CELLS,
                               new String[]{COL_MCC,
                                            COL_MNC,
                                            COL_LAC,
                                            COL_CID,
                                            COL_LATITUDE,
                                            COL_LONGITUDE,
                                            COL_ACCURACY,
                                            COL_SAMPLES},
                               bySpec,
                               specArgArry,
                               null,
                               null,
                               null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {;
                int db_mcc = 0;
                int db_mnc = 0;
                int db_lac = 0;
                int db_cid = 0;

                double lat = 0d;
                double lng = 0d;
                double rng = 0d;
                int samples = 0;

                double thisLat = 0d;
                double thisLng = 0d;
                double thisRng = 0d;
                int thisSamples = 0;

                // Get weighted average of tower locations and coverage
                // range from reports by the various providers (OpenCellID,
                // Mozilla location services, etc.)
                while (!cursor.isLast()) {
                    cursor.moveToNext();
                    db_mcc = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MCC));
                    db_mnc = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MNC));
                    db_lac = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LAC));
                    db_cid = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CID));
                    thisLat = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE));
                    thisLng = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE));
                    thisRng = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ACCURACY));
                    thisSamples = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SAMPLES));
                    if (DEBUG) Log.d(TAG, "query result: " +
                                          db_mcc + ", " + db_mnc + ", " + db_lac + ", " + db_cid + ", " +
                                          thisLat + ", " + thisLng + ", " + thisRng + ", " + thisSamples);
                    if (thisSamples < 1)
                        thisSamples = 1;

                    lat += (thisLat * thisSamples);
                    lng += (thisLng * thisSamples);
                    if (thisRng > rng)
                        rng = thisRng;
                    samples += thisSamples;
                }
                if (DEBUG) Log.d(TAG, "Final result: " +
                                      db_mcc + ", " + db_mnc + ", " + db_lac + ", " + db_cid + ", " +
                                      lat/samples + ", " + lng/samples + ", " + rng );
                Bundle extras = new Bundle();
                cellLocInfo = LocationHelper.create("gsm", (float)lat/samples, (float)lng/samples, (float)rng, extras);
                queryResultCache.put(args, cellLocInfo);
                if (DEBUG) Log.d(TAG,"Cell info found: "+args.toString());
            } else {
                if (DEBUG) Log.d(TAG,"DB Cursor empty for: "+args.toString());
                queryResultNegativeCache.put(args, true);
            }
            cursor.close();
        } else {
            if (DEBUG) Log.d(TAG,"DB Cursor null for: "+args.toString());
            queryResultNegativeCache.put(args, true);
        }
        return cellLocInfo;
    }
}
