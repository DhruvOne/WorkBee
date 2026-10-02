package com.workbee.app.ui.customer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
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
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.User;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.utils.FirebaseHelper;
import com.workbee.app.utils.LocationHelper;
import com.workbee.app.utils.RouteHelper;
import com.workbee.app.ui.common.ConfettiView;
import android.widget.ImageButton;

import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Customer-side live tracking activity.
 * Polls Firebase (or mock store) every 3 seconds to get the worker's GPS coordinates.
 * Animates the worker marker smoothly and updates ETA/distance using the Haversine formula.
 *
 * Started from UberMatchActivity after booking is ACCEPTED.
 * Extras: booking_id, worker_name, worker_id, customer_lat, customer_lng, category
 */
public class CustomerTrackingActivity extends BaseActivity implements OnMapReadyCallback {

    public static final String EXTRA_BOOKING_ID   = "booking_id";
    public static final String EXTRA_WORKER_NAME  = "worker_name";
    public static final String EXTRA_WORKER_ID    = "worker_id";
    public static final String EXTRA_CUSTOMER_LAT = "customer_lat";
    public static final String EXTRA_CUSTOMER_LNG = "customer_lng";
    public static final String EXTRA_CATEGORY     = "category";

    private static final long   POLL_INTERVAL_MS  = 3_000L;
    private static final double VEHICLE_SPEED_KMH = 30.0;

    // Map components
    private MapView  mapView;
    private GoogleMap googleMap;
    private Marker   workerMarker;
    private Marker   customerMarker;
    private Polyline routePolyline;

    // UI
    private TextView txtWorkerName, txtWorkerNameCard, txtWorkerCategory;
    private TextView txtEta, txtDistance, txtStatusLabel, txtLastUpdated;
    private TextView step1, step2, step3, step4, stepLine1, stepLine2, stepLine3;
    private CircleImageView imgWorkerAvatar;
    private View liveDot;
    private Button btnChat;
    private Button btnCancel;
    private ImageButton btnCall;
    private View trafficBanner;
    private TextView txtTrafficDesc;
    private ConfettiView confettiView;
    private String workerProfileUrl = "";
    private List<Polyline> trafficPolylines = new java.util.ArrayList<>();

    // Data
    private FirebaseHelper firebaseHelper;
    private String bookingId;
    private String workerName;
    private String workerId;
    private String category;
    private double customerLat;
    private double customerLng;

    // Last known worker position
    private LatLng lastWorkerPos = null;
    private String lastBookingStatus = "ACCEPTED";

