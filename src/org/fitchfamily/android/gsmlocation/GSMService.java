package org.fitchfamily.android.gsmlocation;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.fitchfamily.android.gsmlocation.model.CellInfo;
import org.microg.nlp.api.LocationBackendService;
import org.microg.nlp.api.LocationHelper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class GSMService extends LocationBackendService {

    protected String TAG = "org.fitchfamily.android.gsmlocation";

    protected Lock lock = new ReentrantLock();
    protected Thread worker = null;
    protected CellbasedLocationProvider lp = null;

    private Location lastLocation = null;

    public void start() {
        if (worker != null && worker.isAlive()) return;

        Log.d(TAG, "Starting location backend");
        Handler handler = new Handler(Looper.getMainLooper());
        final Context ctx = getApplicationContext();
        handler.post(new Runnable() {
            public void run() {
                CellbasedLocationProvider.getInstance().init(ctx);
            }
        });
        try {
            lock.lock();
            if (worker != null && worker.isAlive()) worker.interrupt();
            worker = new Thread() {
                public void run() {
                    Log.d(TAG, "Starting reporter thread");
                    lp = CellbasedLocationProvider.getInstance();
                    double lastLng = 0d;
                    double lastLat = 0d;
                    try {
                        while (true) {
                            Thread.sleep(1000);

                            try {
                                CellInfo[] infos = lp.getAll();

                                if (infos.length == 0) {
                                    lastLocation = null;
                                    continue;
                                }

                                double lng = 0d;
                                double lat = 0d;
                                float acc = 0f;

                                // use accuracy (observed cell tower
                                // coverage range) as proxy for position
                                // weighting rather than simply average
                                // locations
                                int totalWeight = 0;

                                for(CellInfo c : infos) {
                                    float thisAcc = (float) c.getRng();

                                    if (thisAcc < 1f)
                                        thisAcc = 1f;
                                    int wgt = (int) (100000f / thisAcc);
                                    if (wgt < 1)
                                        wgt = 1;
                                    lng += (c.getLng() * wgt);
                                    lat += (c.getLat() * wgt);
                                    acc += (thisAcc * wgt);
                                    totalWeight += wgt;
//                                    Log.d(TAG, "loop (" + lat + "," + lng + "," + acc + "," + wgt+ ")/" + totalWeight);
                                }
                                lng /= totalWeight;
                                lat /= totalWeight;
                                acc /= totalWeight;
                                if (lng != lastLng || lat != lastLat) {
                                    Log.d(TAG, "new location (" + lat + "," + lng + "," + acc + ") Number of towers:" + infos.length);
                                    lastLng = lng;
                                    lastLat = lat;
                                    lastLocation = LocationHelper.create("gsm", lat, lng, acc);
                                    report(lastLocation);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Update loop failed", e);
                                lastLocation = null;
                            }
                        }
                    } catch (InterruptedException e) {}
                }
            };
            worker.start();
        } catch (Exception e) {
            Log.e(TAG, "Start failed", e);
        } finally {
            try { lock.unlock(); } catch (Exception e) {}
        }
    }

    @Override
    protected Location update() {
        start();
//        Log.e(TAG, "update: " + lastLocation.toString());
        return (lastLocation);
    }

    @Override
    protected void onOpen() {
        super.onOpen();

        start();

        Log.d(TAG, "Binder OPEN called");
    }

    protected void onClose() {
        Log.d(TAG, "Binder CLOSE called");
        super.onClose();
        try {
            lock.lock();
            if (worker != null && worker.isAlive()) worker.interrupt();
            if (worker != null) worker = null;
        } finally {
            try { lock.unlock(); } catch (Exception e) {}
        }
    }

}
