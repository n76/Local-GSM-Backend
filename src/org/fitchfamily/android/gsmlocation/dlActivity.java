package org.fitchfamily.android.gsmlocation;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * MainActivity displays the screen's UI and starts a dlFragment which will
 * execute an asynchronous task and will retain itself when configuration
 * changes occur.
 */
public class dlActivity extends Activity implements dlFragment.TaskCallbacks {
    private static String TAG = appConstants.TAG_PREFIX+"dlActivity";
    private static boolean DEBUG = appConstants.DEBUG;

    private static final String KEY_CURRENT_PROGRESS = "current_progress";
    private static final String KEY_LOG_PROGRESS = "log_progress";
    private static final String TAG_TASK_FRAGMENT = "dl_task_fragment";

    private dlFragment mTaskFragment;


    private Button mButton;
    private ProgressBar mProgressBar;
    private TextView mTextView;
    private String logText;

    private String OpenCellId_API;
    private String MCCfilter;
    private boolean doOCI;
    private boolean doMLS;

    private boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.i(TAG, "onCreate(Bundle)");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download);

        OpenCellId_API  = getIntent().getExtras().getString("ociAPI");
        MCCfilter       = getIntent().getExtras().getString("mccFilter");
        doOCI           = getIntent().getExtras().getBoolean("doOCI");
        doMLS           = getIntent().getExtras().getBoolean("doMLS");
        if (DEBUG) {
            Log.d(TAG, "Use OpenCellID data = " + String.valueOf(doOCI));
            Log.d(TAG, "Use Mozilla data = " + String.valueOf(doMLS));
            Log.d(TAG, "OpenCellId API Key = " + OpenCellId_API);
            Log.d(TAG, "MCC filtering = " + MCCfilter);
        }

        // Initialize views.
        mProgressBar=(ProgressBar) findViewById(R.id.progress);

        mTextView = (TextView)findViewById(R.id.logText);
        logText = "";
        mTextView.setText(logText);

        mButton = (Button) findViewById(R.id.cancel_button);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (running) {
                    if (DEBUG) Log.i(TAG, "mButton.onClick: Stop running task");
                    mTaskFragment.cancel();
                } else {
                    if (DEBUG) Log.i(TAG, "mButton.onClick: Finish Activity");
                    finish();
                }
            }
        });
        mButton.setText(getString(R.string.cancel_string));

        // Restore saved state.
        if (savedInstanceState != null) {
            mProgressBar.setProgress(savedInstanceState.getInt(KEY_CURRENT_PROGRESS));
            mTextView.setText(savedInstanceState.getString(KEY_LOG_PROGRESS));
        }

        FragmentManager fm = getFragmentManager();
        mTaskFragment = (dlFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is being retained
        // over a configuration change.
        if (mTaskFragment == null) {
            mTaskFragment = new dlFragment();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
            running = true;
            mTaskFragment.start(doOCI, doMLS, OpenCellId_API, MCCfilter);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG) Log.i(TAG, "onSaveInstanceState(Bundle)");
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_PROGRESS, mProgressBar.getProgress());
        outState.putString(KEY_LOG_PROGRESS, mTextView.getText().toString());
    }

    /*********************************/
    /***** TASK CALLBACK METHODS *****/
    /*********************************/

    @Override
    public void onPreExecute() {
        if (DEBUG) Log.i(TAG, "onPreExecute()");
        mButton.setText(getString(R.string.cancel_string));
        running = true;
//        Toast.makeText(this, R.string.task_started_msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProgressUpdate(int percent, String logText) {
//         if (DEBUG) Log.i(TAG, "onProgressUpdate(" + percent + "%)");
        mProgressBar.setProgress(percent * mProgressBar.getMax() / 100);
        mTextView.setText(logText);
    }

    @Override
    public void onCancelled() {
        if (DEBUG) Log.i(TAG, "onCancelled()");
        mButton.setText(getString(R.string.okay_string));
        mProgressBar.setProgress(0);
        running = false;

//         mTextView.setText(getString(R.string.zero_percent));
//         Toast.makeText(this, R.string.task_cancelled_msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPostExecute() {
        if (DEBUG) Log.i(TAG, "onPostExecute()");
        mProgressBar.setProgress(mProgressBar.getMax());
        mButton.setText(getString(R.string.okay_string));
        running = false;

//         mPercent.setText(getString(R.string.one_hundred_percent));
//         Toast.makeText(this, R.string.task_complete_msg, Toast.LENGTH_SHORT).show();
    }

    /************************/
    /***** OPTIONS MENU *****/
    /************************/
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_trigger_config_change:
                // Simulate a configuration change. Only available on
                // Honeycomb and above.
                recreate();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
*/

    /************************/
    /***** LOGS & STUFF *****/
    /************************/

    @Override
    protected void onStart() {
        if (DEBUG) Log.i(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onResume() {
        if (DEBUG) Log.i(TAG, "onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (DEBUG) Log.i(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (DEBUG) Log.i(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }

}