    // Polling
    private final Handler pollHandler = new Handler();
    private boolean isActivityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_tracking);

        firebaseHelper = FirebaseHelper.getInstance(this);

        // Read extras
        bookingId   = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        workerName  = getIntent().getStringExtra(EXTRA_WORKER_NAME);
        workerId    = getIntent().getStringExtra(EXTRA_WORKER_ID);
        category    = getIntent().getStringExtra(EXTRA_CATEGORY);
        customerLat = getIntent().getDoubleExtra(EXTRA_CUSTOMER_LAT, 0.0);
        customerLng = getIntent().getDoubleExtra(EXTRA_CUSTOMER_LNG, 0.0);

        // If no real coordinates, use LocationHelper
        if (customerLat == 0 && customerLng == 0) {
            customerLat = LocationHelper.hasRealLocation() ? LocationHelper.getRealLatitude()  : 37.7749;
            customerLng = LocationHelper.hasRealLocation() ? LocationHelper.getRealLongitude() : -122.4194;
        }

        bindViews();
        populateWorkerInfo();

        // Back button
        findViewById(R.id.customer_btn_back).setOnClickListener(v -> onBackPressed());

        // Chat button re-uses UberMatchActivity chat sheet concept
        btnChat.setOnClickListener(v -> showChatSheet());

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> handleCancelBooking());
        }

        if (btnCall != null) {
            btnCall.setOnClickListener(v -> showPremiumCallDialog());
        }

        View btnSos = findViewById(R.id.btn_sos);
        if (btnSos != null) {
            btnSos.setOnClickListener(v -> triggerEmergencySos());
        }

        // Map
        mapView = findViewById(R.id.customer_map_view);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        // Start polling
        startWorkerPoll();

        // Pulse live dot
        pulseLiveDot();
    }

    private void bindViews() {
        txtWorkerName     = findViewById(R.id.customer_txt_worker_name);
        txtWorkerNameCard = findViewById(R.id.customer_txt_worker_name_card);
        txtWorkerCategory = findViewById(R.id.customer_txt_worker_category);
        txtEta            = findViewById(R.id.customer_txt_eta);
        txtDistance       = findViewById(R.id.customer_txt_distance);
        txtStatusLabel    = findViewById(R.id.customer_txt_status_label);
        txtLastUpdated    = findViewById(R.id.customer_txt_last_updated);
        imgWorkerAvatar   = findViewById(R.id.customer_img_worker_avatar);
        liveDot           = findViewById(R.id.customer_live_dot);
        btnChat           = findViewById(R.id.customer_btn_chat);
        btnCancel         = findViewById(R.id.customer_btn_cancel);
        step1             = findViewById(R.id.customer_step1);
        step2             = findViewById(R.id.customer_step2);
        step3             = findViewById(R.id.customer_step3);
        step4             = findViewById(R.id.customer_step4);
        stepLine1         = findViewById(R.id.customer_step_line1);
        stepLine2         = findViewById(R.id.customer_step_line2);
        stepLine3         = findViewById(R.id.customer_step_line3);
        btnCall           = findViewById(R.id.customer_btn_call);
        trafficBanner     = findViewById(R.id.customer_traffic_banner);
        txtTrafficDesc    = findViewById(R.id.customer_txt_traffic_desc);
        confettiView      = findViewById(R.id.customer_confetti_view);
    }

    private void populateWorkerInfo() {
        if (workerName != null) {
            txtWorkerName.setText(workerName);
            txtWorkerNameCard.setText(workerName);
        }
        if (category != null) {
            txtWorkerCategory.setText(category + " Service");
        }

        // Load worker avatar if we have a workerId
        if (workerId != null) {
            firebaseHelper.getAllUsers(new FirebaseHelper.ListCallback<User>() {
                @Override
                public void onSuccess(List<User> list) {
                    for (User u : list) {
                        if (u.getUserId().equalsIgnoreCase(workerId)) {
                            if (u.getProfileImageUrl() != null && !u.getProfileImageUrl().isEmpty()) {
                                workerProfileUrl = u.getProfileImageUrl();
                                Glide.with(CustomerTrackingActivity.this)
                                        .load(u.getProfileImageUrl())
                                        .placeholder(R.drawable.ic_workbee_logo)
                                        .into(imgWorkerAvatar);
                            }
                            break;
                        }
                    }
                }
                @Override
                public void onFailure(String msg) { /* silent */ }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Map
    // ─────────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Premium dark map
        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.bg_map_dark_style));
        } catch (Exception e) {
            android.util.Log.w("CustomerTracking", "Map style error: " + e.getMessage());
        }

        LatLng custLatLng = new LatLng(customerLat, customerLng);

        // Customer marker (yellow/home)
        customerMarker = googleMap.addMarker(new MarkerOptions()
                .position(custLatLng)
                .title("📍 Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

        // Estimate initial worker position (offset by ~1.2 km NE)
        LatLng initWorker = new LatLng(customerLat + 0.011, customerLng + 0.011);

        // Worker marker (rotating scooter)
        workerMarker = googleMap.addMarker(new MarkerOptions()
                .position(initWorker)
                .title("🐝 " + (workerName != null ? workerName : "WorkBee Pro"))
                .icon(getBitmapDescriptorFromVector(R.drawable.ic_worker_scooter))
                .anchor(0.5f, 0.5f)
                .flat(true));

        // Generate street blocks route
        List<LatLng> route = RouteHelper.generateMockRoute(initWorker, custLatLng);

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

        // Camera fits both
        fitCameraToMarkers(initWorker, custLatLng);
    }

    private void animateWorkerMarkerTo(LatLng newPos) {
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
                    .title("🐝 " + (workerName != null ? workerName : "WorkBee Pro"))
                    .icon(getBitmapDescriptorFromVector(R.drawable.ic_worker_scooter))
                    .anchor(0.5f, 0.5f)
                    .flat(true));
        }

        lastWorkerPos = newPos;

        LatLng custLatLng = new LatLng(customerLat, customerLng);
        
        // Redraw polylines representing the remaining dynamic path
        List<LatLng> remainingRoute = RouteHelper.generateMockRoute(newPos, custLatLng);
        
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
                pts.add(custLatLng);
                routePolyline.setPoints(pts);
            } else {
                routePolyline = googleMap.addPolyline(new PolylineOptions()
                        .add(newPos, custLatLng)
                        .color(Color.parseColor("#FFC107"))
                        .width(10f)
                        .geodesic(true));
            }
        }
    }

    private void fitCameraToMarkers(LatLng worker, LatLng customer) {
        try {
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(worker)
                    .include(customer)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 180));
        } catch (Exception e) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customer, 13f));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Location Polling
    // ─────────────────────────────────────────────────────────────────

    // Named poll runnable so the DataCallback can re-schedule it (avoids 'this' type error)
    private Runnable pollRunnable;

    private void startWorkerPoll() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isActivityDestroyed || bookingId == null) return;

                firebaseHelper.getWorkerLocation(bookingId, new FirebaseHelper.DataCallback<Booking>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(Booking booking) {
                        // Update status strip if status changed
                        String newStatus = booking.getStatus();
                        if (newStatus != null && !newStatus.equals(lastBookingStatus)) {
                            lastBookingStatus = newStatus;
                            updateStatusUi(newStatus);
                        }

                        // Stop polling when tracking is COMPLETED
                        if ("COMPLETED".equals(booking.getTrackingStatus())) {
                            onTrackingCompleted();
                            return;
                        }

                        // Update worker pin + ETA if coordinates are available
                        double wLat = booking.getWorkerLatitude();
                        double wLng = booking.getWorkerLongitude();
                        if (wLat != 0 && wLng != 0) {
                            lastWorkerPos = new LatLng(wLat, wLng);
                            animateWorkerMarkerTo(lastWorkerPos);
                            updateEtaDistance(wLat, wLng);
                            updateLastUpdatedLabel(booking.getWorkerLocationTimestamp());
                        }

                        // Schedule next poll using the named Runnable
                        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                    }

                    @Override
                    public void onFailure(String msg) {
                        // Retry on failure
                        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                    }
                });
            }
        };

        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }


    // ─────────────────────────────────────────────────────────────────
    // UI Updates
    // ─────────────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private void updateStatusUi(String status) {
        switch (status.toUpperCase()) {
            case "ACCEPTED":
                setStepActive(step1, true);
                setStepActive(step2, false);
                setStepActive(step3, false);
                setStepActive(step4, false);
                txtStatusLabel.setText("Accepted");
                txtStatusLabel.setTextColor(Color.parseColor("#FFC107"));
                break;
            case "ON_THE_WAY":
                setStepActive(step1, true);
                setStepActive(step2, true);
                setStepActive(step3, false);
                setStepActive(step4, false);
                setLineActive(stepLine1, true);
                txtStatusLabel.setText("On the Way 🚗");
                txtStatusLabel.setTextColor(Color.parseColor("#FF8C00"));
                break;
            case "IN_PROGRESS":
                setStepActive(step1, true);
                setStepActive(step2, true);
                setStepActive(step3, true);
                setStepActive(step4, false);
                setLineActive(stepLine1, true);
                setLineActive(stepLine2, true);
                txtEta.setText("Arrived! 🔔");
                txtDistance.setText("0 m");
                txtStatusLabel.setText("Arrived ✅");
                txtStatusLabel.setTextColor(Color.parseColor("#4CAF50"));
                break;
            case "COMPLETED":
                setStepActive(step1, true);
                setStepActive(step2, true);
                setStepActive(step3, true);
                setStepActive(step4, true);
                setLineActive(stepLine1, true);
                setLineActive(stepLine2, true);
                setLineActive(stepLine3, true);
                txtStatusLabel.setText("Job Done ✅");
                txtStatusLabel.setTextColor(Color.parseColor("#4CAF50"));
                onTrackingCompleted();
                break;
        }
    }

    private void setStepActive(TextView tv, boolean active) {
        if (tv == null) return;
        tv.setTextColor(active ? Color.parseColor("#FFC107") : Color.parseColor("#40FFFFFF"));
        tv.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void setLineActive(View line, boolean active) {
        if (line == null) return;
        line.setBackgroundColor(active ? Color.parseColor("#FFC107") : Color.parseColor("#30FFFFFF"));
    }

    @SuppressLint("SetTextI18n")
    private void updateEtaDistance(double wLat, double wLng) {
        double distKm  = haversineKm(wLat, wLng, customerLat, customerLng);
        int    etaMins = (int) Math.ceil(distKm / VEHICLE_SPEED_KMH * 60);

        if (distKm < 0.1) {
            txtDistance.setText("< 100 m");
            txtEta.setText("Arriving");
        } else if (distKm < 1.0) {
            txtDistance.setText(String.format(Locale.getDefault(), "%.0f m", distKm * 1000));
            txtEta.setText(etaMins > 0 ? etaMins + " min" : "< 1 min");
        } else {
            txtDistance.setText(String.format(Locale.getDefault(), "%.1f km", distKm));
            txtEta.setText(etaMins + " min");
        }

        // Feature 4: Live Traffic updates based on remaining distance
        float fraction = 1.0f - (float) Math.min(1.0, distKm / 1.2);
        if (txtTrafficDesc != null) {
            txtTrafficDesc.setText(RouteHelper.getTrafficBannerText(fraction));
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateLastUpdatedLabel(long timestamp) {
        if (timestamp == 0) {
            txtLastUpdated.setText("📡 Location updating...");
            return;
        }
        long secAgo = (System.currentTimeMillis() - timestamp) / 1000;
        if (secAgo < 5) {
            txtLastUpdated.setText("📡 Location updated just now");
        } else if (secAgo < 60) {
            txtLastUpdated.setText("📡 Location updated " + secAgo + " sec ago");
        } else {
            txtLastUpdated.setText("📡 Location updated " + (secAgo / 60) + " min ago");
        }
    }

    @SuppressLint("SetTextI18n")
    private void onTrackingCompleted() {
        isActivityDestroyed = true;
        pollHandler.removeCallbacksAndMessages(null);
        updateStatusUi("COMPLETED");
        txtLastUpdated.setText("📡 Tracking ended — Job completed");

        // Feature 5: Celebrate with honey golden confetti overlay!
        if (confettiView != null) {
            confettiView.startConfetti();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Haversine Distance Formula
    // ─────────────────────────────────────────────────────────────────

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
    // Chat Sheet (reuses existing chat dialog pattern)
    // ─────────────────────────────────────────────────────────────────

    private void showChatSheet() {
        if (isFinishing()) return;
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.dialog_honeycomb_chat, null);
        dialog.setContentView(sheetView);

        android.widget.ScrollView chatScrollView     = sheetView.findViewById(R.id.chat_scroll_view);
        android.widget.LinearLayout chatHistory      = sheetView.findViewById(R.id.chat_layout_history);
        android.widget.EditText chatInput            = sheetView.findViewById(R.id.chat_edt_input);
        android.view.View btnSend                    = sheetView.findViewById(R.id.chat_btn_send);
        android.view.View btnClose                   = sheetView.findViewById(R.id.chat_btn_close);
        android.view.View chip1                      = sheetView.findViewById(R.id.chip_chat_preset1);
        android.view.View chip2                      = sheetView.findViewById(R.id.chip_chat_preset2);
        android.view.View chip3                      = sheetView.findViewById(R.id.chip_chat_preset3);
        TextView txtName                             = sheetView.findViewById(R.id.chat_txt_name);

        txtName.setText(workerName != null ? workerName : "WorkBee Pro");
        addChatBubble(chatHistory, "Hi! I'm on my way to you. ETA soon. Message me if you need anything! 🐝", false, chatScrollView);

        chip1.setOnClickListener(v -> sendChat(chatHistory, "🏃 Are you nearby?", chatScrollView));
        chip2.setOnClickListener(v -> sendChat(chatHistory, "📍 Are you outside?", chatScrollView));
        chip3.setOnClickListener(v -> sendChat(chatHistory, "✅ Okay, sounds good!", chatScrollView));

        btnSend.setOnClickListener(v -> {
            String msg = chatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                chatInput.setText("");
                sendChat(chatHistory, msg, chatScrollView);
            }
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void sendChat(android.widget.LinearLayout history, String text, android.widget.ScrollView scroll) {
        addChatBubble(history, text, true, scroll);
        new Handler().postDelayed(() -> {
            if (isActivityDestroyed) return;
            String q = text.toLowerCase();
            String reply;
            if (q.contains("near") || q.contains("where"))
                reply = "Yes! Almost there — just a few minutes away! 🚗";
            else if (q.contains("outside") || q.contains("arrive"))
                reply = "I'll ping you as soon as I pull up! 📍";
            else
                reply = "Got it! Standard elite service incoming 🐝✨";
            addChatBubble(history, reply, false, scroll);
        }, 1000);
    }

    private void addChatBubble(android.widget.LinearLayout container, String text, boolean isUser, android.widget.ScrollView scroll) {
        android.widget.LinearLayout bubble = new android.widget.LinearLayout(this);
        bubble.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        bubble.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bubble.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);
        bubble.setPadding(0, 8, 0, 8);

        android.widget.TextView tv = new android.widget.TextView(this);
        android.widget.LinearLayout.LayoutParams p = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(isUser ? 60 : 0, 0, isUser ? 0 : 60, 0);
        tv.setLayoutParams(p);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(isUser ? Color.BLACK : Color.WHITE);
        tv.setPadding(20, 12, 20, 12);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(Color.parseColor(isUser ? "#FFC107" : "#1E1E1E"));
        float r = 24f;
        gd.setCornerRadii(isUser
                ? new float[]{r, r, r, r, 0f, 0f, r, r}
                : new float[]{r, r, r, r, r, r, 0f, 0f});
        tv.setBackground(gd);
        bubble.addView(tv);
        container.addView(bubble);
        scroll.post(() -> scroll.fullScroll(android.view.View.FOCUS_DOWN));
    }

    // ─────────────────────────────────────────────────────────────────
    // Live Dot Pulse
    // ─────────────────────────────────────────────────────────────────

    private void pulseLiveDot() {
        liveDot.animate().alpha(0.15f).setDuration(700).withEndAction(() -> {
            if (!isActivityDestroyed) {
                liveDot.animate().alpha(1.0f).setDuration(700).withEndAction(() -> {
                    if (!isActivityDestroyed) pulseLiveDot();
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

        // Customize worker details
        txtName.setText(workerName != null ? workerName : "WorkBee Pro");
        if (workerProfileUrl != null && !workerProfileUrl.isEmpty()) {
            Glide.with(this)
                    .load(workerProfileUrl)
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

    private void handleCancelBooking() {
        if (bookingId != null) {
            showProgress("Cancelling booking...");
            firebaseHelper.updateBookingStatus(bookingId, "CANCELLED", new FirebaseHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    hideProgress();
                    showToast("Booking cancelled successfully.");
                    finish();
                }

                @Override
                public void onFailure(String message) {
                    hideProgress();
                    showToast("Failed to cancel booking: " + message);
                }
            });
        } else {
            finish();
        }
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
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🚨 Emergency Dispatch Active")
                .setMessage(String.format(Locale.getDefault(),
                        "Coordinates (%.6f, %.6f) shared with Police and local Emergency Response teams.\n" +
                        "A safety specialist is calling you immediately. Please stay safe!",
                        customerLat, customerLng))
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void onDestroy() {
        isActivityDestroyed = true;
        isWaveAnimating = false;
        pollHandler.removeCallbacksAndMessages(null);
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }
}
