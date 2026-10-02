package com.workbee.app.ui.provider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.workbee.app.R;
import com.workbee.app.data.model.Booking;
import com.workbee.app.data.model.User;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.ui.common.ConfettiView;
import com.workbee.app.utils.FirebaseHelper;
import com.workbee.app.utils.LocationTrackingService;
import com.workbee.app.utils.RouteHelper;
import android.widget.ImageButton;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

import java.util.Locale;

/**
 * Provider/Worker-side map activity.
 * Shows customer pin, worker's live location, route line, ETA, and status action buttons.
 * Starts LocationTrackingService (foreground background GPS) on launch.
 */
public class WorkerTrackingActivity extends BaseActivity implements OnMapReadyCallback {

    // Intent extras
    public static final String EXTRA_BOOKING_ID       = "booking_id";
    public static final String EXTRA_CUSTOMER_LAT     = "customer_lat";
    public static final String EXTRA_CUSTOMER_LNG     = "customer_lng";
    public static final String EXTRA_CUSTOMER_NAME    = "customer_name";
    public static final String EXTRA_CUSTOMER_ADDRESS = "customer_address";

    private static final long   SELF_LOCATION_POLL_MS = 3_500L;
    private static final double WALK_SPEED_KMH        = 30.0; // Avg vehicle speed for ETA
    private static final int    LOC_PERMISSION_REQUEST = 201;

    private MapView mapView;
    private GoogleMap googleMap;
    private Marker  workerMarker;
    private Marker  customerMarker;
    private Polyline routePolyline;

    private TextView txtJobStatus, txtCustomerName, txtCustomerAddress;
    private TextView txtEta, txtDistance, txtStatusBadge;
    private Button  btnOnTheWay, btnArrived, btnComplete;
    private View    gpsDot;
    private ImageButton btnCall;
    private View trafficBanner;
    private TextView txtTrafficDesc;
    private ConfettiView confettiView;
    private String customerProfileUrl = "";
    private LatLng lastWorkerPos = null;
    private List<Polyline> trafficPolylines = new java.util.ArrayList<>();

    private FirebaseHelper firebaseHelper;
    private String bookingId;
    private String customerName;
    private double customerLat;
    private double customerLng;

    // Worker's own last known position (from location service)
    private double workerLat = 0;
    private double workerLng = 0;

    // Current job status
    private String currentStatus = "ACCEPTED"; // ACCEPTED → ON_THE_WAY → IN_PROGRESS → COMPLETED

