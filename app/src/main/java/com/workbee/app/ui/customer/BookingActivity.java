package com.workbee.app.ui.customer;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import androidx.annotation.NonNull;
import com.workbee.app.R;
import com.workbee.app.data.model.Booking;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.User;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.utils.FirebaseHelper;
import com.workbee.app.utils.LocationHelper;

import java.util.Locale;

public class BookingActivity extends BaseActivity {

    private ImageButton btnBack;
    private DatePicker datePicker;
    private RadioGroup rgTimeSlots;
    private EditText edtDescription, edtAddress;
    private TextInputLayout layoutAddress;
    private TextView txtBaseRate, txtServiceFee, txtTotalPrice;
    private Button btnConfirm;

    private MaterialCardView cardPayCash, cardPayOnline, cardPayWallet;
    private TextView txtPayCashTitle, txtPayOnlineTitle, txtPayWalletTitle;
    private EditText edtPromoCode;
    private android.widget.Button btnApplyPromo;
    private TextView txtPromoStatus;
    private String selectedPaymentMethod = "CASH";

    private double calculatedTotal;
    private String appliedPromoCode = "";
    private double discountAmount = 0.0;

    // Selected category details
    private String categoryName = "Plumbing";
    private double categoryRate = 25.0;
    private com.workbee.app.data.model.Provider selectedProvider = null;

    // Issue selection widgets
    private RadioGroup rgIssues;
    private RadioButton optIssue1, optIssue2, optIssue3, optIssue4;
    private double selectedIssuePrice = 250.0;
    private String selectedIssueName = "Other";

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        btnBack = findViewById(R.id.btn_back);
        datePicker = findViewById(R.id.date_picker);
        rgTimeSlots = findViewById(R.id.rg_time_slots);
        edtDescription = findViewById(R.id.edt_description);
        edtAddress = findViewById(R.id.edt_address);
        layoutAddress = findViewById(R.id.layout_address);
        if (layoutAddress != null) {
            layoutAddress.setStartIconOnClickListener(v -> fetchCurrentLocationGps());
        }
        txtBaseRate = findViewById(R.id.txt_base_rate);
        txtServiceFee = findViewById(R.id.txt_service_fee);
        txtTotalPrice = findViewById(R.id.txt_total_price);
        btnConfirm = findViewById(R.id.btn_confirm_booking);

        cardPayCash = findViewById(R.id.card_pay_cash);
        cardPayOnline = findViewById(R.id.card_pay_online);
        cardPayWallet = findViewById(R.id.card_pay_wallet);
        txtPayCashTitle = findViewById(R.id.txt_pay_cash_title);
        txtPayOnlineTitle = findViewById(R.id.txt_pay_online_title);
        txtPayWalletTitle = findViewById(R.id.txt_pay_wallet_title);

        edtPromoCode = findViewById(R.id.edt_promo_code);
        btnApplyPromo = findViewById(R.id.btn_apply_promo);
        txtPromoStatus = findViewById(R.id.txt_promo_status);

        if (btnApplyPromo != null) {
            btnApplyPromo.setOnClickListener(v -> {
                String code = edtPromoCode.getText().toString().trim().toUpperCase();
                if ("WELCOME100".equals(code) || "BEEGOLD".equals(code)) {
                    appliedPromoCode = code;
                    calculatePricing();
                    txtPromoStatus.setVisibility(android.view.View.VISIBLE);
                    txtPromoStatus.setText("Coupon " + code + " applied successfully!");
                    txtPromoStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                    showToast("Promo Applied! 🐝");
                } else {
                    appliedPromoCode = "";
                    calculatePricing();
                    txtPromoStatus.setVisibility(android.view.View.VISIBLE);
                    txtPromoStatus.setText("Invalid coupon code.");
                    txtPromoStatus.setTextColor(android.graphics.Color.parseColor("#F44336"));
                }
            });
        }

        rgIssues = findViewById(R.id.rg_issues);
        optIssue1 = findViewById(R.id.issue_option_1);
        optIssue2 = findViewById(R.id.issue_option_2);
        optIssue3 = findViewById(R.id.issue_option_3);
        optIssue4 = findViewById(R.id.issue_option_4);

        // Fetch selected category details
        selectedProvider = (com.workbee.app.data.model.Provider) getIntent().getSerializableExtra("provider_object");
        if (selectedProvider != null) {
            categoryName = selectedProvider.getCategory();
            categoryRate = selectedProvider.getHourlyRate();
        } else {
            categoryName = getIntent().getStringExtra("category_name");
            if (categoryName == null) categoryName = "Plumbing";
            categoryRate = getIntent().getDoubleExtra("category_rate", 25.0);
        }

