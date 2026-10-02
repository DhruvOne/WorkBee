package com.workbee.app.ui.customer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.workbee.app.R;
import com.workbee.app.data.model.Booking;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.User;
import android.graphics.Typeface;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.utils.FirebaseHelper;
import com.workbee.app.utils.LocationHelper;

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

import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class UberMatchActivity extends BaseActivity {

    private FirebaseHelper firebaseHelper;
    private String bookingId;
    private String targetProviderName;

    private TextView txtStatusTitle, txtStatusDesc;
    private TextView txtTimelineStep1, txtTimelineStep2, txtTimelineStep3;
    private View layoutRadar;

    private View cardMatchSuccess;
    private CircleImageView imgAvatar;
    private TextView txtMatchedName, txtMatchedRating, txtMatchedEta;
    private Button btnCancel;
    private Button btnMessagePro;
    private Button btnTrackLive; // NEW: live tracking button

    // Matched booking reference (needed to pass data to tracking activity)
    private Booking matchedBooking;

    private Handler pollHandler = new Handler();
    private boolean isDestroyed = false;
    private int pollingCount = 0;

    private Handler etaHandler = new Handler();
    private int etaMinutes = 8;

    // Concentric Radar ring views
    private View ring1, ring2, ring3;
    private Handler radarHandler = new Handler();
    private boolean isRadarAnimating = true;

    // Google Maps properties
    private MapView mapView;
    private GoogleMap googleMap;
    private Polyline routePolyline;
    private Marker providerMarker;
    private Marker customerMarker;
    private LatLng providerStartLatLng;
    private LatLng customerLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uber_match);

        firebaseHelper = FirebaseHelper.getInstance(this);

        bookingId = getIntent().getStringExtra("booking_id");
        targetProviderName = getIntent().getStringExtra("provider_name");

        txtStatusTitle = findViewById(R.id.txt_match_status_title);
        txtStatusDesc = findViewById(R.id.txt_match_status_desc);
        txtTimelineStep1 = findViewById(R.id.txt_timeline_step1);
        txtTimelineStep2 = findViewById(R.id.txt_timeline_step2);
        txtTimelineStep3 = findViewById(R.id.txt_timeline_step3);
        layoutRadar = findViewById(R.id.layout_radar_searching);

        cardMatchSuccess = findViewById(R.id.card_match_success);
        imgAvatar = findViewById(R.id.img_matched_avatar);
        txtMatchedName = findViewById(R.id.txt_matched_name);
        txtMatchedRating = findViewById(R.id.txt_matched_rating);
        txtMatchedEta = findViewById(R.id.txt_matched_eta);
        btnCancel = findViewById(R.id.btn_cancel_match);
        btnMessagePro = findViewById(R.id.btn_message_pro);
        btnTrackLive = findViewById(R.id.btn_track_live);

        // Initialize MapView
        mapView = findViewById(R.id.map_view);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
        }

        if (targetProviderName != null) {
            txtStatusDesc.setText("Pinging " + targetProviderName + " first within 2.5 km...");
        }

        btnCancel.setOnClickListener(v -> handleCancelBooking());
        btnMessagePro.setOnClickListener(v -> showChatDialog());

        android.widget.Button btnCancelBroadcast = findViewById(R.id.btn_cancel_broadcast);
        if (btnCancelBroadcast != null) {
            btnCancelBroadcast.setOnClickListener(v -> handleCancelBooking());
        }

        // Start Concentric radar rings pulse loops
        startRadarAnimations();

        startBookingPollLoop();
    }

    private void startBookingPollLoop() {
        pollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isDestroyed) return;

                pollingCount++;
                checkBookingStatus();

                // Poll every 1.5 seconds
                pollHandler.postDelayed(this, 1500);
            }
        }, 1500);
    }

    private void checkBookingStatus() {
        if (bookingId == null) return;

        firebaseHelper.getAllBookings(new FirebaseHelper.ListCallback<Booking>() {
            @Override
            public void onSuccess(List<Booking> list) {
                Booking target = null;
                for (Booking b : list) {
                    if (bookingId.equalsIgnoreCase(b.getBookingId())) {
                        target = b;
                        break;
                    }
                }

                if (target != null) {
                    String status = target.getStatus();
                    if ("ACCEPTED".equalsIgnoreCase(status)) {
                        handleMatchSuccess(target);
                    } else if ("REJECTED".equalsIgnoreCase(status)) {
                        handleMatchDeclined();
                    } else {
                        // Still pending, update search logs to make it feel alive!
                        if (pollingCount == 4) {
                            txtStatusTitle.setText("Polishing Vibe Radar Signals... 📡");
                            txtStatusDesc.setText("Extending broadcast radius to 4.2 km... Finding active pros.");
                        } else if (pollingCount == 8) {
                            txtStatusTitle.setText("Broadcasting Vibe Signal... ⚡");
                            txtStatusDesc.setText("Re-pinging active service providers on the grid...");
                        }
                    }
                }
            }

            @Override
            public void onFailure(String message) {}
        });
    }

    private void handleMatchSuccess(Booking booking) {
        // Stop polling
        isDestroyed = true;
        pollHandler.removeCallbacksAndMessages(null);

        // Store reference for tracking
        matchedBooking = booking;

        // Stop radar pulsing scale loops
        stopRadarAnimations();

        layoutRadar.setVisibility(View.GONE);
        findViewById(R.id.card_match_status).setVisibility(View.GONE);
        cardMatchSuccess.setVisibility(View.VISIBLE);
        android.widget.Button btnCancelBroadcast = findViewById(R.id.btn_cancel_broadcast);
        if (btnCancelBroadcast != null) {
            btnCancelBroadcast.setVisibility(View.GONE);
        }

        // Update Timeline
        txtTimelineStep2.setText("✓ Match Secured! 🎉");
        txtTimelineStep2.setTextColor(Color.parseColor("#FFC107"));
        txtTimelineStep2.setTypeface(null, Typeface.BOLD);

        txtTimelineStep3.setText("⌛ Provider en route to your location");
        txtTimelineStep3.setTextColor(Color.WHITE);

        // Fill matched provider info
        txtMatchedName.setText(booking.getProviderName());

        // Show the Track Live button
        if (btnTrackLive != null) {
            btnTrackLive.setVisibility(View.VISIBLE);
            btnTrackLive.setOnClickListener(v -> launchCustomerTracking(booking));
        }
        
        firebaseHelper.getProviderDetails(booking.getProviderId(), new FirebaseHelper.DataCallback<Provider>() {
            @Override
            public void onSuccess(Provider provider) {
                txtMatchedRating.setText(String.format(Locale.getDefault(), "%.1f ★ Vibe Level V Certified", provider.getRating()));
                
                // Fetch customer user profile to render matched provider photo (or use standard)
                firebaseHelper.getAllUsers(new FirebaseHelper.ListCallback<User>() {
                    @Override
                    public void onSuccess(List<User> list) {
                        for (User u : list) {
                            if (u.getUserId().equalsIgnoreCase(booking.getProviderId())) {
                                if (u.getProfileImageUrl() != null && !u.getProfileImageUrl().isEmpty()) {
                                    Glide.with(UberMatchActivity.this)
                                         .load(u.getProfileImageUrl())
                                         .placeholder(R.drawable.ic_workbee_logo)
                                         .into(imgAvatar);
                                }
                                break;
                            }
                        }
                    }
                    @Override
                    public void onFailure(String message) {}
                });
            }

            @Override
            public void onFailure(String message) {}
        });

        // Initialize and setup the Google Map
        if (mapView != null) {
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    googleMap = map;
                    setupMapAndRoute();
                }
            });
        }

        startEtaSimulator();
    }

    private void handleMatchDeclined() {
        Toast.makeText(this, "Match declined. Re-broadcasting nearby...", Toast.LENGTH_SHORT).show();
        txtStatusTitle.setText("Ride Declined. Searching Next Nearest Pro... 📡");
        txtStatusDesc.setText("Vibe matching other service providers in your neighborhood...");
        pollingCount = 0;

        // Reset booking status back to pending in mock database to seek next driver
        firebaseHelper.updateBookingStatus(bookingId, "PENDING", new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {}
            @Override
            public void onFailure(String message) {}
        });
    }

    private void startEtaSimulator() {
        etaHandler.postDelayed(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                if (etaMinutes > 1) {
                    etaMinutes -= 2;
                    txtMatchedEta.setText(" " + etaMinutes + " mins (on the way)");

                    // Update Google Map provider marker en route!
                    if (googleMap != null && providerMarker != null && providerStartLatLng != null && customerLatLng != null) {
                        float progress = (8f - etaMinutes) / 8f; // progress from 0 to 1
                        LatLng currentPos = interpolateLatLng(providerStartLatLng, customerLatLng, progress);
                        providerMarker.setPosition(currentPos);

                        if (routePolyline != null) {
                            java.util.List<LatLng> updatedPath = new java.util.ArrayList<>();
                            updatedPath.add(currentPos);
                            updatedPath.add(customerLatLng);
                            routePolyline.setPoints(updatedPath);
                        }
                    }

                    etaHandler.postDelayed(this, 6000); // decrement every 6s
                } else {
                    txtMatchedEta.setText(" Arrived! 🔔 (Outside your door)");
                    txtTimelineStep3.setText("✓ Provider arrived! 🔔");
                    txtTimelineStep3.setTextColor(Color.parseColor("#FFC107"));
                    txtTimelineStep3.setTypeface(null, Typeface.BOLD);
                    Toast.makeText(UberMatchActivity.this, "Your WorkBee Pro has arrived! 🔔", Toast.LENGTH_LONG).show();

                    // Snap provider marker to customer
                    if (googleMap != null && providerMarker != null && customerLatLng != null) {
                        providerMarker.setPosition(customerLatLng);
                        if (routePolyline != null) {
                            routePolyline.remove();
                        }
                    }

                    // Transform cancel button into receipt card trigger button
                    btnCancel.setText("Complete & View Receipt 🧾");
                    btnCancel.setTextColor(Color.BLACK);
                    btnCancel.setBackgroundColor(Color.parseColor("#FFC107"));
                    btnCancel.setOnClickListener(v -> showInvoiceDialog());
                }
            }
        }, 6000);
    }

    /** Opens the customer live-tracking map for this booking */
    private void launchCustomerTracking(Booking booking) {
        Intent intent = new Intent(this, CustomerTrackingActivity.class);
        intent.putExtra(CustomerTrackingActivity.EXTRA_BOOKING_ID,   booking.getBookingId());
        intent.putExtra(CustomerTrackingActivity.EXTRA_WORKER_NAME,  booking.getProviderName());
        intent.putExtra(CustomerTrackingActivity.EXTRA_WORKER_ID,    booking.getProviderId());
        intent.putExtra(CustomerTrackingActivity.EXTRA_CATEGORY,     booking.getCategory());
        intent.putExtra(CustomerTrackingActivity.EXTRA_CUSTOMER_LAT, customerLatLng != null ? customerLatLng.latitude  : 0.0);
        intent.putExtra(CustomerTrackingActivity.EXTRA_CUSTOMER_LNG, customerLatLng != null ? customerLatLng.longitude : 0.0);
        startActivity(intent);
    }

    private void handleCancelBooking() {

        if (bookingId != null) {
            showProgress("Cancelling booking dispatch...");
            firebaseHelper.updateBookingStatus(bookingId, "CANCELLED", new FirebaseHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    hideProgress();
                    Toast.makeText(UberMatchActivity.this, "Booking cancelled successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String message) {
                    hideProgress();
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    // CONCENTRIC PULSING RADAR ANIMATIONS (Option 1)
    private void startRadarAnimations() {
        ring1 = findViewById(R.id.radar_ring1);
        ring2 = findViewById(R.id.radar_ring2);
        ring3 = findViewById(R.id.radar_ring3);

        if (ring1 == null || ring2 == null || ring3 == null) return;

        isRadarAnimating = true;
        animateRing(ring1, 0);
        animateRing(ring2, 800);
        animateRing(ring3, 1600);
    }

    private void animateRing(final View ring, int delay) {
        radarHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isRadarAnimating || isDestroyed) return;

                ring.setVisibility(View.VISIBLE);
                ring.setScaleX(1.0f);
                ring.setScaleY(1.0f);
                ring.setAlpha(0.6f);

                ring.animate()
                    .scaleX(2.5f)
                    .scaleY(2.5f)
                    .alpha(0.0f)
                    .setDuration(2400)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            animateRing(ring, 0);
                        }
                    })
                    .start();
            }
        }, delay);
    }

    private void stopRadarAnimations() {
        isRadarAnimating = false;
        radarHandler.removeCallbacksAndMessages(null);
        if (ring1 != null) ring1.animate().cancel();
        if (ring2 != null) ring2.animate().cancel();
        if (ring3 != null) ring3.animate().cancel();
    }

    // GLASSMORPHIC DIRECT CHAT BOTTOM SHEET MESSENGER (Option 2)
    private void showChatDialog() {
        if (isFinishing()) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_honeycomb_chat, null);
        dialog.setContentView(sheetView);

        // Find views
        android.widget.ScrollView chatScrollView = sheetView.findViewById(R.id.chat_scroll_view);
        android.widget.LinearLayout chatHistory = sheetView.findViewById(R.id.chat_layout_history);
        android.widget.EditText chatEdtInput = sheetView.findViewById(R.id.chat_edt_input);
        View btnSend = sheetView.findViewById(R.id.chat_btn_send);
        View btnClose = sheetView.findViewById(R.id.chat_btn_close);

        View chip1 = sheetView.findViewById(R.id.chip_chat_preset1);
        View chip2 = sheetView.findViewById(R.id.chip_chat_preset2);
        View chip3 = sheetView.findViewById(R.id.chip_chat_preset3);

        TextView txtName = sheetView.findViewById(R.id.chat_txt_name);
        CircleImageView chatImgAvatar = sheetView.findViewById(R.id.chat_img_avatar);

        // Set matching provider details
        txtName.setText(targetProviderName != null ? targetProviderName : "WorkBee Pro Specialist");

        // Welcome message
        addChatMessage(chatHistory, "Yo! I'm on the way to help you out. Vibe check secured! Tap a chip or message me if you need anything!", false, chatScrollView);

        // Quick chip clicks
        chip1.setOnClickListener(v -> handleUserSend(chatHistory, "🏃‍♂️ Are you near?", chatScrollView));
        chip2.setOnClickListener(v -> handleUserSend(chatHistory, "📍 Arrived outside?", chatScrollView));
        chip3.setOnClickListener(v -> handleUserSend(chatHistory, "✨ Sounds good!", chatScrollView));

        btnSend.setOnClickListener(v -> {
            String msg = chatEdtInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                chatEdtInput.setText("");
                handleUserSend(chatHistory, msg, chatScrollView);
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleUserSend(android.widget.LinearLayout chatHistory, String text, android.widget.ScrollView scrollView) {
        addChatMessage(chatHistory, text, true, scrollView);

        // Simulated AI provider reply delay
        new Handler().postDelayed(() -> {
            if (isDestroyed) return;

            String query = text.toLowerCase();
            if (query.contains("near") || query.contains("where")) {
                addChatMessage(chatHistory, "Yes, absolute speedrun! 🏃‍♂️ Just passed the Blossom Tech Park. ETA 4 mins max!", false, scrollView);
            } else if (query.contains("arrive") || query.contains("outside")) {
                addChatMessage(chatHistory, "Pinging you soon as I pull into the driveway! Vibe check outside! 📍🚗", false, scrollView);
            } else {
                addChatMessage(chatHistory, "Zero worries! Standard elite service incoming! 🐝✨", false, scrollView);
            }
        }, 1200);
    }

    private void addChatMessage(android.widget.LinearLayout chatHistoryContainer, String text, boolean isUser, android.widget.ScrollView scrollView) {
        android.widget.LinearLayout bubbleContainer = new android.widget.LinearLayout(this);
        bubbleContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        bubbleContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bubbleContainer.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);
        bubbleContainer.setPadding(0, 8, 0, 8);

        android.widget.TextView bubble = new android.widget.TextView(this);
        android.widget.LinearLayout.LayoutParams bubbleParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        bubbleParams.setMargins(isUser ? 40 : 0, 0, isUser ? 0 : 40, 0);
        bubble.setLayoutParams(bubbleParams);
        bubble.setText(text);
        bubble.setTextSize(13f);
        bubble.setTextColor(isUser ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
        bubble.setPadding(20, 12, 20, 12);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(android.graphics.Color.parseColor(isUser ? "#FFC107" : "#1E1E1E"));
        
        float r = 24f;
        if (isUser) {
            gd.setCornerRadii(new float[]{r, r, r, r, 0f, 0f, r, r});
        } else {
            gd.setCornerRadii(new float[]{r, r, r, r, r, r, 0f, 0f});
        }
        bubble.setBackground(gd);
        bubbleContainer.addView(bubble);
        chatHistoryContainer.addView(bubbleContainer);

        scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
    }

    // PERFORATED GOLDEN INVOICE TICKET RECEIPT (Option 4)
    private void showInvoiceDialog() {
        if (isFinishing()) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_golden_invoice, null);
        dialog.setContentView(sheetView);

        TextView txtTotal = sheetView.findViewById(R.id.invoice_txt_total);
        TextView txtBaseRate = sheetView.findViewById(R.id.invoice_txt_base_rate);
        TextView txtFee = sheetView.findViewById(R.id.invoice_txt_fee);
        TextView txtOrderId = sheetView.findViewById(R.id.invoice_txt_booking_id);
        View btnClose = sheetView.findViewById(R.id.invoice_btn_close);

        // Customize fields based on booking data
        if (bookingId != null) {
            txtOrderId.setText("ORDER ID: " + bookingId.toUpperCase());
        }

        // Set realistic receipt values
        txtBaseRate.setText("₹35.00 / hr");
        txtFee.setText("₹7.00");
        txtTotal.setText("₹62.00");

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // return to main dashboard
        });

        dialog.show();
    }

    private void setupMapAndRoute() {
        if (googleMap == null) return;

        // Apply premium dark-carbon map style
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.bg_map_dark_style));
            if (!success) {
                android.util.Log.e("UberMatchActivity", "Style parsing failed.");
            }
        } catch (android.content.res.Resources.NotFoundException e) {
            android.util.Log.e("UberMatchActivity", "Can't find style. Error: ", e);
        }

        // Get customer location
        double custLat = LocationHelper.hasRealLocation() ? LocationHelper.getRealLatitude() : 37.7749;
        double custLng = LocationHelper.hasRealLocation() ? LocationHelper.getRealLongitude() : -122.4194;
        customerLatLng = new LatLng(custLat, custLng);

        // Put provider at an offset
        double provLat = custLat + 0.008;
        double provLng = custLng + 0.008;
        providerStartLatLng = new LatLng(provLat, provLng);

        // Add markers
        customerMarker = googleMap.addMarker(new MarkerOptions()
                .position(customerLatLng)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

        providerMarker = googleMap.addMarker(new MarkerOptions()
                .position(providerStartLatLng)
                .title("WorkBee Pro Specialist")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Draw Polyline
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(providerStartLatLng)
                .add(customerLatLng)
                .color(Color.parseColor("#FFC107"))
                .width(8f);
        routePolyline = googleMap.addPolyline(polylineOptions);

        // Zoom camera to include both points
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                try {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(providerStartLatLng);
                    builder.include(customerLatLng);
                    LatLngBounds bounds = builder.build();
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
                } catch (Exception e) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customerLatLng, 14f));
                }
            }
        });
    }

    private LatLng interpolateLatLng(LatLng start, LatLng end, float fraction) {
        double lat = (end.latitude - start.latitude) * fraction + start.latitude;
        double lng = (end.longitude - start.longitude) * fraction + start.longitude;
        return new LatLng(lat, lng);
    }

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

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        pollHandler.removeCallbacksAndMessages(null);
        etaHandler.removeCallbacksAndMessages(null);
        stopRadarAnimations();
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
    }
}
