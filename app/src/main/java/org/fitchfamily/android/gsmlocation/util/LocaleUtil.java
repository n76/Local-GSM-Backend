package org.fitchfamily.android.gsmlocation.util;

import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.lang3.LocaleUtils;
import org.fitchfamily.android.gsmlocation.BuildConfig;
import org.fitchfamily.android.gsmlocation.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class LocaleUtil {
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String TAG = LogUtils.makeLogTag(LocaleUtil.class);

    private LocaleUtil() {

    }

    /**
     * Tries to get the name for the country
     *
     * @param code language code, for example en
     * @return the country name or the code
     */
    public static String getCountryName(@NonNull String code) {
        try {
            return LocaleUtils.toLocale("en_" + code.toUpperCase(Locale.ENGLISH)).getDisplayCountry();
        } catch (IllegalArgumentException ex) {
            if (DEBUG) {
                Log.d(TAG, "couldn't resolve " + code, ex);
            }

            return code;
        }
    }

    public static List<String> getCountryNames(Set<String> codes) {
        List<String> names = new ArrayList<>();

        for (String code : codes) {
            String resolved = getCountryName(code);

            if (!names.contains(resolved)) {
                names.add(resolved);
            }
        }

        Collections.sort(names);

        return Collections.unmodifiableList(names);
    }
}
