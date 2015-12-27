package org.fitchfamily.android.gsmlocation.ui.base;

import android.support.v4.app.Fragment;

import com.octo.android.robospice.SpiceManager;

import org.fitchfamily.android.gsmlocation.async.SpiceService;

public abstract class BaseFragment extends Fragment {
    private SpiceManager spiceManager = new SpiceManager(SpiceService.class);

    @Override
    public void onStart() {
        spiceManager.start(getContext());
        super.onStart();
    }

    @Override
    public void onStop() {
        spiceManager.shouldStop();
        super.onStop();
    }

    protected SpiceManager getSpiceManager() {
        return spiceManager;
    }
}
