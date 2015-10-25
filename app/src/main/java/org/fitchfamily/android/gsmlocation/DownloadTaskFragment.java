package org.fitchfamily.android.gsmlocation;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

/*
 * TaskFragment manages a single background task and retains itself across
 * configuration changes.
 *
 * Background tasks gathers data from OpenCellId and/or Mozilla Location
 * services and produces a new database file in the name specified in the
 * Config class. We don't actually touch the file being used by
 * the actual tower lookup.
 *
 * If/when the tower lookup is called, it will check for the existence of
 * the new file and if so, close the file it is using, purge its caches,
 * and then move the old file to backup and the new file to active.
 */
public class DownloadTaskFragment extends Fragment {
    private static final String TAG = makeLogTag(DownloadTaskFragment.class);
    private static final boolean DEBUG = Config.DEBUG;

    /**
     * Callback interface through which the fragment can report the task's
     * progress and results back to the Activity.
     */
    interface TaskCallbacks {
        void onPreExecute();

        void onProgressUpdate(int percent, String logText);

        void onCancelled();

        void onPostExecute();
    }

    private TaskCallbacks mCallbacks;
    private DownloadDataTask mTask = null;

    /**
     * Hold a reference to the parent Activity so we can report the task's current
     * progress and results. The Android framework will pass us a reference to the
     * newly created Activity after each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        if (DEBUG)
            Log.i(TAG, "onAttach(Activity)");

        super.onAttach(activity);

        // Hold a reference to the parent Activity so we can report back the task's
        // current progress and results.
        mCallbacks = (TaskCallbacks) activity;
    }

    /**
     * This method is called once when the Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG)
            Log.i(TAG, "onCreate(Bundle)");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) throws IllegalStateException {
        if (DEBUG)
            Log.i(TAG, "onCreate(Bundle)");
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
        if (DEBUG)
            Log.i(TAG, "onDestroy()");
        super.onDestroy();
        cancel();
    }

    /*****************************/
    /***** TASK FRAGMENT API *****/
    /*****************************/
    /**
     * Starts background task.
     *
     * @param doOCI
     * @param doMLS
     * @param OpenCellId_API
     * @param MCCfilter
     * @param MNCfilter
     */
    public void start(boolean doOCI, boolean doMLS, String OpenCellId_API, String MCCfilter, String MNCfilter, Context myCtx) {
        if (mTask == null) {
            mTask = new DownloadDataTask();
            mTask.initialize(doOCI, doMLS, OpenCellId_API, MCCfilter, MNCfilter, myCtx);
            mTask.execute();
        }
    }

    /**
     * Cancels the background task.
     */
    public void cancel() {
        if (DEBUG) Log.i(TAG, "cancel()");
        mTask.setState(mTask.CANCELED);
    }

    /************************/
    /***** LOGS & STUFF *****/
    /**
     * ********************
     */

    @Override
    public void onStart() {
        if (DEBUG)
            Log.i(TAG, "onStart()");
        super.onStart();
    }

    @Override
    public void onResume() {
        if (DEBUG)
            Log.i(TAG, "onResume()");
        super.onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG)
            Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    public void onStop() {
        if (DEBUG)
            Log.i(TAG, "onStop()");
        super.onStop();
    }

    /***************************/
    /***** BACKGROUND TASK *****/
    /**
     * ***********************
     */

    class ProgressInfo {
        public int percent;
        public String log_line;

        ProgressInfo(int curPercent, String newLogLine) {
            percent = curPercent;
            log_line = newLogLine;
        }
    }

    /**
     * A task that performs the actual downloading and building of a new
     * database it proxies progress updates and results back to the Activity.
     */
    private class DownloadDataTask extends AsyncTask<Context, ProgressInfo, Void> {
        private String OpenCellId_API;
        private String MCCfilter;
        private String MNCfilter;
        private boolean doOCI;
        private boolean doMLS;

        private Boolean mccEnable[] = new Boolean[1000];
        private Boolean mncEnable[] = new Boolean[1000];

        private int percentComplete;

        private File newDbFile;
        private SQLiteDatabase database = null;

