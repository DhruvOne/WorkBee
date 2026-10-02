package com.workbee.app.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.workbee.app.R;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;

/**
 * Foreground Service that pushes the worker's GPS coordinates to Firebase (or Mock store)
 * every ~4 seconds while the job is ACTIVE.
 *
 * Start it with:
 *   Intent i = new Intent(context, LocationTrackingService.class);
 *   i.putExtra("booking_id", bookingId);
 *   i.putExtra("customer_lat", customerLat);
 *   i.putExtra("customer_lng", customerLng);
 *   context.startForegroundService(i);  // or startService() on pre-O
 *
 * Stop it with:
 *   Intent stop = new Intent(context, LocationTrackingService.class);
 *   stop.setAction(LocationTrackingService.ACTION_STOP_TRACKING);
 *   context.startService(stop);
 */
public class LocationTrackingService extends Service {

    public static final String ACTION_STOP_TRACKING = "com.workbee.app.STOP_TRACKING";
    private static final String CHANNEL_ID  = "wb_location_tracking";
    private static final int    NOTIF_ID    = 9001;
    private static final String TAG         = "LocationTrackingService";

    // Extras
    public static final String EXTRA_BOOKING_ID   = "booking_id";
    public static final String EXTRA_CUSTOMER_LAT = "customer_lat";
    public static final String EXTRA_CUSTOMER_LNG = "customer_lng";

    // GPS update interval (ms)
    private static final long LOCATION_INTERVAL_MS    = 4_000L;
    private static final long LOCATION_FASTEST_MS     = 2_000L;

    // Mock mode simulation: worker starts ~1.2 km away and moves toward customer over ~8 min
    private static final long   MOCK_TOTAL_DURATION_MS = 8 * 60 * 1000L;
    private static final long   MOCK_UPDATE_INTERVAL   = 4_000L;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private FirebaseHelper firebaseHelper;

    private String bookingId;
    private double customerLat;
    private double customerLng;

