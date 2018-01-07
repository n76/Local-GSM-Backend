package org.fitchfamily.android.gsmlocation.ui.database;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;
import com.octo.android.robospice.request.listener.RequestCancellationListener;
import com.octo.android.robospice.request.listener.RequestProgress;
import com.octo.android.robospice.request.listener.RequestProgressListener;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.fitchfamily.android.gsmlocation.R;
import org.fitchfamily.android.gsmlocation.Settings;
import org.fitchfamily.android.gsmlocation.async.DownloadSpiceRequest;
import org.fitchfamily.android.gsmlocation.database.CellLocationDatabase;
import org.fitchfamily.android.gsmlocation.ui.base.BaseFragment;

@EFragment(R.layout.fragment_update_database)
public class UpdateDatabaseFragment extends BaseFragment implements
        PendingRequestListener<DownloadSpiceRequest.Result>, RequestCancellationListener, RequestProgressListener {

    private static final int PAGE_PROGRESS = 1;

    private static final int PAGE_INFO = 0;

    private static final int DETAILS_PAGE_BUTTON = 0;

    private static final int DETAILS_PAGE_LOG = 1;

    private static final int REQUEST_PERMISSION_STORAGE = 2;

    @ViewById
    protected TextView progressString;

    @ViewById
    protected ProgressBar progress;

    @ViewById
    protected ViewSwitcher switcher;

    @ViewById
    protected TextView lastUpdate;

    @ViewById
    protected TextView recordCount;

    @ViewById
    protected ViewSwitcher detailsSwitcher;

    @ViewById
    protected TextView log;

    @ViewById
    protected TextView errorSources;

    @ViewById
    protected TextView errorPermission;

    @ViewById
    protected Button settings;

    @ViewById
    protected Button permissionAllow;

    @ViewById
    protected Button update;

    @InstanceState
    protected boolean showLog;

    private Listener listener;

    @AfterViews
    protected void init() {
        setShowLog(showLog);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateLastDatabaseUpdate();
        showDatabaseSize();
        updateShownErrors();
        registerListener();
    }

    private void registerListener() {
        onDownloadStarted();
        getSpiceManager().addListenerIfPending(DownloadSpiceRequest.Result.class, DownloadSpiceRequest.CACHE_KEY, this);
    }

    @Click
    protected void update() {
        onDownloadStarted();
        getSpiceManager().execute(new DownloadSpiceRequest(getContext()), DownloadSpiceRequest.CACHE_KEY, DurationInMillis.ALWAYS_EXPIRED, this);
    }

    @Click
    protected void cancel() {
        getSpiceManager().cancel(DownloadSpiceRequest.Result.class, DownloadSpiceRequest.CACHE_KEY);
    }

    @Click
    protected void details() {
        setShowLog(true);
    }

    @Click
    protected void settings() {
        listener.openSettings();
    }

    @Click
    protected void permissionAllow() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_STORAGE);
    }

    private void onDownloadDone() {
        switcher.setDisplayedChild(PAGE_INFO);
        setShowLog(false);
    }

    private void onDownloadStarted() {
        switcher.setDisplayedChild(PAGE_PROGRESS);
        resetProgress();
    }

    void setProgress(int current, int max) {
        final boolean end = current == max;

        progress.setProgress(current);
        progress.setMax(max);
        progress.setIndeterminate(end);

        final float percent = ((float) current * 100.f) / ((float) max);
        progressString.setText(end ? null : (String.valueOf(Math.floor(percent * 10) / 10) + "%"));
    }

    private void resetProgress() {
        setProgress(100, 100);
    }

    private void updateShownErrors() {
        final boolean hasNoSources = !Settings.with(this).useMozillaLocationService() &&
                !Settings.with(this).useOpenCellId() &&
                !Settings.with(this).useLacells();

        final boolean hasNoPermission = ContextCompat.checkSelfPermission(
                getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED;
        final boolean hasProblem = hasNoSources | hasNoPermission;

        errorSources.setVisibility(hasNoSources ? View.VISIBLE : View.GONE);
        settings.setVisibility(hasNoSources ? View.VISIBLE : View.GONE);

        errorPermission.setVisibility(hasNoPermission ? View.VISIBLE : View.GONE);
        permissionAllow.setVisibility(hasNoPermission ? View.VISIBLE : View.GONE);

        update.setEnabled(!hasProblem);
    }

    @Background
    protected void updateLastDatabaseUpdate() {
        if (isAdded()) {
            long time = Settings.with(this).databaseLastModified();

            if (time != 0) {
                setLastUpdateString(getString(
                        R.string.fragment_update_database_last_update,
                        DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS)
                ), true);
            } else if (Settings.with(this).databaseFile() != null) {
                // exists but not readable
                setLastUpdateString(getString(
                        R.string.fragment_update_database_no_permission, Settings.with(this).databaseFile()
                ), true);
            } else {
                // not found
                setLastUpdateString(getString(R.string.fragment_update_database_no_database_found), false);
            }
        }
    }

    @UiThread
    void setLastUpdateString(String string, boolean is_update) {
        if (isAdded()) {
            lastUpdate.setText(string);
            update.setText(is_update ? R.string.fragment_update_database_start_update : R.string.fragment_update_database_create_database);
        }
    }

    @Background
    protected void showDatabaseSize() {
        CellLocationDatabase db = new CellLocationDatabase(getContext());
        db.checkForNewDatabase();
        setDatabaseSizeString(db.getDatabaseSize());
    }

    @UiThread
    protected void setDatabaseSizeString(long size) {
        if (isAdded()) {
            recordCount.setText(getString(R.string.fragment_size_database, size));
        }
    }

    private String getLog() {
        DownloadSpiceRequest last = DownloadSpiceRequest.lastInstance;
        return last == null ? null : last.getLog();
    }

    private void updateLogView() {
        log.setText(getLog());
    }

    private void setShowLog(boolean showLog) {
        this.showLog = showLog;
        detailsSwitcher.setDisplayedChild(showLog ? DETAILS_PAGE_LOG : DETAILS_PAGE_BUTTON);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (Listener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    //
    // LISTENERS
    //

    @Override
    public void onRequestNotFound() {
        onDownloadDone();
    }

    @Override
    public void onRequestCancelled() {
        onDownloadDone();
    }

    @Override
    public void onRequestFailure(SpiceException ex) {
        onDownloadDone();
        DatabaseUpdateExceptionDialogFragment_.builder()
                .log(getLog())
                .build()
                .show(getFragmentManager());
    }

    @Override
    public void onRequestSuccess(DownloadSpiceRequest.Result result) {
        if (isAdded()) {
            onDownloadDone();
            updateLastDatabaseUpdate();
            showDatabaseSize();
        }
    }

    @Override
    public void onRequestProgressUpdate(RequestProgress progress) {
        updateLogView();
        setProgress((int) progress.getProgress(), DownloadSpiceRequest.PROGRESS_MAX);
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (isAdded() && (requestCode == REQUEST_PERMISSION_STORAGE)) {
            updateShownErrors();
            updateLastDatabaseUpdate();
            showDatabaseSize();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public interface Listener {
        void openSettings();
    }
}
