package org.fitchfamily.android.gsmlocation.util;

import android.location.Location;

public abstract class LocationUtil {
    private LocationUtil() {

    }

    public static boolean equals(Location l1, Location l2) {
        if (l1 == null && l2 == null) {
            return true;
        } else if (l1 == null || l2 == null) {
            return false;
        } else {
            return (l1.getLatitude() == l2.getLatitude()) &&
                    (l1.getLongitude() == l2.getLongitude()) &&
                    (l1.getAccuracy() == l2.getAccuracy());
        }
    }
}
