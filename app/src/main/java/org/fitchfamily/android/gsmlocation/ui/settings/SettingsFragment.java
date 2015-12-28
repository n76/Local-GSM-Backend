package org.fitchfamily.android.gsmlocation.ui.settings;

import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.CheckedChange;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.ViewById;
import org.fitchfamily.android.gsmlocation.R;
import org.fitchfamily.android.gsmlocation.Settings;
import org.fitchfamily.android.gsmlocation.async.DownloadSpiceRequest;
import org.fitchfamily.android.gsmlocation.async.ObtainOpenCellIdKeySpiceRequest;
import org.fitchfamily.android.gsmlocation.data.MobileCountryCodes;
import org.fitchfamily.android.gsmlocation.services.opencellid.OpenCellIdLimitException;
import org.fitchfamily.android.gsmlocation.ui.base.BaseFragment;
import org.fitchfamily.android.gsmlocation.ui.settings.mcc.AreaListActivity_;
import org.fitchfamily.android.gsmlocation.util.LocaleUtil;

import java.util.Set;

@EFragment(R.layout.fragment_settings)
public class SettingsFragment extends BaseFragment {
    private static final int PAGE_BLOCKED = 0;

    private static final int PAGE_SETTINGS = 1;

    private static final int REQUEST_EDIT_AREAS = 1;

    @ViewById
    protected ViewSwitcher switcher;

    private final PendingRequestListener<DownloadSpiceRequest.Result> databaseListener = new PendingRequestListener<DownloadSpiceRequest.Result>() {
        @Override
        public void onRequestNotFound() {
            switcher.setDisplayedChild(PAGE_SETTINGS);
        }

        @Override
        public void onRequestFailure(SpiceException spiceException) {
            switcher.setDisplayedChild(PAGE_SETTINGS);
        }

        @Override
        public void onRequestSuccess(DownloadSpiceRequest.Result result) {
            switcher.setDisplayedChild(PAGE_SETTINGS);
        }
    };

    @ViewById
    protected CheckBox openCellId;

    @ViewById
    protected CheckBox mozillaLocationServices;

    @ViewById
    protected ProgressBar openCellIdProgress;

    private final PendingRequestListener<ObtainOpenCellIdKeySpiceRequest.Result> openCellIdListener = new PendingRequestListener<ObtainOpenCellIdKeySpiceRequest.Result>() {
        @Override
        public void onRequestFailure(SpiceException spiceException) {
            stopObtainingShowingOpenCellIdApiKey();

            OpenCellIdExceptionDialogFragment_.builder()
                    .reason(
                            spiceException.getCause() instanceof OpenCellIdLimitException ?
                                    OpenCellIdExceptionDialogFragment.Reason.day_limit :
                                    OpenCellIdExceptionDialogFragment.Reason.connection
                    )
                    .build()
                    .show(getFragmentManager());
        }

        @Override
        public void onRequestSuccess(ObtainOpenCellIdKeySpiceRequest.Result result) {
            stopObtainingShowingOpenCellIdApiKey();
            openCellId.setChecked(true);
        }

        @Override
        public void onRequestNotFound() {
            stopObtainingShowingOpenCellIdApiKey();
        }
    };

    @ViewById
    protected TextView areaList;

    @Override
    public void onStart() {
        super.onStart();
        switcher.setDisplayedChild(PAGE_BLOCKED);
        getSpiceManager().addListenerIfPending(DownloadSpiceRequest.Result.class, DownloadSpiceRequest.CACHE_KEY, databaseListener);
    }

    @AfterViews
    protected void init() {
        openCellId.setChecked(Settings.with(this).useOpenCellId());
        mozillaLocationServices.setChecked(Settings.with(this).useMozillaLocationService());
    }

    @AfterViews
    protected void reconnect() {
        showObtainingOpenCellIdApiKey();
        getSpiceManager().addListenerIfPending(ObtainOpenCellIdKeySpiceRequest.Result.class, ObtainOpenCellIdKeySpiceRequest.CACHE_KEY, openCellIdListener);
    }

    @CheckedChange
    protected void openCellId(boolean checked) {
        if (checked && TextUtils.isEmpty(Settings.with(this).openCellIdApiKey())) {
            openCellId.setChecked(false);
            obtainOpenCellIdApiKey();
        } else {
            Settings.with(this).useOpenCellId(checked);
        }
    }

    @CheckedChange
    protected void mozillaLocationServices(boolean checked) {
        Settings.with(this).useMozillaLocationService(checked);
    }

    private void obtainOpenCellIdApiKey() {
        showObtainingOpenCellIdApiKey();
        getSpiceManager().execute(new ObtainOpenCellIdKeySpiceRequest(getContext()), ObtainOpenCellIdKeySpiceRequest.CACHE_KEY, DurationInMillis.ALWAYS_EXPIRED, openCellIdListener);
    }

    private void showObtainingOpenCellIdApiKey() {
        openCellId.setEnabled(false);
        openCellIdProgress.setVisibility(View.VISIBLE);
    }

    private void stopObtainingShowingOpenCellIdApiKey() {
        openCellId.setEnabled(true);
        openCellIdProgress.setVisibility(View.GONE);
    }

    @Click
    protected void editAreas() {
        AreaListActivity_.intent(this).startForResult(REQUEST_EDIT_AREAS);
    }

    @AfterViews
    @OnActivityResult(REQUEST_EDIT_AREAS)
    protected void updateAreaView() {
        final Set<Integer> set = Settings.with(this).mccFilterSet();

        if (set.isEmpty()) {
            areaList.setText(R.string.fragment_settings_card_areas_nothing_chosen);
        } else {
            final StringBuilder builder = new StringBuilder();
            final MobileCountryCodes.Regions regions = MobileCountryCodes.with(getContext()).getAreas(set);

            for (String area : LocaleUtil.getCountryNames(regions.areas())) {
                builder.append(area).append('\n');
            }

            if (regions.containsUnresolved()) {
                builder.append(getString(R.string.fragment_settings_card_areas_other)).append('\n');
            }

            String text = builder.toString();

            if (text.endsWith("\n")) {
                text = text.substring(0, text.length() - 1);
            }

            areaList.setText(text);
        }
    }
}
