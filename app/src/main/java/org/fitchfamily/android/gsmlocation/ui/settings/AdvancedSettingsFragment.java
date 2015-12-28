package org.fitchfamily.android.gsmlocation.ui.settings;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.github.machinarius.preferencefragment.PreferenceFragment;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;

import org.androidannotations.annotations.AfterPreferences;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.PreferenceScreen;
import org.fitchfamily.android.gsmlocation.R;
import org.fitchfamily.android.gsmlocation.async.DownloadSpiceRequest;
import org.fitchfamily.android.gsmlocation.async.SpiceService;
import org.fitchfamily.android.gsmlocation.services.opencellid.OpenCellId;

import java.util.HashSet;
import java.util.Set;

@EFragment
@PreferenceScreen(R.xml.settings_prefs)
public class AdvancedSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private SpiceManager spiceManager = new SpiceManager(SpiceService.class);

    private SharedPreferences sp;

    private Set<Preference> preferences = new HashSet<>();

    @Override
    public void onStart() {
        super.onStart();
        spiceManager.start(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        spiceManager.shouldStop();
    }

    @AfterPreferences
    protected void init() {
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        bindPreferenceSummaryToValue(findPreference("oci_key_preference"));
        bindPreferenceSummaryToValue(findPreference("mcc_filter_preference"));
        bindPreferenceSummaryToValue(findPreference("mnc_filter_preference"));

        // only enable settings when the database download is not running
        setPreferencesEnabled(false);
        spiceManager.addListenerIfPending(DownloadSpiceRequest.Result.class, DownloadSpiceRequest.CACHE_KEY, new PendingRequestListener<DownloadSpiceRequest.Result>() {
            @Override
            public void onRequestNotFound() {
                setPreferencesEnabled(true);
            }

            @Override
            public void onRequestFailure(SpiceException spiceException) {
                setPreferencesEnabled(true);
            }

            @Override
            public void onRequestSuccess(DownloadSpiceRequest.Result result) {
                setPreferencesEnabled(true);
            }
        });
    }

    private void setPreferencesEnabled(boolean enabled) {
        for (Preference preference : preferences) {
            preference.setEnabled(enabled);
        }
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
        onPreferenceChange(preference, sp.getString(preference.getKey(), ""));
        preferences.add(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String prefKey = preference.getKey();

        if (prefKey.equals("oci_key_preference")) {
            String newKey = newValue.toString();
            if (!newKey.isEmpty() && !OpenCellId.isApiKeyValid(newKey)) {
                new AlertDialog.Builder(getActivity()).setMessage(R.string.invalid_oci_api_key)
                        .setCancelable(false).setPositiveButton(android.R.string.ok, null).show();
                return false;
            } else {
                preference.setSummary(newValue.toString());
            }
        } else {
            preference.setSummary(newValue.toString());
        }

        return true;
    }
}
