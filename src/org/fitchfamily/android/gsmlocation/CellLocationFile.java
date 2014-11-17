package org.fitchfamily.android.gsmlocation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;


import org.fitchfamily.android.gsmlocation.model.myCellInfo;


class CellLocationFile {
    private static final String TABLE_CELLS = "cells";
    private static final String COL_LATITUDE = "latitude";
    private static final String COL_LONGITUDE = "longitude";
    private static final String COL_ACCURACY = "accuracy";
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
    private final LruCache<QueryArgs, Boolean> queryResultNegativeCache =
            new LruCache<QueryArgs, Boolean>(10000);
    /**
     * DB positive query cache (found in the db).
     */
    private final LruCache<QueryArgs, List<myCellInfo>> queryResultCache =
            new LruCache<QueryArgs, List<myCellInfo>>(10000);

    public void init(Context ctx) {
        openDatabase();
    }

    private void openDatabase() {
        if (database == null) {
            this.file = new File(appConstants.DB_FILE_NAME);
            if (file.exists() && file.canRead()) {
                database = SQLiteDatabase.openDatabase(file.getAbsolutePath(),
                                                       null,
                                                       SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            } else {
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

    public synchronized List<myCellInfo> query(final Integer mcc, final Integer mnc, final int cid, final int lac) {
        List<String> specArgs = new ArrayList<String>();
        String delim = "";
        String bySpec = "";

        // short circuit duplicate calls
        QueryArgs args = new QueryArgs(mcc, mnc, cid, lac);
        Boolean negative = queryResultNegativeCache.get(args);
        if (negative != null && negative.booleanValue()) return null;

        List<myCellInfo> cached = queryResultCache.get(args);
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

        Cursor cursor =
                database.query(TABLE_CELLS,
                               new String[]{COL_MCC,
                                            COL_MNC,
                                            COL_LAC,
                                            COL_CID,
                                            COL_LATITUDE,
                                            COL_LONGITUDE,
                                            COL_ACCURACY},
                               bySpec,
                               specArgArry,
                               null,
                               null,
                               null);
        if (cursor != null) {
            List<myCellInfo> ciList = new ArrayList<myCellInfo>();
            try {
                if (cursor.getCount() > 0) {
                    while (!cursor.isLast()) {
                        cursor.moveToNext();

                        myCellInfo ci = new myCellInfo(cursor.getInt(cursor.getColumnIndexOrThrow(COL_MCC)),
                                                   cursor.getInt(cursor.getColumnIndexOrThrow(COL_MNC)),
                                                   cursor.getInt(cursor.getColumnIndexOrThrow(COL_LAC)),
                                                   cursor.getInt(cursor.getColumnIndexOrThrow(COL_CID)),
                                                   cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE)),
                                                   cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE)));
                        ci.setRng(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ACCURACY)));
                        ciList.add(ci);
                    }
                }
            } finally {
                cursor.close();
            }
            queryResultCache.put(args, ciList);
            if (DEBUG) Log.d(TAG,"Cell info found: "+args.toString());
            return ciList;
        }
        if (DEBUG) Log.d(TAG,"Cell info not found: "+args.toString());
        queryResultNegativeCache.put(args, true);
        return null;
    }
}