        // Customize toolbar title
        TextView txtBookingTitle = findViewById(R.id.txt_booking_title);
        if (txtBookingTitle != null) {
            txtBookingTitle.setText("Book " + categoryName);
        }

        // Configure dynamic options
        setupCategoryIssuesOptions();

        btnBack.setOnClickListener(v -> onBackPressed());

        // Setup payment method selection toggles
        cardPayCash.setOnClickListener(v -> {
            selectedPaymentMethod = "CASH";
            updatePaymentSelectionUI();
        });

        cardPayOnline.setOnClickListener(v -> {
            selectedPaymentMethod = "ONLINE";
            updatePaymentSelectionUI();
        });

        if (cardPayWallet != null) {
            cardPayWallet.setOnClickListener(v -> {
                selectedPaymentMethod = "WALLET";
                updatePaymentSelectionUI();
            });
        }

        // Set initial selected state in UI
        updatePaymentSelectionUI();

        // Prepopulate Customer location details if available
        User profile = repository.getCurrentUserProfile();
        if (profile != null && profile.getAddress() != null) {
            edtAddress.setText(profile.getAddress());
        }

        // Initialize Scheduler recommendation
        int initDay = datePicker.getDayOfMonth();
        int initMonth = datePicker.getMonth();
        int initYear = datePicker.getYear();
        updateSchedulerRecommendation(initDay);

        datePicker.init(initYear, initMonth, initDay, (pickerView, year, monthOfYear, dayOfMonth) -> {
            updateSchedulerRecommendation(dayOfMonth);
        });

        findViewById(R.id.btn_apply_scheduler_vibe).setOnClickListener(v -> {
            rgTimeSlots.check(recommendedSlotId);
            com.google.android.material.card.MaterialCardView cardScheduler = findViewById(R.id.card_bee_scheduler);
            if (cardScheduler != null) {
                cardScheduler.setStrokeColor(android.graphics.Color.parseColor("#FFC107"));
                cardScheduler.setStrokeWidth((int) (2.5 * getResources().getDisplayMetrics().density));
            }
            TextView txtVibe = findViewById(R.id.txt_scheduler_vibe_msg);
            if (txtVibe != null) {
                txtVibe.setText("✅ Optimized! 100% matched timeslot applied successfully.");
            }
            showToast("Timeslot optimized! 🐝");
        });

        rgIssues.setOnCheckedChangeListener((group, checkedId) -> {
            updateSelectedIssue(checkedId);
        });

        // Set default checked option trigger
        updateSelectedIssue(rgIssues.getCheckedRadioButtonId());

