package org.fitchfamily.android.gsmlocation;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

/**
 * MainActivity displays the screen's UI and starts DownloadTaskFragment which will
 * execute an asynchronous task and will retain itself when configuration
 * changes occur.
 */
public class DownloadActivity extends Activity implements DownloadTaskFragment.TaskCallbacks {
    private static final String TAG = makeLogTag(DownloadActivity.class);
    private static final boolean DEBUG = Config.DEBUG;

    private static final String KEY_CURRENT_PROGRESS = "current_progress";
    private static final String KEY_LOG_PROGRESS = "log_progress";
    private static final String TAG_TASK_FRAGMENT = "dl_task_fragment";

    private DownloadTaskFragment mTaskFragment;


    private Button mButton;
    private ProgressBar mProgressBar;
    private TextView mTextView;

    private boolean mRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.i(TAG, "onCreate(Bundle)");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        // Initialize views.
        mProgressBar=(ProgressBar) findViewById(R.id.progress);

        mTextView = (TextView)findViewById(R.id.logText);

        mButton = (Button) findViewById(R.id.cancel_button);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mRunning) {
                    if (DEBUG)
                        Log.i(TAG, "mButton.onClick: Stop running task");
                    mTaskFragment.cancel();
                } else {
                    if (DEBUG)
                        Log.i(TAG, "mButton.onClick: Finish Activity");
                    finish();
                }
            }
        });
        mButton.setText(getString(android.R.string.cancel));

        // Restore saved state.
        if (savedInstanceState != null) {
            mProgressBar.setProgress(savedInstanceState.getInt(KEY_CURRENT_PROGRESS));
            mTextView.setText(savedInstanceState.getString(KEY_LOG_PROGRESS));
        }

        FragmentManager fm = getFragmentManager();
        mTaskFragment = (DownloadTaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is being retained
        // over a configuration change.
        if (mTaskFragment == null) {
            mTaskFragment = new DownloadTaskFragment();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
            mRunning = true;
            mTaskFragment.start(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG)
            Log.i(TAG, "onSaveInstanceState(Bundle)");
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_PROGRESS, mProgressBar.getProgress());
        outState.putString(KEY_LOG_PROGRESS, mTextView.getText().toString());
    }

    /*********************************/
    /***** TASK CALLBACK METHODS *****/
    /*********************************/

    @Override
    public void onPreExecute() {
        if (DEBUG)
            Log.i(TAG, "onPreExecute()");
        mButton.setText(getString(android.R.string.cancel));
        mRunning = true;
    }

    @Override
    public void onProgressUpdate(int percent, String logText) {
        mProgressBar.setProgress(percent * mProgressBar.getMax() / 100);
        mTextView.setText(logText);
    }

    @Override
    public void onCancelled() {
        if (DEBUG)
            Log.i(TAG, "onCancelled()");
        mButton.setText(getString(android.R.string.ok));
        mProgressBar.setProgress(0);
        mRunning = false;
    }

    @Override
    public void onPostExecute() {
        if (DEBUG)
            Log.i(TAG, "onPostExecute()");
        mProgressBar.setProgress(mProgressBar.getMax());
        mButton.setText(getString(android.R.string.ok));
        mRunning = false;
    }
}
