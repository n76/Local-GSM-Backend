package org.fitchfamily.android.gsmlocation;

import android.content.Context;

import com.octo.android.robospice.UncachedSpiceService;
import com.octo.android.robospice.networkstate.NetworkStateChecker;

/**
 * Service which is used to execute operations async without an Activity
 */
public class SpiceService extends UncachedSpiceService {
    @Override
    protected NetworkStateChecker getNetworkStateChecker() {
        return new NetworkStateChecker() {
            @Override
            public boolean isNetworkAvailable(Context context) {
                return true;
            }

            @Override
            public void checkPermissions(Context context) {

            }
        };
    }

    @Override
    public int getThreadPriority() {
        return Thread.NORM_PRIORITY;
    }
}