        btnConfirm.setOnClickListener(v -> handleConfirmBooking());
    }

    private void setupCategoryIssuesOptions() {
        String cat = categoryName.toLowerCase();
        if (cat.contains("plumb")) {
            optIssue1.setText("Pipe Leakage (Est. ₹300.00)");
            optIssue2.setText("Broken Tap (Est. ₹150.00)");
            optIssue3.setText("Water Tank Issue (Est. ₹800.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("elect")) {
            optIssue1.setText("Short Circuit (Est. ₹500.00)");
            optIssue2.setText("Fan/Light Fitting (Est. ₹200.00)");
            optIssue3.setText("Appliance Repair (Est. ₹400.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("clean")) {
            optIssue1.setText("Deep House Cleaning (Est. ₹800.00)");
            optIssue2.setText("Kitchen Sanitization (Est. ₹450.00)");
            optIssue3.setText("Bathroom Wash (Est. ₹350.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("ac_repair") || cat.contains("ac repair") || cat.contains("ac ")) {
            optIssue1.setText("AC Wet Service (Est. ₹350.00)");
            optIssue2.setText("Gas Refilling (Est. ₹1200.00)");
            optIssue3.setText("AC Not Cooling (Est. ₹800.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("appliance") || cat.contains("purifier") || cat.contains("ro/")) {
            optIssue1.setText("Appliance Repair (Est. ₹250.00)");
            optIssue2.setText("RO Filter Replace (Est. ₹400.00)");
            optIssue3.setText("Water Check/TDS (Est. ₹150.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("carpenter")) {
            optIssue1.setText("Furniture Repair (Est. ₹200.00)");
            optIssue2.setText("Door Lock Fitting (Est. ₹150.00)");
            optIssue3.setText("Custom Woodwork (Est. ₹500.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("painter")) {
            optIssue1.setText("Wall Touch-up (Est. ₹250.00)");
            optIssue2.setText("Single Room Paint (Est. ₹1200.00)");
            optIssue3.setText("Wall Texture/Stencils (Est. ₹1800.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("pest")) {
            optIssue1.setText("Bedbug/Cockroach (Est. ₹300.00)");
            optIssue2.setText("Termite Control (Est. ₹900.00)");
            optIssue3.setText("General Fumigation (Est. ₹500.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("garden")) {
            optIssue1.setText("Lawn Mowing (Est. ₹180.00)");
            optIssue2.setText("Watering & Pruning (Est. ₹120.00)");
            optIssue3.setText("Soil/Seed Prep (Est. ₹220.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("wash")) {
            optIssue1.setText("Foam Car Wash (Est. ₹150.00)");
            optIssue2.setText("Bike Detail Wash (Est. ₹90.00)");
            optIssue3.setText("Interior Vacuum (Est. ₹250.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("shift")) {
            optIssue1.setText("Local shifting (Est. ₹1500.00)");
            optIssue2.setText("Packers & Movers (Est. ₹5000.00)");
            optIssue3.setText("Single Sofa Shift (Est. ₹600.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("laundry")) {
            optIssue1.setText("Wash & Iron (Est. ₹100.00)");
            optIssue2.setText("Dry Cleaning (Est. ₹250.00)");
            optIssue3.setText("Blanket Wash (Est. ₹180.00)");
            optIssue4.setText("Other (Describe Below)");
        } else if (cat.contains("computer") || cat.contains("laptop") || cat.contains("cctv")) {
            optIssue1.setText("OS Installation (Est. ₹400.00)");
            optIssue2.setText("CCTV Setup (Est. ₹450.00)");
            optIssue3.setText("Screen/Hardware Repair (Est. ₹1500.00)");
            optIssue4.setText("Other (Describe Below)");
        } else {
            optIssue1.setText("Standard Session (Est. ₹400.00)");
            optIssue2.setText("Outstation Ride (Est. ₹1200.00)");
            optIssue3.setText("Emergency Service (Est. ₹600.00)");
            optIssue4.setText("Other (Describe Below)");
        }
    }

    private void updateSelectedIssue(int checkedId) {
        String cat = categoryName.toLowerCase();
        if (checkedId == R.id.issue_option_1) {
            if (cat.contains("plumb")) {
                selectedIssuePrice = 300.00;
                selectedIssueName = "Pipe Leakage";
            } else if (cat.contains("elect")) {
                selectedIssuePrice = 500.00;
                selectedIssueName = "Short Circuit";
            } else if (cat.contains("clean")) {
                selectedIssuePrice = 800.00;
                selectedIssueName = "Deep House Cleaning";
            } else if (cat.contains("ac_repair") || cat.contains("ac repair") || cat.contains("ac ")) {
                selectedIssuePrice = 350.00;
                selectedIssueName = "AC Wet Service";
            } else if (cat.contains("appliance") || cat.contains("purifier") || cat.contains("ro/")) {
                selectedIssuePrice = 250.00;
                selectedIssueName = "Appliance Repair";
            } else if (cat.contains("carpenter")) {
                selectedIssuePrice = 200.00;
                selectedIssueName = "Furniture Repair";
            } else if (cat.contains("painter")) {
                selectedIssuePrice = 250.00;
                selectedIssueName = "Wall Touch-up";
            } else if (cat.contains("pest")) {
                selectedIssuePrice = 300.00;
                selectedIssueName = "Bedbug/Cockroach Treatment";
            } else if (cat.contains("garden")) {
                selectedIssuePrice = 180.00;
                selectedIssueName = "Lawn Mowing";
            } else if (cat.contains("wash")) {
                selectedIssuePrice = 150.00;
                selectedIssueName = "Foam Car Wash";
            } else if (cat.contains("shift")) {
                selectedIssuePrice = 1500.00;
                selectedIssueName = "Local Shifting";
            } else if (cat.contains("laundry")) {
                selectedIssuePrice = 100.00;
                selectedIssueName = "Wash & Iron";
            } else if (cat.contains("computer") || cat.contains("laptop") || cat.contains("cctv")) {
                selectedIssuePrice = 400.00;
                selectedIssueName = "OS Installation";
            } else {
                selectedIssuePrice = 400.00;
                selectedIssueName = "Standard Session";
            }
            edtDescription.setText(selectedIssueName + " service request.");
            edtDescription.setError(null);
        } else if (checkedId == R.id.issue_option_2) {
            if (cat.contains("plumb")) {
                selectedIssuePrice = 150.00;
                selectedIssueName = "Broken Tap";
            } else if (cat.contains("elect")) {
                selectedIssuePrice = 200.00;
                selectedIssueName = "Fan/Light Fitting";
            } else if (cat.contains("clean")) {
                selectedIssuePrice = 450.00;
                selectedIssueName = "Kitchen Sanitization";
            } else if (cat.contains("ac_repair") || cat.contains("ac repair") || cat.contains("ac ")) {
                selectedIssuePrice = 1200.00;
                selectedIssueName = "Gas Refilling";
            } else if (cat.contains("appliance") || cat.contains("purifier") || cat.contains("ro/")) {
                selectedIssuePrice = 400.00;
                selectedIssueName = "RO Filter Replace";
            } else if (cat.contains("carpenter")) {
                selectedIssuePrice = 150.00;
                selectedIssueName = "Door Lock Fitting";
            } else if (cat.contains("painter")) {
                selectedIssuePrice = 1200.00;
                selectedIssueName = "Single Room Paint";
            } else if (cat.contains("pest")) {
                selectedIssuePrice = 900.00;
                selectedIssueName = "Termite Control";
            } else if (cat.contains("garden")) {
                selectedIssuePrice = 120.00;
                selectedIssueName = "Watering & Pruning";
            } else if (cat.contains("wash")) {
                selectedIssuePrice = 90.00;
                selectedIssueName = "Bike Detail Wash";
            } else if (cat.contains("shift")) {
                selectedIssuePrice = 5000.00;
                selectedIssueName = "Packers & Movers";
            } else if (cat.contains("laundry")) {
                selectedIssuePrice = 250.00;
                selectedIssueName = "Dry Cleaning";
            } else if (cat.contains("computer") || cat.contains("laptop") || cat.contains("cctv")) {
                selectedIssuePrice = 450.00;
                selectedIssueName = "CCTV Setup";
            } else {
                selectedIssuePrice = 1200.00;
                selectedIssueName = "Outstation Ride";
            }
            edtDescription.setText(selectedIssueName + " service request.");
            edtDescription.setError(null);
        } else if (checkedId == R.id.issue_option_3) {
            if (cat.contains("plumb")) {
                selectedIssuePrice = 800.00;
                selectedIssueName = "Water Tank Issue";
            } else if (cat.contains("elect")) {
                selectedIssuePrice = 400.00;
                selectedIssueName = "Appliance Repair";
            } else if (cat.contains("clean")) {
                selectedIssuePrice = 350.00;
                selectedIssueName = "Bathroom Wash";
            } else if (cat.contains("ac_repair") || cat.contains("ac repair") || cat.contains("ac ")) {
                selectedIssuePrice = 800.00;
                selectedIssueName = "AC Leakage Repair";
            } else if (cat.contains("appliance") || cat.contains("purifier") || cat.contains("ro/")) {
                selectedIssuePrice = 150.00;
                selectedIssueName = "Water Check/TDS Calibration";
            } else if (cat.contains("carpenter")) {
                selectedIssuePrice = 500.00;
                selectedIssueName = "Custom Woodwork";
            } else if (cat.contains("painter")) {
                selectedIssuePrice = 1800.00;
                selectedIssueName = "Wall Texture/Stencils";
            } else if (cat.contains("pest")) {
                selectedIssuePrice = 500.00;
                selectedIssueName = "General Fumigation";
            } else if (cat.contains("garden")) {
                selectedIssuePrice = 220.00;
                selectedIssueName = "Soil/Seed Prep";
            } else if (cat.contains("wash")) {
                selectedIssuePrice = 250.00;
                selectedIssueName = "Interior Vacuuming";
            } else if (cat.contains("shift")) {
                selectedIssuePrice = 600.00;
                selectedIssueName = "Single Sofa Shift";
            } else if (cat.contains("laundry")) {
                selectedIssuePrice = 180.00;
                selectedIssueName = "Blanket Wash";
            } else if (cat.contains("computer") || cat.contains("laptop") || cat.contains("cctv")) {
                selectedIssuePrice = 1500.00;
                selectedIssueName = "Screen/Hardware Repair";
            } else {
                selectedIssuePrice = 600.00;
                selectedIssueName = "Emergency Service";
            }
            edtDescription.setText(selectedIssueName + " service request.");
            edtDescription.setError(null);
        } else {
            selectedIssuePrice = 250.00; // default base rate for "Other"
            selectedIssueName = "Other";
            edtDescription.setText("");
            edtDescription.requestFocus();
        }
        calculatePricing();
    }

    private void updatePaymentSelectionUI() {
        int activeStroke = (int) (2 * getResources().getDisplayMetrics().density);
        if (cardPayCash != null) cardPayCash.setStrokeWidth("CASH".equals(selectedPaymentMethod) ? activeStroke : 0);
        if (cardPayOnline != null) cardPayOnline.setStrokeWidth("ONLINE".equals(selectedPaymentMethod) ? activeStroke : 0);
        if (cardPayWallet != null) cardPayWallet.setStrokeWidth("WALLET".equals(selectedPaymentMethod) ? activeStroke : 0);

        if (txtPayCashTitle != null) {
            txtPayCashTitle.setTextColor(ContextCompat.getColor(this, "CASH".equals(selectedPaymentMethod) ? R.color.secondary : R.color.grey_600));
            txtPayCashTitle.setTypeface(null, "CASH".equals(selectedPaymentMethod) ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (txtPayOnlineTitle != null) {
            txtPayOnlineTitle.setTextColor(ContextCompat.getColor(this, "ONLINE".equals(selectedPaymentMethod) ? R.color.secondary : R.color.grey_600));
            txtPayOnlineTitle.setTypeface(null, "ONLINE".equals(selectedPaymentMethod) ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (txtPayWalletTitle != null) {
            txtPayWalletTitle.setTextColor(ContextCompat.getColor(this, "WALLET".equals(selectedPaymentMethod) ? R.color.secondary : R.color.grey_600));
            txtPayWalletTitle.setTypeface(null, "WALLET".equals(selectedPaymentMethod) ? Typeface.BOLD : Typeface.NORMAL);
            
            User profile = repository.getCurrentUserProfile();
            double balance = 500.0;
            if (profile != null) {
                balance = profile.getWalletBalance();
            }
            txtPayWalletTitle.setText(String.format("Wallet (₹%.2f)", balance));
        }
    }

    @SuppressLint("DefaultLocale")
    private void calculatePricing() {
        double basePrice = selectedIssuePrice;
        
        if ("WELCOME100".equals(appliedPromoCode)) {
            discountAmount = Math.min(100.0, basePrice);
        } else if ("BEEGOLD".equals(appliedPromoCode)) {
            discountAmount = basePrice * 0.20;
        } else {
            discountAmount = 0.0;
        }

        double serviceFee = basePrice * 0.15;
        calculatedTotal = basePrice - discountAmount + serviceFee;
        if (calculatedTotal < 0) {
            calculatedTotal = 0;
        }

        txtBaseRate.setText(String.format("₹%.2f (Est.)", basePrice));
        txtServiceFee.setText(String.format("₹%.2f", serviceFee));
        
        android.view.View layoutDiscount = findViewById(R.id.layout_discount);
        if (layoutDiscount != null) {
            if (discountAmount > 0) {
                layoutDiscount.setVisibility(android.view.View.VISIBLE);
                TextView txtDiscountAmount = findViewById(R.id.txt_discount_amount);
                if (txtDiscountAmount != null) {
                    txtDiscountAmount.setText(String.format("-₹%.2f", discountAmount));
                }
            } else {
                layoutDiscount.setVisibility(android.view.View.GONE);
            }
        }

        txtTotalPrice.setText(String.format("₹%.2f", calculatedTotal));
    }

    private void handleConfirmBooking() {
        String description = edtDescription.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();

        if (TextUtils.isEmpty(description)) {
            edtDescription.setError("Brief description of task is required");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            edtAddress.setError("Address is required");
            return;
        }

        // Extract Date
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth() + 1; // Month is 0-indexed
        int year = datePicker.getYear();
        String formattedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month, day);

        // Extract Time Slot
        int checkedId = rgTimeSlots.getCheckedRadioButtonId();
        RadioButton checkedSlot = findViewById(checkedId);
        String timeSlot = checkedSlot != null ? checkedSlot.getText().toString() : "09:00 AM - 11:00 AM";

        User customer = repository.getCurrentUserProfile();
        if (customer == null) {
            showToast("Failed to fetch customer profile. Log in again.");
            return;
        }

        if ("WALLET".equals(selectedPaymentMethod)) {
            if (customer.getWalletBalance() < calculatedTotal) {
                showToast("Insufficient Wallet Balance. Please choose another payment method.");
                return;
            }
        }

        String targetProvId;
        String targetProvName;
        if (selectedProvider != null) {
            targetProvId = selectedProvider.getProviderId();
            targetProvName = selectedProvider.getBusinessName();
        } else {
            targetProvId = getMockProviderIdForCategory(categoryName);
            targetProvName = getMockProviderBusinessNameForCategory(categoryName);
        }

        Booking booking = new Booking(
                null,
                customer.getUserId(),
                customer.getFullName(),
                targetProvId,
                targetProvName,
                categoryName,
                formattedDate,
                timeSlot,
                description,
                calculatedTotal,
                address
        );
        booking.setPaymentMethod(selectedPaymentMethod);
        booking.setPromoCode(appliedPromoCode);
        booking.setDiscountAmount(discountAmount);

        if ("WALLET".equals(selectedPaymentMethod)) {
            double newBalance = customer.getWalletBalance() - calculatedTotal;
            customer.setWalletBalance(newBalance);
            repository.updateProfile(customer, new FirebaseHelper.SimpleCallback() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String message) {}
            });
        }

        showProgress("Scheduling your home service...");
        repository.createBooking(booking, new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                hideProgress();
                showToast("Booking confirmed! Initiating Dispatch Vibe Match... 📡");
                
                android.content.Intent matchIntent = new android.content.Intent(BookingActivity.this, UberMatchActivity.class);
                matchIntent.putExtra("booking_id", booking.getBookingId());
                matchIntent.putExtra("provider_name", targetProvName);
                startActivity(matchIntent);
                
                finish();
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                showToast("Booking failed: " + message);
            }
        });
    }

    private String getMockProviderIdForCategory(String categoryName) {
        if (categoryName == null) return "prov_house_maid";
        String cat = categoryName.toLowerCase();
        if (cat.contains("clean") || cat.contains("maid")) {
            return "prov_cleaner";
        } else if (cat.contains("driver")) {
            return "prov_car_driver";
        } else if (cat.contains("cook")) {
            return "prov_veg_cook";
        } else {
            return "prov_house_maid"; // Elite House Maid Services
        }
    }

    private String getMockProviderBusinessNameForCategory(String categoryName) {
        if (categoryName == null) return "Elite House Maid Services";
        String cat = categoryName.toLowerCase();
        if (cat.contains("clean") || cat.contains("maid")) {
            return "Elena's Deep Cleaning";
        } else if (cat.contains("driver")) {
            return "Sparks Private Drivers";
        } else if (cat.contains("cook")) {
            return "Jack's Pure Veg Kitchen";
        } else {
            return "Elite House Maid Services";
        }
    }

    private int recommendedSlotId = R.id.slot2;

    private void updateSchedulerRecommendation(int day) {
        TextView txtVibe = findViewById(R.id.txt_scheduler_vibe_msg);
        com.google.android.material.button.MaterialButton btnApply = findViewById(R.id.btn_apply_scheduler_vibe);
        if (txtVibe == null || btnApply == null) return;

        // Even day -> Sunshine vibe
        if (day % 2 == 0) {
            txtVibe.setText("☀️ Sunshine vibe predicted! Pro load is low. Recommended slot: 11:00 AM - 01:00 PM.");
            recommendedSlotId = R.id.slot2;
            btnApply.setText("Auto-Apply 11:00 AM Slot ✨");
        } else {
            txtVibe.setText("🌧️ Heavy rain vibe expected in the afternoon. Schedule early morning! Recommended slot: 09:00 AM - 11:00 AM.");
            recommendedSlotId = R.id.slot1;
            btnApply.setText("Auto-Apply 09:00 AM Slot ✨");
        }
    }

    private void fetchCurrentLocationGps() {
        if (LocationHelper.hasPermission(this)) {
            showProgress("Getting your current location... 📍");
            LocationHelper locationHelper = new LocationHelper(this);
            locationHelper.fetchCurrentLocation(new LocationHelper.LocationCallback2() {
                @Override
                public void onLocationReceived(double lat, double lng, String address) {
                    hideProgress();
                    if (address != null && !address.isEmpty()) {
                        edtAddress.setText(address);
                        showToast("Location updated successfully! 📍");
                    } else {
                        showToast("Location resolved, but address was empty.");
                    }
                }

                @Override
                public void onLocationFailed(String reason) {
                    hideProgress();
                    showToast("Failed to fetch location: " + reason);
                }
            });
        } else {
            LocationHelper.requestPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocationGps();
            } else {
                showToast("Location permission is required to fetch current location.");
            }
        }
    }
}
