package org.fitchfamily.android.gsmlocation.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import org.fitchfamily.android.gsmlocation.Config;
import org.microg.nlp.api.LocationHelper;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

public class CellLocationDatabase {
    private static final String TAG = makeLogTag("database");
    private static final boolean DEBUG = Config.DEBUG;

    private static final String TABLE_CELLS = "cells";
    private static final String COL_LATITUDE = "latitude";
    private static final String COL_LONGITUDE = "longitude";
    private static final String COL_ACCURACY = "accuracy";
    private static final String COL_SAMPLES = "samples";
    private static final String COL_MCC = "mcc";
    private static final String COL_MNC = "mnc";
    private static final String COL_LAC = "lac";
    private static final String COL_CID = "cid";
    private SQLiteDatabase database;

    private QueryCache queryCache = new QueryCache();

    public void checkForNewDatabase() {
        if (Config.DB_NEW_FILE.exists() && Config.DB_NEW_FILE.canRead()) {
            if (DEBUG)
                Log.d(TAG, "New database file detected.");
            if (database != null)
                database.close();

            database = null;

            Config.DB_FILE.renameTo(Config.DB_BAK_FILE);
            Config.DB_NEW_FILE.renameTo(Config.DB_FILE);
        }
    }

    private void openDatabase() {
        if (database == null) {
            if (DEBUG)
                Log.d(TAG, "Attempting to open database.");

            if (Config.DB_FILE.exists() && Config.DB_FILE.canRead()) {
                try {
                    database = SQLiteDatabase.openDatabase(Config.DB_FILE.getAbsolutePath(),
                            null,
                            SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                } catch (Exception e) {
                    if (DEBUG) Log.e(TAG, "Error opening database: "+ e.getMessage());
                    database = null;
                    Config.DB_FILE.delete();
                    if (Config.DB_BAK_FILE.exists() && Config.DB_BAK_FILE.canRead()) {
                        if (DEBUG) Log.e(TAG, "Reverting to old database");
                        Config.DB_BAK_FILE.renameTo(Config.DB_FILE);
                        openDatabase();
                    }
                }
            } else {
                if (DEBUG) Log.e(TAG, "Unable to open database "+ Config.DB_FILE);
                database = null;
            }
        }
    }

    public synchronized Location query(final Integer mcc, final Integer mnc, final int cid, final int lac) {
        SqlWhereBuilder queryBuilder = new SqlWhereBuilder();

        // short circuit duplicate calls
        QueryArgs args = new QueryArgs(mcc, mnc, cid, lac);

        if(queryCache.contains(args)) {
            return queryCache.get(args);
        }

        openDatabase();
        if (database == null) {
            if (DEBUG)
                Log.d(TAG, "Unable to open cell tower database file.");
            return null;
        }

        // Build up where clause and arguments based on what we were passed
        if (mcc != null) {
            queryBuilder
                    .columnIs(COL_MCC, String.valueOf(mcc))
                    .and();
        }

        if (mnc != null) {
            queryBuilder
                    .columnIs(COL_MNC, String.valueOf(mnc))
                    .and();
        }

        queryBuilder
                .columnIs(COL_LAC, String.valueOf(lac))
                .and()
                .columnIs(COL_CID, String.valueOf(cid));

        Cursor cursor =
                database.query(TABLE_CELLS, new String[]{COL_MCC,
                                COL_MNC,
                                COL_LAC,
                                COL_CID,
                                COL_LATITUDE,
                                COL_LONGITUDE,
                                COL_ACCURACY,
                                COL_SAMPLES},
                        queryBuilder.selection(), queryBuilder.selectionArgs(), null, null, null);

        try {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    int db_mcc = 0;
                    int db_mnc = 0;
                    int db_lac = 0;
                    int db_cid = 0;

                    double lat = 0d;
                    double lng = 0d;
                    double rng = 0d;
                    int samples = 0;

                    double thisLat;
                    double thisLng;
                    double thisRng;
                    int thisSamples;

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
                            lat / samples + ", " + lng / samples + ", " + rng);
                    Bundle extras = new Bundle();
                    Location cellLocInfo = LocationHelper.create("gsm", (float) lat / samples, (float) lng / samples, (float) rng, extras);
                    queryCache.put(args, cellLocInfo);
                    if (DEBUG)
                        Log.d(TAG, "Cell info found: " + args.toString());
                    return cellLocInfo;
                } else {
                    if (DEBUG)
                        Log.d(TAG, "DB Cursor empty for: " + args.toString());
                    queryCache.putUnresolved(args);
                }
            } else {
                if (DEBUG)
                    Log.d(TAG, "DB Cursor null for: " + args.toString());
                queryCache.putUnresolved(args);
            }

            return null;
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }
}