        private String logText;

        public static final int RUNNING = 0;
        public static final int CANCELED = 1;
        public static final int FAILED = 2;
        public static final int SUCCESS = 3;
        private int mState = RUNNING;
        private Context ctx;


        void initialize(boolean doOCI, boolean doMLS, String OpenCellId_API, String MCCfilter, String MNCfilter, Context my_ctx) {

            // Clear logging when starting new download
            ctx = my_ctx;
            percentComplete = 0;
            logText = "";
            if (Config.GEN_LOG_FILE.exists())
                Config.GEN_LOG_FILE.delete();

            this.OpenCellId_API = OpenCellId_API;
            this.MCCfilter = MCCfilter;
            this.MNCfilter = MNCfilter;
            this.doOCI = doOCI;
            this.doMLS = doMLS;
            setState(RUNNING);
            if (DEBUG) {
                Log.d(TAG, "DownloadDataTask:initialize(" + String.valueOf(doOCI)
                        + ", " + String.valueOf(doMLS)
                        + ", \"" + OpenCellId_API
                        + "\", \"" + MCCfilter
                        + "\", \"" + MNCfilter
                        + "\")");
            }

            // mcc filtering is a boolean array. Fill with false (don't use)
            // and then set the mcc codes we want to true.
            for (int i = 0; i < 1000; i++)
                mccEnable[i] = false;

            int enableCount = 0;

            if (!MCCfilter.equals("")) {
                doLog(ctx.getString(R.string.log_MCC_FILTER) + " " + MCCfilter);
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
                for (int i = 0; i < 1000; i++)
                    mccEnable[i] = true;
                doLog(ctx.getString(R.string.log_MCC_WORLD));
            }

            // mnc filtering is a boolean array. Fill with false (don't use)
            // and then set the mnc codes we want to true.
            for (int i = 0; i < 1000; i++)
                mncEnable[i] = false;

            enableCount = 0;
            if (!MNCfilter.equals("")) {
                doLog(ctx.getString(R.string.log_MCC_FILTER) + " " + MNCfilter);
                String[] mncCodes = MNCfilter.split(",");
                for (String c : mncCodes) {
                    if ((c != null) && (c.length() > 0)) {
                        mncEnable[Integer.parseInt(c)] = true;
                        enableCount++;
                    }
                }
            }
            // If no mnc codes were specified, then assume we want the
            // world, so set all codes to true.
            if (enableCount == 0) {
                for (int i = 0; i < 1000; i++)
                    mncEnable[i] = true;
                doLog(ctx.getString(R.string.log_MNC_WORLD));
            }
        }

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                // Proxy the call to the Activity.
                mCallbacks.onPreExecute();
            }
        }

        @Override
        protected void onProgressUpdate(ProgressInfo... progress) {
            if (mCallbacks != null) {
                // Proxy the call to the Activity.
                ProgressInfo thisProgress = progress[0];
                mCallbacks.onProgressUpdate(thisProgress.percent, thisProgress.log_line);
            }
        }

        @Override
        protected void onCancelled() {
            setState(CANCELED);
            if (mCallbacks != null) {
                // Proxy the call to the Activity.
                mCallbacks.onCancelled();
            }
        }

        @Override
        protected void onPostExecute(Void ignore) {
            if (mCallbacks != null) {
                // Proxy the call to the Activity.
                mCallbacks.onPostExecute();
            }
        }

        /**
         * Note that we do NOT call the callback object's methods directly from the
         * background thread, as this could result in a race condition.
         */
        @Override
        protected Void doInBackground(Context... params) {
            long entryTime = System.currentTimeMillis();
            try {

                // Create new database to put everything in.
                Config.DB_DIR.mkdirs();
                newDbFile = File.createTempFile("new_lacells", ".db", Config.DB_DIR);
                database = SQLiteDatabase.openDatabase(newDbFile.getAbsolutePath(),
                        null,
                        SQLiteDatabase.NO_LOCALIZED_COLLATORS +
                                SQLiteDatabase.OPEN_READWRITE +
                                SQLiteDatabase.CREATE_IF_NECESSARY
                );
                database.execSQL("CREATE TABLE cells(mcc INTEGER, mnc INTEGER, lac INTEGER, cid INTEGER, longitude REAL, latitude REAL, accuracy REAL, samples INTEGER, altitude REAL);");

                if (doOCI && (getState() == RUNNING)) {
                    doLog(ctx.getString(R.string.log_GETTING_OCID));
                    getData(Config.OCI_URL_PREFIX + OpenCellId_API + Config.OCI_URL_SUFFIX);
                }

                if (doMLS && (getState() == RUNNING)) {
                    doLog(ctx.getString(R.string.log_GETTING_MOZ));
                    SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd");
                    // Mozilla publishes new CSV files at a bit after the beginning of
                    // a new day in GMT time. Get the time for a place a couple hours
                    // west of Greenwich to allow time for the data to be posted.
                    dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT-03"));
                    getData(Config.MLS_URL_PREFIX + dateFormatGmt.format(new Date()) + "" + Config.MLS_URL_SUFFIX);
                }

                if (getState() == RUNNING) {
                    doLog(ctx.getString(R.string.log_INDICIES));
                    database.execSQL("CREATE INDEX _idx1 ON cells (mcc, mnc, lac, cid);");
                    database.execSQL("CREATE INDEX _idx2 ON cells (lac, cid);");
                }
            } catch (IOException e) {
                setState(FAILED);
                doLog(e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                setState(FAILED);
                doLog(e.getMessage());
                e.printStackTrace();
            }

            // Close the database
            try {
                if (database != null)
                    database.close();
            } catch (Exception e) {
                doLog(e.getMessage());
            }

            // Remove database journal file regardless of how we finished
            try {
                File jFile = new File(newDbFile.getAbsolutePath() + "-journal");
                if (jFile.exists())
                    jFile.delete();
            } catch (Exception e) {
                doLog(e.getMessage());
            }

            // On successful completion, set result into new database file
            // On any failure, remove the result file.
            if (getState() == RUNNING) {         // successful completion
                if (Config.DB_NEW_FILE.exists())
                    Config.DB_NEW_FILE.delete();
                newDbFile.renameTo(Config.DB_NEW_FILE);
                setState(SUCCESS);
            } else {
                doLog(ctx.getString(R.string.log_CLEANING));
                if ((newDbFile != null) && newDbFile.exists()) {
                    newDbFile.delete();
                    newDbFile = null;
                }
            }
            long exitTime = System.currentTimeMillis();
            long execTime = exitTime - entryTime;
            doLog(ctx.getString(R.string.log_TOT_TIME) + " " +
                    execTime + ctx.getString(R.string.log_MILLISEC));

            doLog(ctx.getString(R.string.log_FINISHED));
            return null;
        }

        private void doLog(String s) {
            logText += s + "\n";

            publishProgress(new ProgressInfo(percentComplete, logText));
            if (DEBUG)
                Log.d(TAG, "DownloadDataTask: " + s);
            appendLog(s);
        }

        private void appendLog(String text) {
            if (!Config.GEN_LOG_FILE.exists()) {
                try {
                    Config.GEN_LOG_FILE.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            try {
                BufferedWriter buf = new BufferedWriter(new FileWriter(Config.GEN_LOG_FILE, true));
                buf.append(text);
                buf.newLine();
                buf.flush();
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void getData(String mUrl) throws Exception {
            try {
                long maxLength = 0;
                int totalRecords = 0;
                int insertedRecords = 0;

                long entryTime = System.currentTimeMillis();

                doLog(ctx.getString(R.string.log_URL) + " " + mUrl);

                HttpURLConnection c;
                URL u = new URL(mUrl);

                if (u.getProtocol().equals("https")) {
                    c = (HttpsURLConnection) u.openConnection();
                } else {
                    c = (HttpURLConnection) u.openConnection();
                }
                c.setRequestMethod("GET");
                c.connect();

                // Looks like .gz is about a 4 to 1 compression ratio
                doLog(ctx.getString(R.string.log_CONT_LENGTH) + " " + c.getContentLength());
                maxLength = c.getContentLength() * 4;

                CsvParser cvs = new CsvParser(
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
                String RecsReadStr = ctx.getString(R.string.log_REC_READ);
                String RecsInsertedStr = ctx.getString(R.string.log_REC_INSERTED);

                while (((rec = cvs.parseLine()) != null) &&
                        (rec.size() > 8) &&
                        (getState() == RUNNING)) {
                    totalRecords++;

                    int percentComplete = (int) ((100l * cvs.bytesRead()) / maxLength);
                    if ((totalRecords % 1000) == 0) {
                        String statusText = RecsReadStr + " " + Integer.toString(totalRecords) +
                                ", " + RecsInsertedStr + " " + Integer.toString(insertedRecords);
                        String l = logText + statusText;
                        publishProgress(new ProgressInfo(percentComplete, l));
                    }

                    int mcc = Integer.parseInt((String) rec.get(mccIndex));
                    int mnc = Integer.parseInt((String) rec.get(mncIndex));
                    if ((mcc >= 0) && (mcc <= 999) && mccEnable[mcc] &&
                            (mnc >= 0) && (mnc <= 999) && mncEnable[mnc]) {
                        // Keep transaction size limited
                        if ((insertedRecords % 1000) == 0) {
                            database.setTransactionSuccessful();
                            database.endTransaction();
                            database.beginTransaction();
                        }
                        stmt.bindString(1, Integer.toString(mcc));
                        stmt.bindString(2, rec.get(mncIndex));
                        stmt.bindString(3, rec.get(lacIndex));
                        stmt.bindString(4, rec.get(cidIndex));
                        stmt.bindString(5, rec.get(lonIndex));
                        stmt.bindString(6, rec.get(latIndex));
                        int range = Integer.parseInt(rec.get(accIndex));
                        if (range < Config.MIN_RANGE)
                            range = Config.MIN_RANGE;
                        if (range > Config.MAX_RANGE)
                            range = Config.MAX_RANGE;
                        stmt.bindString(7, Integer.toString(range));
                        stmt.bindString(8, rec.get(smpIndex));
                        long entryID = stmt.executeInsert();
                        stmt.clearBindings();
                        insertedRecords++;
                    }
                }
                if (getState() != RUNNING) {
                    doLog(ctx.getString(R.string.log_ABORTED) + stateString(getState()));
                }
                database.setTransactionSuccessful();
                database.endTransaction();
                doLog(RecsReadStr +
                        " " + Integer.toString(totalRecords) +
                        ", " + RecsInsertedStr +
                        " " + Integer.toString(insertedRecords));

                long exitTime = System.currentTimeMillis();
                long execTime = exitTime - entryTime;

                if (totalRecords < 1)
                    totalRecords = 1;

                float f = (Math.round((1000.0f * execTime) / totalRecords) / 1000.0f);
                doLog(ctx.getString(R.string.log_TOTAL_TIME) +
                        " " + execTime +
                        " " + ctx.getString(R.string.log_MILLISEC) +
                        " (" + f + ctx.getString(R.string.log_MILLISEC_PER_REC) + ")");
            } catch (MalformedURLException e) {
                doLog("getData('" + mUrl + "') failed: " + e.getMessage());
                setState(FAILED);
                throw e;
            } catch (Exception e) {
                setState(FAILED);
                doLog(e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }

        public synchronized void setState(int s) {
            mState = s;
            if (DEBUG)
                Log.d(TAG, "DownloadDataTask.setState(" + s + ")");
        }

        public synchronized int getState() {
            return mState;
        }

        public synchronized String stateString(int st) {
            if (st == RUNNING)
                return ctx.getString(R.string.st_RUNNING);
            if (st == CANCELED)
                return ctx.getString(R.string.st_CANCELED);
            if (st == FAILED)
                return ctx.getString(R.string.st_FAILED);
            if (st == SUCCESS)
                return ctx.getString(R.string.st_SUCCESS);
            return ctx.getString(R.string.st_UNKNOWN)+ "(" + st + ")";
        }
    }
}
