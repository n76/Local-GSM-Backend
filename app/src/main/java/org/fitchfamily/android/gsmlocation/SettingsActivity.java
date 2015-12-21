package org.fitchfamily.android.gsmlocation;

import android.app.Activity;
import android.os.Bundle;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

public class SettingsActivity extends Activity {
    private static final String TAG = makeLogTag(SettingsActivity.class);
    private static final boolean DEBUG = Config.DEBUG;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
