package org.fitchfamily.android.gsmlocation;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;
import com.octo.android.robospice.request.listener.RequestCancellationListener;
import com.octo.android.robospice.request.listener.RequestProgress;
import com.octo.android.robospice.request.listener.RequestProgressListener;

/**
 * Shows the download progress.
 * Before launching this use "DownloadSpiceRequest.executeWith(this);" (for Activities extending BaseActivity)
 */
public class DownloadProgressActivity extends BaseActivity implements PendingRequestListener<DownloadSpiceRequest.Result>, RequestCancellationListener, RequestProgressListener {
    private Button mButton;
    private ProgressBar mProgressBar;
    private TextView mTextView;
    private boolean running = false;

    private void findViews() {
        mProgressBar= (ProgressBar) findViewById(R.id.progress);
        mTextView = (TextView)findViewById(R.id.logText);
        mButton = (Button) findViewById(R.id.cancel_button);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        findViews();

        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (running) {
                    getSpiceManager().cancel(DownloadSpiceRequest.Result.class, DownloadSpiceRequest.CACHE_KEY);
                } else {
                    finish();
                }
            }
        });

        connectService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectService();
    }

    private void connectService() {
        mButton.setText(getString(android.R.string.cancel));
        running = true;
        getSpiceManager().addListenerIfPending(DownloadSpiceRequest.Result.class, DownloadSpiceRequest.CACHE_KEY, this);
    }

    @Override
    public void onRequestSuccess(DownloadSpiceRequest.Result result) {
        mProgressBar.setProgress(mProgressBar.getMax());
        onRequestExecuted();
    }

    @Override
    public void onRequestFailure(SpiceException ex) {
        onRequestExecuted();
    }

    @Override
    public void onRequestCancelled() {
        mProgressBar.setProgress(0);
        onRequestExecuted();
    }

    private void onRequestExecuted() {
        running = false;
        mButton.setText(getString(android.R.string.ok));
        running = false;
        updateTextView();
    }

    @Override
    public void onRequestProgressUpdate(RequestProgress progress) {
        mProgressBar.setProgress(((int) progress.getProgress()) * mProgressBar.getMax() / 100);
        updateTextView();
    }

     @Override
     public void onRequestNotFound() {
         onRequestExecuted();
     }

    private void updateTextView() {
        DownloadSpiceRequest last = DownloadSpiceRequest.lastInstance;
        mTextView.setText(last == null ? null : last.getLog());
    }
}
