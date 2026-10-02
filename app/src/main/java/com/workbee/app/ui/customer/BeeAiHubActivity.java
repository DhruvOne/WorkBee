package com.workbee.app.ui.customer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.workbee.app.R;
import com.workbee.app.data.model.Booking;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.utils.FirebaseHelper;
import com.workbee.app.utils.LocationHelper;

public class BeeAiHubActivity extends BaseActivity {

    private ImageButton btnBack;
    private EditText edtInput;
    private View btnSend;

    private View btnRebook1, btnRebook2, btnRebook3;

    // Interactive camera scanner views
    private View cardInteractiveScanner;
    private View btnLaunchScanner;
    private View layoutScannerOverlay;
    private TextView txtScannerLog;
    private TextView txtScannerCountdown;
    private View btnCloseScanner;

    private static final int CAMERA_REQUEST_CODE = 2002;
    private Handler scannerHandler = new Handler();
    private boolean isScanning = false;
    private int scannerStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bee_ai_hub);

        btnBack = findViewById(R.id.hub_btn_back);
        edtInput = findViewById(R.id.hub_edt_input);
        btnSend = findViewById(R.id.hub_btn_send);

        btnRebook1 = findViewById(R.id.log1_btn_rebook);
        btnRebook2 = findViewById(R.id.log2_btn_rebook);
        btnRebook3 = findViewById(R.id.log3_btn_rebook);

        // Map scanner views
        cardInteractiveScanner = findViewById(R.id.card_interactive_scanner);
        btnLaunchScanner = findViewById(R.id.btn_launch_scanner);
        layoutScannerOverlay = findViewById(R.id.layout_scanner_overlay);
        txtScannerLog = findViewById(R.id.txt_scanner_log);
        txtScannerCountdown = findViewById(R.id.txt_scanner_countdown);
        btnCloseScanner = findViewById(R.id.btn_close_scanner);

        btnBack.setOnClickListener(v -> finish());

        // Quick Re-Booking Redirects
        if (btnRebook1 != null) btnRebook1.setOnClickListener(v -> startRebookDispatch("Plumbing"));
        if (btnRebook2 != null) btnRebook2.setOnClickListener(v -> startRebookDispatch("Cleaning"));
        if (btnRebook3 != null) btnRebook3.setOnClickListener(v -> startRebookDispatch("Watering Plants"));

        btnSend.setOnClickListener(v -> {
            String query = edtInput.getText().toString().trim();
            if (!query.isEmpty()) {
                edtInput.setText("");
                handleAiQuery(query);
            }
        });

        // Setup Scanner clicks
        if (btnLaunchScanner != null) {
            btnLaunchScanner.setOnClickListener(v -> requestCameraPermissionAndStart());
        }
        if (btnCloseScanner != null) {
            btnCloseScanner.setOnClickListener(v -> stopScanner());
        }
    }

    private void startRebookDispatch(String categoryName) {
        Toast.makeText(this, "AI Diagnostic match active: booking " + categoryName + "! ✨", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("category_name", categoryName);
        startActivity(intent);
    }

    private void handleAiQuery(String text) {
        showProgress("BeeAI is analyzing diagnostic query...");

        new Handler().postDelayed(() -> {
            hideProgress();
            String query = text.toLowerCase();

            if (query.contains("leak") || query.contains("pipe") || query.contains("burst")) {
                Toast.makeText(this, "BeeAI: High water pressure pipe burst confirmed on scan 🚰. Requires master plumber.", Toast.LENGTH_LONG).show();
            } else if (query.contains("clean") || query.contains("maid") || query.contains("room")) {
                Toast.makeText(this, "BeeAI: Messy bedroom scan checked 🧹. Recommended service length is 2 hours.", Toast.LENGTH_LONG).show();
            } else if (query.contains("yard") || query.contains("garden") || query.contains("grass")) {
                Toast.makeText(this, "BeeAI: Jungle grass overgrown lawn height (9.5 inches) 🪴. Est: $45 total.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "BeeAI: Scanning databases for '" + text + "' parameters... Vibe check verified!", Toast.LENGTH_LONG).show();
            }
        }, 1000);
    }

    // ── BeeAI Camera Scanner Diagnostics Implementation ──────────────────────

    private void requestCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            launchScannerViewfinder();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchScannerViewfinder();
            } else {
                Toast.makeText(this, "Simulating scanner using offline camera parameters...", Toast.LENGTH_SHORT).show();
                launchScannerViewfinder();
            }
        }
    }

    private void launchScannerViewfinder() {
        if (layoutScannerOverlay != null) {
            layoutScannerOverlay.setVisibility(View.VISIBLE);
        }
        isScanning = true;
        animateLaserLine();
        runScannerDiagnosticsSequence();
    }

    private void stopScanner() {
        isScanning = false;
        if (layoutScannerOverlay != null) {
            layoutScannerOverlay.setVisibility(View.GONE);
        }
        scannerHandler.removeCallbacksAndMessages(null);
        View laser = findViewById(R.id.scanner_laser_line);
        if (laser != null) {
            laser.animate().cancel();
        }
    }

    private void animateLaserLine() {
        if (!isScanning) return;
        
        View laser = findViewById(R.id.scanner_laser_line);
        View viewfinder = findViewById(R.id.scanner_viewfinder);
        if (laser == null || viewfinder == null) return;
        
        int startY = viewfinder.getTop();
        int height = viewfinder.getHeight();
        if (height == 0) height = 500; // fallback
        
        laser.setTranslationY(startY);
        laser.animate()
            .translationY(startY + height)
            .setDuration(1500)
            .withEndAction(() -> {
                if (!isScanning) return;
                laser.animate()
                    .translationY(startY)
                    .setDuration(1500)
                    .withEndAction(this::animateLaserLine)
                    .start();
            })
            .start();
    }

    private void runScannerDiagnosticsSequence() {
        scannerStep = 5;
        scannerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isScanning) return;
                
                if (scannerStep > 0) {
                    if (txtScannerCountdown != null) {
                        txtScannerCountdown.setText("Securing locking bounds in " + scannerStep + "s...");
                    }
                    
                    if (txtScannerLog != null) {
                        if (scannerStep == 5) {
                            txtScannerLog.setText("CONNECTING TO BEEAI CLOUD SATELLITE... 📡");
                        } else if (scannerStep == 4) {
                            txtScannerLog.setText("CALIBRATING DIALECTRIC CAMERA SENSORS... 📷");
                        } else if (scannerStep == 3) {
                            txtScannerLog.setText("LOCKING BOUNDING BOXES ON CLOGGED FLOW DRAINAGE... 🔍");
                        } else if (scannerStep == 2) {
                            txtScannerLog.setText("ANALYSER DETECTING: SEDIMENT OBSTRUCTION IN INTERIOR COUPLING... 🚨");
                        } else if (scannerStep == 1) {
                            txtScannerLog.setText("MATCHING LOCAL SPECIALISTS GRID VECTORS... ⚡");
                        }
                    }
                    
                    scannerStep--;
                    scannerHandler.postDelayed(this, 1000);
                } else {
                    if (txtScannerCountdown != null) {
                        txtScannerCountdown.setText("Scan Complete! Parsing recommendations...");
                    }
                    if (txtScannerLog != null) {
                        txtScannerLog.setText("DIAGNOSTIC MATCH SECURED! 💯");
                    }
                    
                    scannerHandler.postDelayed(() -> showScanRecommendationDialog(), 800);
                }
            }
        }, 1000);
    }

    private void showScanRecommendationDialog() {
        stopScanner();
        
        if (isFinishing()) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_bee_ai_recommendation, null);
        dialog.setContentView(sheetView);

        View btnClose = sheetView.findViewById(R.id.rec_btn_close);
        View btnDispatch = sheetView.findViewById(R.id.rec_btn_dispatch);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnDispatch != null) {
            btnDispatch.setOnClickListener(v -> {
                dialog.dismiss();
                dispatchMockBooking();
            });
        }

        dialog.show();
    }

    private void dispatchMockBooking() {
        showProgress("Creating live diagnostic dispatch...");
        
        Booking newBooking = new Booking();
        String uniqueId = "diag_" + System.currentTimeMillis();
        newBooking.setBookingId(uniqueId);
        newBooking.setCustomerName("You (Via BeeAI Scan)");
        String custId = FirebaseHelper.getInstance(this).getCurrentUserId();
        newBooking.setCustomerId(custId != null ? custId : "mock_customer_id");
        newBooking.setProviderId("diag_pro_1");
        newBooking.setProviderName("Elena's Deep Cleaning");
        newBooking.setCategory("Plumbing");
        newBooking.setStatus("ACCEPTED");
        newBooking.setDate("Right Now");
        newBooking.setTimeSlot("Right Now");
        newBooking.setAddress(LocationHelper.hasRealLocation() ? LocationHelper.getRealAddress() : "Mock Diagnostic Lab 4B");
        newBooking.setTotalPrice(62.0);

        FirebaseHelper.getInstance(this).createBooking(newBooking, new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                hideProgress();
                Toast.makeText(BeeAiHubActivity.this, "BeeAI Dispatch success! Redirecting to live tracking... 🚀", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(BeeAiHubActivity.this, UberMatchActivity.class);
                intent.putExtra("booking_id", uniqueId);
                intent.putExtra("provider_name", "Elena's Deep Cleaning");
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                Toast.makeText(BeeAiHubActivity.this, "Dispatch failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        stopScanner();
        super.onDestroy();
    }
}
