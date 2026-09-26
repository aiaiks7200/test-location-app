package com.test.mocklocation;

import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

public class LocationHelper {
    
    public static void setupProvider(LocationManager locationManager) {
        try {
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false,  // requiresNetwork
                false,  // requiresSatellite
                false,  // requiresCell
                false,  // hasMonetaryCost
                true,   // supportsAltitude
                true,   // supportsSpeed
                true,   // supportsBearing
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            );
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void sendMockLocation(LocationManager locationManager, double lat, double lng, 
                                         float bearing, float speed, double altitude) {
        try {
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(lat);
            location.setLongitude(lng);
            location.setAltitude(altitude);
            location.setBearing(bearing);
            location.setSpeed(speed);
            location.setAccuracy(1.0f);
            location.setTime(System.currentTimeMillis());
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void removeProvider(LocationManager locationManager) {
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
