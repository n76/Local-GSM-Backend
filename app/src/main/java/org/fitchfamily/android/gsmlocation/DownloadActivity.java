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

    private static final String TAG_TASK_FRAGMENT = "dl_task_fragment";
    private DownloadTaskFragment mTaskFragment;

    private Button mButton;
    private ProgressBar mProgressBar;
    private TextView mTextView;

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
                if (mTaskFragment.isTaskRunning()) {
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

        FragmentManager fm = getFragmentManager();
        mTaskFragment = (DownloadTaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is being retained
        // over a configuration change.
        if (mTaskFragment == null) {
            mTaskFragment = new DownloadTaskFragment();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
            mTaskFragment.start(this);
        }
    }

    /*********************************/
    /***** TASK CALLBACK METHODS *****/
    /*********************************/

    @Override
    public void onPreExecute() {
        if (DEBUG)
            Log.i(TAG, "onPreExecute()");
        mButton.setText(getString(android.R.string.cancel));
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
    }

    @Override
    public void onPostExecute() {
        if (DEBUG)
            Log.i(TAG, "onPostExecute()");
        mProgressBar.setProgress(mProgressBar.getMax());
        mButton.setText(getString(android.R.string.ok));
    }
}