    // Mock simulation state
    private boolean usingMockSimulation = false;
    private double mockStartLat;
    private double mockStartLng;
    private long   mockSimStartTime;
    private final Handler mockHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        fusedClient       = LocationServices.getFusedLocationProviderClient(this);
        firebaseHelper    = FirebaseHelper.getInstance(getApplicationContext());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        if (ACTION_STOP_TRACKING.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        bookingId   = intent.getStringExtra(EXTRA_BOOKING_ID);
        customerLat = intent.getDoubleExtra(EXTRA_CUSTOMER_LAT, 0.0);
        customerLng = intent.getDoubleExtra(EXTRA_CUSTOMER_LNG, 0.0);

        if (bookingId == null) {
            Log.e(TAG, "No booking_id provided — stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Show foreground notification (REQUIRED for background location on Android 8+)
        startForeground(NOTIF_ID, buildNotification("📍 WorkBee — Tracking your location..."));

        // Update tracking status to ACTIVE
        firebaseHelper.updateBookingTrackingStatus(bookingId, "ACTIVE", new FirebaseHelper.SimpleCallback() {
            @Override public void onSuccess() { Log.d(TAG, "Tracking status set to ACTIVE"); }
            @Override public void onFailure(String msg) { Log.w(TAG, "Failed to set ACTIVE: " + msg); }
        });

        startLocationUpdates();

        return START_STICKY; // Restart if killed by OS
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed — stopping GPS updates.");
        stopLocationUpdates();
        mockHandler.removeCallbacksAndMessages(null);

        // Mark tracking as COMPLETED when service stops
        if (bookingId != null) {
            firebaseHelper.updateBookingTrackingStatus(bookingId, "COMPLETED", new FirebaseHelper.SimpleCallback() {
                @Override public void onSuccess() { Log.d(TAG, "Tracking status set to COMPLETED"); }
                @Override public void onFailure(String msg) { Log.w(TAG, "Failed to set COMPLETED: " + msg); }
            });
        }

        super.onDestroy();
    }

    // ─────────────────────────────────────────────────────────────────
    // Location Updates
    // ─────────────────────────────────────────────────────────────────

    private void startLocationUpdates() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location loc = locationResult.getLastLocation();
                if (loc != null) {
                    pushLocation(loc.getLatitude(), loc.getLongitude());
                }
            }
        };

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
                .setMinUpdateIntervalMillis(LOCATION_FASTEST_MS)
                .build();

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "FusedLocation failed: " + e.getMessage() + " — Switching to mock simulation.");
                        startMockSimulation();
                    });
        } catch (SecurityException e) {
            Log.w(TAG, "Location permission denied — Switching to mock simulation.");
            startMockSimulation();
        }
    }

    private void stopLocationUpdates() {
        if (fusedClient != null && locationCallback != null) {
            try {
                fusedClient.removeLocationUpdates(locationCallback);
            } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Mock Simulation (moves from offset start position toward customer)
    // ─────────────────────────────────────────────────────────────────

    private void startMockSimulation() {
        if (customerLat == 0 && customerLng == 0) {
            // Fallback defaults (San Francisco) — only when no customer coordinates provided
            customerLat = 37.7749;
            customerLng = -122.4194;
        }
        // Start the worker ~1.2 km northeast of the customer
        mockStartLat  = customerLat + 0.011;
        mockStartLng  = customerLng + 0.011;
        mockSimStartTime = System.currentTimeMillis();
        usingMockSimulation = true;

        scheduleMockUpdate();
    }

    private void scheduleMockUpdate() {
        mockHandler.postDelayed(this::doMockUpdate, MOCK_UPDATE_INTERVAL);
    }

    private void doMockUpdate() {
        if (!usingMockSimulation || bookingId == null) return;

        long elapsed  = System.currentTimeMillis() - mockSimStartTime;
        float fraction = Math.min(1.0f, (float) elapsed / MOCK_TOTAL_DURATION_MS);

        // Generate the zig-zag street-path route snapping to city blocks
        LatLng startLatLng = new LatLng(mockStartLat, mockStartLng);
        LatLng endLatLng = new LatLng(customerLat, customerLng);
        List<LatLng> route = RouteHelper.generateMockRoute(startLatLng, endLatLng);

        // Get realistic snapped road position at this time fraction
        LatLng snappedPos = RouteHelper.getPositionAtFraction(route, fraction);

        pushLocation(snappedPos.latitude, snappedPos.longitude);

        if (fraction < 1.0f) {
            scheduleMockUpdate();
        } else {
            Log.d(TAG, "Mock simulation: worker arrived at customer location.");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Push to Firebase / Mock Store
    // ─────────────────────────────────────────────────────────────────

    private void pushLocation(double lat, double lng) {
        Log.d(TAG, "Pushing location: lat=" + lat + " lng=" + lng + " for booking=" + bookingId);
        firebaseHelper.updateWorkerLocation(bookingId, lat, lng, new FirebaseHelper.SimpleCallback() {
            @Override public void onSuccess() { /* silent */ }
            @Override public void onFailure(String msg) {
                Log.w(TAG, "Location push failed: " + msg);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // Notification Helpers
    // ─────────────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WorkBee Location Tracking",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Live GPS tracking for active service jobs");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_workbee_logo)
                .setContentTitle("WorkBee — Live Tracking Active")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)  // Cannot be dismissed by user
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // Static helper to start / stop the service from any Activity
    // ─────────────────────────────────────────────────────────────────

    public static void startTracking(Context context, String bookingId, double customerLat, double customerLng) {
        Intent intent = new Intent(context, LocationTrackingService.class);
        intent.putExtra(EXTRA_BOOKING_ID, bookingId);
        intent.putExtra(EXTRA_CUSTOMER_LAT, customerLat);
        intent.putExtra(EXTRA_CUSTOMER_LNG, customerLng);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopTracking(Context context) {
        Intent intent = new Intent(context, LocationTrackingService.class);
        intent.setAction(ACTION_STOP_TRACKING);
        context.startService(intent);
    }
}
