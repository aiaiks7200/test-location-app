package com.test.mocklocation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

public class MockLocationService extends Service {

    public static final String ACTION_SET_LOCATION = "com.test.mocklocation.SET_LOCATION";
    public static final String ACTION_START_ROUTE = "com.test.mocklocation.START_ROUTE";
    public static final String ACTION_STOP = "com.test.mocklocation.STOP";

    private static final String CHANNEL_ID = "mock_location_channel";
    private static final int NOTIFICATION_ID = 1001;

    private LocationManager locationManager;
    private Handler handler;

    private boolean isFixedMode = false;
    private double fixedLat, fixedLng;

    private boolean isRouteMode = false;
    private double startLat, startLng, endLat, endLng;
    private float routeSpeed = 5.0f;
    private double routeProgress = 0;

    private Runnable updateRunnable;

    public static volatile boolean isServiceRunning = false;
    public static volatile double mockLatitude = 0;
    public static volatile double mockLongitude = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.initializing)));

        String action = intent.getAction();
        if (ACTION_SET_LOCATION.equals(action)) {
            double lat = intent.getDoubleExtra("lat", 0);
            double lng = intent.getDoubleExtra("lng", 0);
            startFixedLocation(lat, lng);
        } else if (ACTION_START_ROUTE.equals(action)) {
            startLat = intent.getDoubleExtra("start_lat", 0);
            startLng = intent.getDoubleExtra("start_lng", 0);
            endLat = intent.getDoubleExtra("end_lat", 0);
            endLng = intent.getDoubleExtra("end_lng", 0);
            routeSpeed = intent.getFloatExtra("speed", 5.0f);
            startRouteSimulation();
        } else if (ACTION_STOP.equals(action)) {
            stopAll();
            stopSelf();
            return START_NOT_STICKY;
        }

        isServiceRunning = true;
        return START_STICKY;
    }

    private void startFixedLocation(double lat, double lng) {
        stopAll();
        fixedLat = lat;
        fixedLng = lng;
        mockLatitude = lat;
        mockLongitude = lng;
        isFixedMode = true;

        try {
            setupAllProviders();
        } catch (SecurityException e) {
            notifyError(getString(R.string.set_mock_location_app));
            return;
        }

        sendLocationToAllProviders(fixedLat, fixedLng, 0, 0, 50.0);

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFixedMode) return;
                try {
                    sendLocationToAllProviders(fixedLat, fixedLng, 0, 0, 50.0);
                } catch (SecurityException e) {
                    isFixedMode = false;
                    return;
                }
                updateNotification(String.format("Fixed: %.6f, %.6f", fixedLat, fixedLng));
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(updateRunnable, 1000);
    }

    private void startRouteSimulation() {
        stopAll();
        isRouteMode = true;
        routeProgress = 0;
        mockLatitude = startLat;
        mockLongitude = startLng;

        try {
            setupAllProviders();
        } catch (SecurityException e) {
            notifyError(getString(R.string.set_mock_location_app));
            return;
        }

        final float bearing = (float) Math.toDegrees(
                Math.atan2(endLng - startLng, endLat - startLat));

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRouteMode) return;

                double lat = startLat + (endLat - startLat) * routeProgress;
                double lng = startLng + (endLng - startLng) * routeProgress;

                mockLatitude = lat;
                mockLongitude = lng;

                try {
                    sendLocationToAllProviders(lat, lng, bearing, routeSpeed, 50.0);
                } catch (SecurityException e) {
                    isRouteMode = false;
                    return;
                }

                updateNotification(String.format("Route: %.6f, %.6f", lat, lng));

                routeProgress += 0.01;
                if (routeProgress >= 1.0) {
                    isRouteMode = false;
                    startFixedLocation(endLat, endLng);
                    return;
                }
                handler.postDelayed(this, 1000);
            }
        };

        sendLocationToAllProviders(startLat, startLng, bearing, routeSpeed, 50.0);
        handler.postDelayed(updateRunnable, 1000);
    }

    private void setupAllProviders() throws SecurityException {
        try { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER); } catch (Exception ignored) {}
        locationManager.addTestProvider(
            LocationManager.GPS_PROVIDER,
            false, false, false, false,
            true, true, true,
            android.location.Criteria.POWER_LOW,
            android.location.Criteria.ACCURACY_FINE
        );
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        try { locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) {}
        try {
            locationManager.addTestProvider(
                LocationManager.NETWORK_PROVIDER,
                true, true, false, false,
                true, true, true,
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            );
            locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        } catch (SecurityException e) {
            // Some devices don't allow mocking network provider
        }
    }

    private void sendLocationToAllProviders(double lat, double lng, float bearing, float speed, double altitude)
            throws SecurityException {
        long time = System.currentTimeMillis();
        long elapsed = SystemClock.elapsedRealtimeNanos();

        Location gpsLoc = new Location(LocationManager.GPS_PROVIDER);
        gpsLoc.setLatitude(lat);
        gpsLoc.setLongitude(lng);
        gpsLoc.setAltitude(altitude);
        gpsLoc.setBearing(bearing);
        gpsLoc.setSpeed(speed);
        gpsLoc.setAccuracy(1.0f);
        gpsLoc.setTime(time);
        gpsLoc.setElapsedRealtimeNanos(elapsed);
        try {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, gpsLoc);
        } catch (Exception ignored) {}

        try {
            Location netLoc = new Location(LocationManager.NETWORK_PROVIDER);
            netLoc.setLatitude(lat);
            netLoc.setLongitude(lng);
            netLoc.setAltitude(altitude);
            netLoc.setBearing(bearing);
            netLoc.setSpeed(speed);
            netLoc.setAccuracy(10.0f);
            netLoc.setTime(time);
            netLoc.setElapsedRealtimeNanos(elapsed);
            locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, netLoc);
        } catch (Exception ignored) {}
    }

    private void stopAll() {
        isFixedMode = false;
        isRouteMode = false;
        isServiceRunning = false;

        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }

        try { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER); } catch (Exception ignored) {}
        try { locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) {}
    }

    private void notifyError(final String msg) {
        updateNotification(getString(R.string.error_prefix) + msg);
        handler.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(MockLocationService.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, getString(R.string.mock_location_notification), NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(getString(R.string.mock_location_notification));
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private Notification createNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
                .setContentTitle(getString(R.string.mock_location_notification))
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, createNotification(text));
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        stopAll();
        super.onDestroy();
    }
}