    private final Handler selfPollHandler = new Handler();
    private boolean isActivityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_tracking);

        firebaseHelper = FirebaseHelper.getInstance(this);

        // Read intent extras
        bookingId           = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        customerLat         = getIntent().getDoubleExtra(EXTRA_CUSTOMER_LAT, 0.0);
        customerLng         = getIntent().getDoubleExtra(EXTRA_CUSTOMER_LNG, 0.0);
        customerName        = getIntent().getStringExtra(EXTRA_CUSTOMER_NAME);
        String customerAddr = getIntent().getStringExtra(EXTRA_CUSTOMER_ADDRESS);

        // Bind views
        txtJobStatus        = findViewById(R.id.worker_txt_job_status);
        txtCustomerName     = findViewById(R.id.worker_txt_customer_name);
        txtCustomerAddress  = findViewById(R.id.worker_txt_customer_address);
        txtEta              = findViewById(R.id.worker_txt_eta);
        txtDistance         = findViewById(R.id.worker_txt_distance);
        txtStatusBadge      = findViewById(R.id.worker_txt_status_badge);
        btnOnTheWay         = findViewById(R.id.worker_btn_on_the_way);
        btnArrived          = findViewById(R.id.worker_btn_arrived);
        btnComplete         = findViewById(R.id.worker_btn_complete);
        gpsDot              = findViewById(R.id.worker_gps_dot);

        if (customerName != null) txtCustomerName.setText(customerName);
        if (customerAddr != null) txtCustomerAddress.setText(customerAddr);

        // Bind new views
        btnCall        = findViewById(R.id.worker_btn_call);
        trafficBanner  = findViewById(R.id.worker_traffic_banner);
        txtTrafficDesc = findViewById(R.id.worker_txt_traffic_desc);
        confettiView   = findViewById(R.id.worker_confetti_view);

        // Back button
        findViewById(R.id.worker_btn_back).setOnClickListener(v -> onBackPressed());

        // Call Button
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> showPremiumCallDialog());
        }

        View btnSos = findViewById(R.id.btn_sos);
        if (btnSos != null) {
            btnSos.setOnClickListener(v -> triggerEmergencySos());
        }

        // Button listeners
        btnOnTheWay.setOnClickListener(v -> handleOnTheWay());
        btnArrived.setOnClickListener(v -> handleArrived());
        btnComplete.setOnClickListener(v -> handleCompleteJob());

        // Load customer profile photo
        if (bookingId != null) {
            firebaseHelper.getWorkerLocation(bookingId, new FirebaseHelper.DataCallback<Booking>() {
                @Override
                public void onSuccess(Booking booking) {
                    firebaseHelper.getAllUsers(new FirebaseHelper.ListCallback<User>() {
                        @Override
                        public void onSuccess(List<User> list) {
                            for (User u : list) {
                                if (u.getUserId().equalsIgnoreCase(booking.getCustomerId())) {
                                    customerProfileUrl = u.getProfileImageUrl();
                                    break;
                                }
                            }
                        }
                        @Override public void onFailure(String msg) {}
                    });
                }
                @Override public void onFailure(String msg) {}
            });
        }

        // Initialize map
        mapView = findViewById(R.id.worker_map_view);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        // Request location permissions and start tracking service
        requestLocationPermissionsAndStartTracking();

        // Animate GPS dot
        pulseGpsDot();
    }

    // ─────────────────────────────────────────────────────────────────
    // Permission + Service
    // ─────────────────────────────────────────────────────────────────

    private void requestLocationPermissionsAndStartTracking() {
        boolean hasFine   = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasBackground = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            hasBackground = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        if (!hasFine) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOC_PERMISSION_REQUEST);
        } else if (!hasBackground && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    LOC_PERMISSION_REQUEST + 1);
        } else {
            startGpsService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Start service regardless — it will fall back to mock simulation if permission denied
        startGpsService();
    }

    private void startGpsService() {
        if (bookingId == null) return;
        LocationTrackingService.startTracking(this, bookingId, customerLat, customerLng);

        // Also start polling Firebase to read worker's own GPS updates (for map rendering)
        startSelfLocationPoll();
    }

    // ─────────────────────────────────────────────────────────────────
    // Poll booking to get worker's own location (for rendering on map)
    // ─────────────────────────────────────────────────────────────────

    private void startSelfLocationPoll() {
        selfPollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed || bookingId == null) return;

                firebaseHelper.getWorkerLocation(bookingId, new FirebaseHelper.DataCallback<Booking>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(Booking booking) {
                        if (booking.getWorkerLatitude() != 0 && booking.getWorkerLongitude() != 0) {
                            workerLat = booking.getWorkerLatitude();
                            workerLng = booking.getWorkerLongitude();
                            updateMapWorkerPin(new LatLng(workerLat, workerLng));
                            updateEtaAndDistance();
                        }
                    }

                    @Override
                    public void onFailure(String msg) { /* silent */ }
                });

                selfPollHandler.postDelayed(this, SELF_LOCATION_POLL_MS);
            }
        }, SELF_LOCATION_POLL_MS);
    }

    // ─────────────────────────────────────────────────────────────────
    // Map Setup
    // ─────────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Premium dark carbon map style
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.bg_map_dark_style));
            if (!success) android.util.Log.w("WorkerTracking", "Map style failed.");
        } catch (Exception e) {
            android.util.Log.w("WorkerTracking", "Map style error: " + e.getMessage());
        }

        LatLng customerLatLng = new LatLng(customerLat, customerLng);

        // Customer marker (home icon in amber)
        customerMarker = googleMap.addMarker(new MarkerOptions()
                .position(customerLatLng)
                .title("📍 Customer Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

        // Estimate worker start ~1.2 km away for initial render
        double initWorkerLat = (workerLat != 0) ? workerLat : customerLat + 0.011;
        double initWorkerLng = (workerLng != 0) ? workerLng : customerLng + 0.011;
        LatLng initWorkerLatLng = new LatLng(initWorkerLat, initWorkerLng);

        // Worker marker (rotating scooter)
        workerMarker = googleMap.addMarker(new MarkerOptions()
                .position(initWorkerLatLng)
                .title("🐝 You (WorkBee Pro)")
                .icon(getBitmapDescriptorFromVector(R.drawable.ic_worker_scooter))
                .anchor(0.5f, 0.5f)
                .flat(true));

        // Generate street blocks route
        List<LatLng> route = RouteHelper.generateMockRoute(initWorkerLatLng, customerLatLng);

        // Clear any old lines
        for (Polyline pl : trafficPolylines) { pl.remove(); }
        trafficPolylines.clear();

        // Draw multi-colored traffic flow line blocks
        if (route.size() >= 4) {
            for (int i = 0; i < route.size() - 1; i++) {
                String color = RouteHelper.getTrafficColorForSegment(i);
                Polyline pl = googleMap.addPolyline(new PolylineOptions()
                        .add(route.get(i), route.get(i + 1))
                        .color(Color.parseColor(color))
                        .width(10f)
                        .geodesic(true));
                trafficPolylines.add(pl);
            }
        } else {
            routePolyline = googleMap.addPolyline(new PolylineOptions()
                    .addAll(route)
                    .color(Color.parseColor("#FFC107"))
                    .width(10f)
                    .geodesic(true));
        }

        // Zoom to fit both points
        try {
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(initWorkerLatLng)
                    .include(customerLatLng)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140));
        } catch (Exception e) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customerLatLng, 13f));
        }
    }

    private void updateMapWorkerPin(LatLng newPos) {
        if (googleMap == null) return;

        float bearing = 0f;
        if (lastWorkerPos != null) {
            bearing = RouteHelper.calculateBearing(lastWorkerPos, newPos);
        }

        if (workerMarker != null) {
            workerMarker.setPosition(newPos);
            if (bearing != 0f) {
                workerMarker.setRotation(bearing);
            }
        } else {
            workerMarker = googleMap.addMarker(new MarkerOptions()
                    .position(newPos)
                    .title("🐝 You (WorkBee Pro)")
                    .icon(getBitmapDescriptorFromVector(R.drawable.ic_worker_scooter))
                    .anchor(0.5f, 0.5f)
                    .flat(true));
        }

        lastWorkerPos = newPos;

        LatLng customerLatLng = new LatLng(customerLat, customerLng);
        
        // Redraw polylines representing the remaining dynamic path
        List<LatLng> remainingRoute = RouteHelper.generateMockRoute(newPos, customerLatLng);
        
        // Clear any old lines
        for (Polyline pl : trafficPolylines) { pl.remove(); }
        trafficPolylines.clear();

        if (remainingRoute.size() >= 4) {
            for (int i = 0; i < remainingRoute.size() - 1; i++) {
                String color = RouteHelper.getTrafficColorForSegment(i);
                Polyline pl = googleMap.addPolyline(new PolylineOptions()
                        .add(remainingRoute.get(i), remainingRoute.get(i + 1))
                        .color(Color.parseColor(color))
                        .width(10f)
                        .geodesic(true));
                trafficPolylines.add(pl);
            }
        } else {
            if (routePolyline != null) {
                java.util.List<LatLng> pts = new java.util.ArrayList<>();
                pts.add(newPos);
                pts.add(customerLatLng);
                routePolyline.setPoints(pts);
            } else {
                routePolyline = googleMap.addPolyline(new PolylineOptions()
                        .add(newPos, customerLatLng)
                        .color(Color.parseColor("#FFC107"))
                        .width(10f)
                        .geodesic(true));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ETA & Distance
    // ─────────────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private void updateEtaAndDistance() {
        if (workerLat == 0 && workerLng == 0) return;

        double distKm  = haversineKm(workerLat, workerLng, customerLat, customerLng);
        int    etaMins = (int) Math.ceil(distKm / WALK_SPEED_KMH * 60);

        if (distKm < 0.1) {
            txtDistance.setText("< 100 m");
            txtEta.setText("Arrived");
        } else if (distKm < 1.0) {
            txtDistance.setText(String.format(Locale.getDefault(), "%.0f m", distKm * 1000));
            txtEta.setText(etaMins > 0 ? etaMins + " min" : "< 1 min");
        } else {
            txtDistance.setText(String.format(Locale.getDefault(), "%.1f km", distKm));
            txtEta.setText(etaMins + " min");
        }

        // Feature 4: Live Traffic updates on the provider side too!
        float fraction = 1.0f - (float) Math.min(1.0, distKm / 1.2);
        if (txtTrafficDesc != null) {
            txtTrafficDesc.setText(RouteHelper.getTrafficBannerText(fraction));
        }
    }

    /** Haversine formula — returns distance in km between two lat/lng points */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─────────────────────────────────────────────────────────────────
    // Status Action Buttons
    // ─────────────────────────────────────────────────────────────────

    private void handleOnTheWay() {
        if (bookingId == null) return;
        showProgress("Updating status...");
        firebaseHelper.updateBookingStatus(bookingId, "ON_THE_WAY", new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                hideProgress();
                currentStatus = "ON_THE_WAY";
                txtJobStatus.setText("🚗 On the Way — Customer is waiting");
                txtStatusBadge.setText("On the Way");
                txtStatusBadge.setTextColor(Color.parseColor("#FFC107"));
                btnOnTheWay.setVisibility(View.GONE);
                btnArrived.setVisibility(View.VISIBLE);
                showToast("Customer notified — You're on the way! 🚗");
            }
            @Override
            public void onFailure(String msg) {
                hideProgress();
                showToast("Failed: " + msg);
            }
        });
    }

    private void handleArrived() {
        if (bookingId == null) return;
        showProgress("Updating status...");
        firebaseHelper.updateBookingStatus(bookingId, "IN_PROGRESS", new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                hideProgress();
                currentStatus = "IN_PROGRESS";
                txtJobStatus.setText("🏠 Arrived — Job In Progress");
                txtStatusBadge.setText("Arrived");
                txtStatusBadge.setTextColor(Color.parseColor("#FF8C00"));
                btnArrived.setVisibility(View.GONE);
                btnComplete.setVisibility(View.VISIBLE);
                txtEta.setText("Here");
                showToast("Customer notified — You've arrived! 🏠");
            }
            @Override
            public void onFailure(String msg) {
                hideProgress();
                showToast("Failed: " + msg);
            }
        });
    }

    private void handleCompleteJob() {
        if (bookingId == null) return;
        showProgress("Completing job...");
        firebaseHelper.updateBookingStatus(bookingId, "COMPLETED", new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Stop location tracking service
                LocationTrackingService.stopTracking(WorkerTrackingActivity.this);

                hideProgress();
                showToast("Job completed! Great work 🐝✅");
                txtJobStatus.setText("✅ Job Completed");
                txtStatusBadge.setText("Completed");
                txtStatusBadge.setTextColor(Color.parseColor("#4CAF50"));
                btnComplete.setEnabled(false);
                btnComplete.setText("✅ Completed");

                // Feature 5: Gold stinger payout animation triggers!
                if (confettiView != null) {
                    confettiView.startConfetti();
                }

                // Return to dashboard after 4 seconds to enjoy celebration!
                new Handler().postDelayed(() -> finish(), 4000);
            }
            @Override
            public void onFailure(String msg) {
                hideProgress();
                showToast("Failed: " + msg);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // GPS Dot Pulse Animation
    // ─────────────────────────────────────────────────────────────────

    private void pulseGpsDot() {
        gpsDot.animate().alpha(0.2f).setDuration(600).withEndAction(() -> {
            if (!isActivityDestroyed) {
                gpsDot.animate().alpha(1.0f).setDuration(600).withEndAction(() -> {
                    if (!isActivityDestroyed) pulseGpsDot();
                }).start();
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    // ─────────────────────────────────────────────────────────────────
    // Glassmorphic calling screen (simulated VoIP)
    // ─────────────────────────────────────────────────────────────────

    private boolean isWaveAnimating = false;

    private void showPremiumCallDialog() {
        if (isFinishing()) return;

        com.google.android.material.bottomsheet.BottomSheetDialog callDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_glass_call, null);
        callDialog.setContentView(sheetView);

        TextView txtState = sheetView.findViewById(R.id.call_txt_state);
        TextView txtName = sheetView.findViewById(R.id.call_txt_name);
        CircleImageView imgAvatar = sheetView.findViewById(R.id.call_img_avatar);
        View ring1 = sheetView.findViewById(R.id.call_ring1);
        View ring2 = sheetView.findViewById(R.id.call_ring2);
        View hangup = sheetView.findViewById(R.id.call_layout_hangup);
        View voiceWave = sheetView.findViewById(R.id.call_layout_voice_wave);

        // Customize customer details
        txtName.setText(customerName != null ? customerName : "WorkBee Customer");
        if (customerProfileUrl != null && !customerProfileUrl.isEmpty()) {
            Glide.with(this)
                    .load(customerProfileUrl)
                    .placeholder(R.drawable.ic_workbee_logo)
                    .into(imgAvatar);
        }

        // Start Concentric pulsing rings loop
        animatePulsingCallRings(ring1, ring2);

        // Simulated connection & ticking timing
        final Handler callHandler = new Handler();
        final int[] callSecs = {0};
        final boolean[] isCallConnected = {false};

        // Connect call after 3s
        callHandler.postDelayed(() -> {
            if (callDialog.isShowing()) {
                isCallConnected[0] = true;
                txtState.setText("Connected • 00:00");
                txtState.setTextColor(Color.parseColor("#4CAF50"));
                if (voiceWave != null) {
                    voiceWave.setAlpha(1.0f);
                    animateVoiceWave(voiceWave);
                }
            }
        }, 3000);

        // Call ticker loop
        final Runnable ticker = new Runnable() {
            @Override
            public void run() {
                if (!callDialog.isShowing()) return;

                if (isCallConnected[0]) {
                    callSecs[0]++;
                    int m = callSecs[0] / 60;
                    int s = callSecs[0] % 60;
                    txtState.setText(String.format(Locale.getDefault(), "Connected • %02d:%02d", m, s));
                }
                callHandler.postDelayed(this, 1000);
            }
        };
        callHandler.postDelayed(ticker, 1000);

        hangup.setOnClickListener(v -> {
            isWaveAnimating = false;
            callHandler.removeCallbacksAndMessages(null);
            callDialog.dismiss();
            showToast("Call Ended 📞");
        });

        callDialog.setOnDismissListener(dialog -> {
            isWaveAnimating = false;
            callHandler.removeCallbacksAndMessages(null);
            if (ring1 != null) ring1.animate().cancel();
            if (ring2 != null) ring2.animate().cancel();
        });

        callDialog.show();
    }

    private void animatePulsingCallRings(View ring1, View ring2) {
        if (ring1 == null || ring2 == null || isActivityDestroyed) return;

        ring1.setScaleX(1.0f);
        ring1.setScaleY(1.0f);
        ring1.setAlpha(0.6f);
        ring1.animate().scaleX(1.5f).scaleY(1.5f).alpha(0.0f).setDuration(1500).withEndAction(() -> {
            if (!isActivityDestroyed) animatePulsingCallRings(ring1, ring2);
        }).start();

        new Handler().postDelayed(() -> {
            if (ring2 != null && !isActivityDestroyed) {
                ring2.setScaleX(1.0f);
                ring2.setScaleY(1.0f);
                ring2.setAlpha(0.4f);
                ring2.animate().scaleX(1.7f).scaleY(1.7f).alpha(0.0f).setDuration(1500).start();
            }
        }, 600);
    }

    private void animateVoiceWave(View waveContainer) {
        if (waveContainer == null) return;
        isWaveAnimating = true;

        View b1 = waveContainer.findViewById(R.id.wave_bar1);
        View b2 = waveContainer.findViewById(R.id.wave_bar2);
        View b3 = waveContainer.findViewById(R.id.wave_bar3);
        View b4 = waveContainer.findViewById(R.id.wave_bar4);
        View b5 = waveContainer.findViewById(R.id.wave_bar5);
        View b6 = waveContainer.findViewById(R.id.wave_bar6);
        View b7 = waveContainer.findViewById(R.id.wave_bar7);

        java.util.Random rand = new java.util.Random();
        Handler wh = new Handler();
        wh.post(new Runnable() {
            @Override
            public void run() {
                if (!isWaveAnimating || isActivityDestroyed) return;

                if (b1 != null) b1.setScaleY(0.4f + rand.nextFloat() * 1.2f);
                if (b2 != null) b2.setScaleY(0.4f + rand.nextFloat() * 1.2f);
                if (b3 != null) b3.setScaleY(0.4f + rand.nextFloat() * 1.2f);
                if (b4 != null) b4.setScaleY(0.4f + rand.nextFloat() * 1.2f);
                if (b5 != null) b5.setScaleY(0.4f + rand.nextFloat() * 1.2f);
                if (b6 != null) b6.setScaleY(0.4f + rand.nextFloat() * 1.2f);
                if (b7 != null) b7.setScaleY(0.4f + rand.nextFloat() * 1.2f);

                wh.postDelayed(this, 120);
            }
        });
    }

    private com.google.android.gms.maps.model.BitmapDescriptor getBitmapDescriptorFromVector(int vectorResId) {
        android.graphics.drawable.Drawable vectorDrawable = androidx.core.content.ContextCompat.getDrawable(this, vectorResId);
        if (vectorDrawable == null) return null;
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void triggerEmergencySos() {
        if (isFinishing()) return;

        // Blinking red siren screen countdown dialog
        final com.google.android.material.bottomsheet.BottomSheetDialog sosDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sos_alert, null);
        sosDialog.setContentView(dialogView);
        sosDialog.setCancelable(false);

        TextView txtCountdown = dialogView.findViewById(R.id.sos_txt_countdown);
        View btnCancelSos = dialogView.findViewById(R.id.sos_btn_cancel);
        View mainContainer = dialogView.findViewById(R.id.sos_main_container);

        final Handler handler = new Handler();
        final int[] secondsLeft = {5};
        final boolean[] isSirenActive = {true};

        // Pulsing background color animation loop
        final Runnable colorPulse = new Runnable() {
            private boolean isRed = false;
            @Override
            public void run() {
                if (!isSirenActive[0]) return;
                mainContainer.setBackgroundColor(isRed ? Color.parseColor("#B30000") : Color.parseColor("#330000"));
                isRed = !isRed;
                
                // Vibrate device if possible for haptic feedback
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(100);
                    }
                }
                handler.postDelayed(this, 300);
            }
        };
        handler.post(colorPulse);

        final Runnable countdown = new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (secondsLeft[0] <= 0) {
                    isSirenActive[0] = false;
                    sosDialog.dismiss();
                    showEmergencyConfirmedAlert();
                } else {
                    txtCountdown.setText(String.valueOf(secondsLeft[0]));
                    secondsLeft[0]--;
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(countdown);

        btnCancelSos.setOnClickListener(v -> {
            isSirenActive[0] = false;
            handler.removeCallbacksAndMessages(null);
            sosDialog.dismiss();
            showToast("Emergency SOS Cancelled 🛡️");
        });

        sosDialog.show();
    }

    private void showEmergencyConfirmedAlert() {
        double lat = (workerLat != 0) ? workerLat : 37.7749;
        double lng = (workerLng != 0) ? workerLng : -122.4194;
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🚨 Emergency Dispatch Active")
                .setMessage(String.format(Locale.getDefault(),
                        "Coordinates (%.6f, %.6f) shared with Police and local Emergency Response teams.\n" +
                        "A safety specialist is calling you immediately. Please stay safe!",
                        lat, lng))
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void onDestroy() {
        isActivityDestroyed = true;
        isWaveAnimating = false;
        selfPollHandler.removeCallbacksAndMessages(null);
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }
}
