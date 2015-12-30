package org.fitchfamily.android.gsmlocation.database;

import android.location.Location;
import android.os.Bundle;

import org.microg.nlp.api.LocationHelper;

public class LocationCalculator {
    double lat, lng, rng;
    int samples;

    public LocationCalculator add(double lat, double lng, int samples, double rng) {
        if(samples < 1) {
            samples = 1;
        }

        this.lat += (lat * samples);
        this.lng += (lng * samples);
        this.rng = Math.max(this.rng, rng);
        this.samples += samples;

        return this;
    }

    public Location toLocation() {
        return LocationHelper.create("gsm", (float) lat / samples, (float) lng / samples, (float) rng, new Bundle());
    }

    @Override
    public String toString() {
        return lat / samples + ", " + lng / samples + ", " + rng;
    }
}
