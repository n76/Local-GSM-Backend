package org.fitchfamily.android.gsmlocation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import java.net.MalformedURLException;

import javax.net.ssl.HttpsURLConnection;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

/**
 * TaskFragment manages a single background task and retains itself across
 * configuration changes.
 */
public class dlFragment extends Fragment {
    private static String TAG = appConstants.TAG_PREFIX+"dlFragment";
    private static boolean DEBUG = appConstants.DEBUG;

    /**
     * Callback interface through which the fragment can report the task's
     * progress and results back to the Activity.
     */
    static interface TaskCallbacks {
        void onPreExecute();
        void onProgressUpdate(int percent, String logText);
        void onCancelled();
        void onPostExecute();
    }

    private TaskCallbacks mCallbacks;
    private downloadDataAsync mTask;
    private boolean mRunning;

    /**
     * Hold a reference to the parent Activity so we can report the task's current
     * progress and results. The Android framework will pass us a reference to the
     * newly created Activity after each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        if (DEBUG) Log.i(TAG, "onAttach(Activity)");
        super.onAttach(activity);
        if (!(activity instanceof TaskCallbacks)) {
            throw new IllegalStateException("Activity must implement the TaskCallbacks interface.");
        }

        // Hold a reference to the parent Activity so we can report back the task's
        // current progress and results.
        mCallbacks = (TaskCallbacks) activity;
    }

    /**
     * This method is called once when the Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.i(TAG, "onCreate(Bundle)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) throws IllegalStateException {
        if (DEBUG) Log.i(TAG, "onCreate(Bundle)");
        super.onActivityCreated(savedInstanceState);
    }
    /**
     * Note that this method is <em>not</em> called when the Fragment is being
     * retained across Activity instances. It will, however, be called when its
     * parent Activity is being destroyed for good (such as when the user clicks
     * the back button, etc.).
     */
    @Override
    public void onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy()");
        super.onDestroy();
        cancel();
    }

    /*****************************/
    /***** TASK FRAGMENT API *****/
    /*****************************/

    /**
     * Start the background task.
     */
    public void start(boolean doOCI,
                      boolean doMLS,
                      String OpenCellId_API,
                      String MCCfilter) {
        if (!mRunning) {
            mTask = new downloadDataAsync(doOCI,
                                          doMLS,
                                          OpenCellId_API,
                                          MCCfilter);
            mTask.execute();
            mRunning = true;
        }
    }

    /**
    * Cancel the background task.
    */
    public void cancel() {
        if (DEBUG) Log.i(TAG, "cancel() while running="+String.valueOf(mRunning));
        if (mRunning) {
            mTask.cancel(false);
            mTask = null;
            mRunning = false;
        }
    }

    /**
     * Returns the current state of the background task.
     */
    public boolean isRunning() {
        return mRunning;
    }

    /************************/
    /***** LOGS & STUFF *****/
    /************************/

    @Override
    public void onStart() {
        if (DEBUG) Log.i(TAG, "onStart()");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.i(TAG, "onResume()");
        super.onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG) Log.i(TAG, "onStop()");
        super.onStop();
    }

    /***************************/
    /***** BACKGROUND TASK *****/
    /***************************/

    class progressInfo {
        public int percent;
        public String log_line;

        progressInfo(int curPercent, String newLogLine ) {
            percent = curPercent;
            log_line = newLogLine;
        }
    }

    /**
     * A task that performs the actual downloading and building of a new
     * database it proxies progress updates and results back to the Activity.
     */
    private class downloadDataAsync extends AsyncTask<Context, progressInfo, Void> {

        private String OpenCellId_API;
        private String MCCfilter;
        private boolean doOCI;
        private boolean doMLS;

        private Boolean mccEnable[] = new Boolean[1000];

        private int percentComplete;

        private File newDbFile;
        private SQLiteDatabase database = null;

        private String logText;

        downloadDataAsync(boolean doOCI,
                          boolean doMLS,
                          String OpenCellId_API,
                          String MCCfilter) {

            percentComplete = 0;
            logText = "";

            this.OpenCellId_API = OpenCellId_API;
            this.MCCfilter = MCCfilter;
            this.doOCI = doOCI;
            this.doMLS = doMLS;
            if (DEBUG) {
                Log.d(TAG, "downloadDataAsync(" + String.valueOf(doOCI)
                        +  ", " + String.valueOf(doMLS)
                        +  ", \"" + OpenCellId_API
                        +  "\", \"" + MCCfilter
                        +  "\")");
            }

            // mcc filtering is a boolean array. Fill with false (don't use)
            // and then set the mcc codes we want to true.
            for (int i=0; i<1000; i++)
                mccEnable[i] = false;
            int enableCount = 0;
            if (!MCCfilter.equals("")) {
                doLog("MCC filter: " + MCCfilter );
                String[] mccCodes = MCCfilter.split(",");
                for (String c : mccCodes) {
                    if ((c != null) && (c.length() > 0)) {
                        mccEnable[Integer.parseInt(c)] = true;
                        enableCount++;
                    }
                }
            }
            // If no mcc codes were specified, then assume we want the
            // world, so set all codes to true.
            if (enableCount == 0) {
                for (int i=0; i<1000; i++)
                    mccEnable[i] = true;
                doLog("No MCC Filters, assume world");
            }
        }

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                // Proxy the call to the Activity.
                mCallbacks.onPreExecute();
            }
            mRunning = true;
        }

        @Override
        protected void onProgressUpdate(progressInfo... progress) {
            if (mCallbacks != null) {
            // Proxy the call to the Activity.
                progressInfo thisProgress = progress[0];
                mCallbacks.onProgressUpdate(thisProgress.percent, thisProgress.log_line);
            }
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks != null) {
                // Proxy the call to the Activity.
                mCallbacks.onCancelled();
            }
            mRunning = false;
        }

        @Override
        protected void onPostExecute(Void ignore) {
            if (mCallbacks != null) {
                // Proxy the call to the Activity.
                mCallbacks.onPostExecute();
            }
            mRunning = false;
        }

        /**
         * Note that we do NOT call the callback object's methods directly from the
         * background thread, as this could result in a race condition.
         */
        @Override
        protected Void doInBackground(Context... params) {
            try {
//                doLog(appConstants.DB_FILE.getAbsolutePath());

                // Create new database to put everything in.
                appConstants.DB_DIR.mkdirs();
                newDbFile = File.createTempFile("new_lacells", ".db", appConstants.DB_DIR);
                database = SQLiteDatabase.openDatabase(newDbFile.getAbsolutePath(),
                                                       null,
                                                       SQLiteDatabase.NO_LOCALIZED_COLLATORS +
                                                       SQLiteDatabase.OPEN_READWRITE +
                                                       SQLiteDatabase.CREATE_IF_NECESSARY
                                                       );
                database.execSQL("CREATE TABLE cells(mcc INTEGER, mnc INTEGER, lac INTEGER, cid INTEGER, longitude REAL, latitude REAL, accuracy REAL, samples INTEGER, altitude REAL);");
                database.execSQL("CREATE INDEX _idx1 ON cells (mcc, mnc, lac, cid);");
                database.execSQL("CREATE INDEX _idx2 ON cells (lac, cid);");

                if (doOCI) {
                    doLog("Getting Tower Data From Open Cell ID. . .");
//                    doLog("OpenCellId API Key = " + OpenCellId_API);
                    getData(appConstants.OCI_URL_PREFIX + OpenCellId_API + appConstants.OCI_URL_SUFFIX);
                }
                if (doMLS) {
                    doLog("Getting Tower Data From Mozilla Location Services. . .");
                    SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd");
                    dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                    getData(appConstants.MLS_URL_PREFIX + dateFormatGmt.format(new Date())+"" + appConstants.MLS_URL_SUFFIX);
                }

                database.execSQL("VACUUM;");

            } catch (Exception e) {
                doLog(e.getMessage());
            }

            if (database != null)
                database.close();

            File jFile = new File(newDbFile.getAbsolutePath() + "-journal");
            if (jFile.exists())
                jFile.delete();

           if (!isCancelled()) {         // successful completion
                if (appConstants.DB_BAK_FILE.exists())
                    appConstants.DB_BAK_FILE.delete();
                if (appConstants.DB_FILE.exists())
                    appConstants.DB_FILE.renameTo(appConstants.DB_BAK_FILE);
                newDbFile.renameTo(appConstants.DB_FILE);
            } else {
                if ((newDbFile != null) && newDbFile.exists()) {
                    newDbFile.delete();
                    newDbFile = null;
                }
            }

            doLog("Finished.");
            return null;
        }

        private void doLog(String s) {
            logText += s + "\n";

            publishProgress(new progressInfo(percentComplete, logText));
            if (DEBUG) Log.d(TAG, s);
        }

        private void getData(String mUrl) throws Exception {
            try {
                long maxLength = 0;
                int totalRecords = 0;
                int insertedRecords = 0;

                long entryTime = System.currentTimeMillis();

                doLog("URL is " + mUrl);

                HttpURLConnection c = null;
                URL u = new URL(mUrl);
                if (u.getProtocol().equals("https")) {
                    c = (HttpsURLConnection) u.openConnection();
                } else {
                    c = (HttpURLConnection) u.openConnection();
                }
                c.setRequestMethod("GET");
                c.connect();

                // Looks like .gz is about a 4 to 1 compression ratio
                doLog("Content length = "+c.getContentLength());
                maxLength = c.getContentLength()*4;

                csvParser cvs = new csvParser(
                                new BufferedInputStream(
                                new GZIPInputStream(
                                new BufferedInputStream(
                                c.getInputStream()))));

                // CSV Field    ==> Database Field
                // radio        ==>
                // mcc          ==> mcc
                // net          ==> mnc
                // area         ==> lac
                // cell         ==> cid
                // unit         ==>
                // lon          ==> longitude
                // lat          ==> latitude
                // range        ==> accuracy
                // samples      ==> samples
                // changeable   ==>
                // created      ==>
                // updated      ==>
                // averageSignal==>
                List headers = cvs.parseLine();
                int mccIndex = headers.indexOf("mcc");
                int mncIndex = headers.indexOf("net");
                int lacIndex = headers.indexOf("area");
                int cidIndex = headers.indexOf("cell");
                int lonIndex = headers.indexOf("lon");
                int latIndex = headers.indexOf("lat");
                int accIndex = headers.indexOf("range");
                int smpIndex = headers.indexOf("samples");

                String sql = "INSERT INTO cells (mcc, mnc, lac, cid, longitude, latitude, accuracy, samples, altitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, -1);";
                SQLiteStatement stmt = database.compileStatement(sql);
                database.beginTransaction();

                List<String> rec = null;
                while (((rec = cvs.parseLine()) != null) &&
                       (rec.size() > 8) &&
                       !isCancelled()) {
                    totalRecords++;

                    int percentComplete = (int)((100l * cvs.bytesRead()) / maxLength);
                    if ((totalRecords % 1000) == 0) {
                        String statusText = "Records Read: " + Integer.toString(totalRecords) +
                                            ", Inserted: " + Integer.toString(insertedRecords);
                        String l = logText + statusText;
                        publishProgress(new progressInfo(percentComplete, l));
                    }

                    int mcc = Integer.parseInt((String) rec.get(mccIndex));
                    if ((mcc >= 0) && (mcc <=999) && mccEnable[mcc]) {
                        // Keep transaction size limited
                        if ((insertedRecords % 1000) == 0) {
                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.beginTransaction();
                        }
                        stmt.bindString(1, (String) Integer.toString(mcc));
                        stmt.bindString(2, (String) rec.get(mncIndex));
                        stmt.bindString(3, (String) rec.get(lacIndex));
                        stmt.bindString(4, (String) rec.get(cidIndex));
                        stmt.bindString(5, (String) rec.get(lonIndex));
                        stmt.bindString(6, (String) rec.get(latIndex));
                        int range = Integer.parseInt((String) rec.get(accIndex));
                        if (range < appConstants.MIN_RANGE)
                            range = appConstants.MIN_RANGE;
                        if (range > appConstants.MAX_RANGE)
                            range = appConstants.MAX_RANGE;
                        stmt.bindString(7, (String) Integer.toString(range));
                        stmt.bindString(8, (String) rec.get(smpIndex));
                        long entryID = stmt.executeInsert();
                        stmt.clearBindings();
                        insertedRecords++;
                    }
                }
                database.setTransactionSuccessful();
                database.endTransaction();
                doLog("Records Read: " + Integer.toString(totalRecords) +
                      ", Inserted: " + Integer.toString(insertedRecords));

                long exitTime = System.currentTimeMillis();
                long execTime = exitTime-entryTime;
                if (totalRecords < 1)
                    totalRecords = 1;
                doLog("Total Time: " + execTime + "ms (" + (1.0*execTime)/totalRecords + "ms/record)");
            } catch (MalformedURLException e) {
                doLog("getData('" + mUrl + "') failed: " + e.getMessage());
                throw e;
            }
        }
    }
}
