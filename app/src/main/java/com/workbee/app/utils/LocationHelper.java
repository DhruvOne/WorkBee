package com.workbee.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

/**
 * LocationHelper — wraps FusedLocationProviderClient for real GPS tracking.
 * Used across LoginActivity, HomeFragment, ProfileFragment, and UberMatchActivity.
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Shared real location — updated once after login
    private static double realLatitude  = 0.0;
    private static double realLongitude = 0.0;
    private static String realAddress   = "";

    private final Context context;
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    public interface LocationCallback2 {
        void onLocationReceived(double latitude, double longitude, String address);
        void onLocationFailed(String reason);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /** Check if location permissions are granted */
    public static boolean hasPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Request location permissions from an Activity */
    public static void requestPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /** Fetch the last known or current GPS location once */
    public void fetchCurrentLocation(LocationCallback2 callback) {
        if (!hasPermission(context)) {
            callback.onLocationFailed("Location permission not granted.");
            return;
        }

        try {
            // First try last known (fast)
            fusedClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    saveAndReturn(location, callback);
                } else {
                    // Request a fresh single update
                    requestFreshLocation(callback);
                }
            }).addOnFailureListener(e -> requestFreshLocation(callback));
        } catch (SecurityException e) {
            callback.onLocationFailed("Security exception: " + e.getMessage());
        }
    }

    private void requestFreshLocation(LocationCallback2 callback) {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMaxUpdates(1)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result != null && !result.getLocations().isEmpty()) {
                    Location loc = result.getLocations().get(0);
                    saveAndReturn(loc, callback);
                    fusedClient.removeLocationUpdates(locationCallback);
                } else {
                    callback.onLocationFailed("Could not get location.");
                }
            }
        };

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            callback.onLocationFailed("Permission denied: " + e.getMessage());
        }
    }

    private void saveAndReturn(Location location, LocationCallback2 callback) {
        realLatitude  = location.getLatitude();
        realLongitude = location.getLongitude();
        realAddress   = reverseGeocode(realLatitude, realLongitude);
        Log.d(TAG, "Real location: " + realLatitude + ", " + realLongitude + " → " + realAddress);
        callback.onLocationReceived(realLatitude, realLongitude, realAddress);
    }

    /** Convert lat/lng to human-readable address using Geocoder */
    public String reverseGeocode(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (addr.getSubLocality() != null)  sb.append(addr.getSubLocality()).append(", ");
                if (addr.getLocality() != null)     sb.append(addr.getLocality()).append(", ");
                if (addr.getAdminArea() != null)    sb.append(addr.getAdminArea());
                return sb.toString().trim().replaceAll(", $", "");
            }
        } catch (Exception e) {
            Log.w(TAG, "Geocoder failed: " + e.getMessage());
        }
        return "456 Blossom Lane, Tech City";
    }

    // ── Static accessors for the shared real location ──────────────────────

    public static double getRealLatitude()  { return realLatitude;  }
    public static double getRealLongitude() { return realLongitude; }
    public static String getRealAddress()   { return realAddress;   }
    public static boolean hasRealLocation() { return realLatitude != 0.0 && realLongitude != 0.0; }

    /** Stop continuous location updates if running */
    public void stopUpdates() {
        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }
}
