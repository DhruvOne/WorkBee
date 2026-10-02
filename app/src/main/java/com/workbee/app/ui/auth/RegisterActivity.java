package com.workbee.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.workbee.app.R;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.User;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import com.workbee.app.ui.admin.AdminMainActivity;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.ui.customer.CustomerMainActivity;
import com.workbee.app.ui.provider.ProviderMainActivity;
import com.workbee.app.utils.FirebaseHelper;
import com.workbee.app.utils.LocationHelper;

public class RegisterActivity extends BaseActivity {

    private User pendingRedirectUser = null;

    private EditText edtFullName, edtEmail, edtPhone, edtPassword, edtConfirmPassword;
    private RadioGroup rgRole;
    private RadioButton rbCustomer, rbProvider;
    private LinearLayout layoutProviderFields;

    // Worker Registration OTP views
    private View layoutPasswordFields, layoutRegPhoneVerificationContainer, layoutRegOtpVerification;
    private EditText edtRegOtp;
    private android.widget.Button btnVerifyRegPhone, btnVerifyRegOtp;
    private TextView txtPhoneVerifiedBadge;
    private boolean isPhoneVerified = false;
    
    // Provider specific
    private EditText edtBusinessName, edtExperience, edtRate, edtBio;
    private Spinner spinCategory;

    private View btnRegister;
    private TextView txtLogin, btnTierGuide;
    private ImageButton btnPasswordToggle, btnConfirmPasswordToggle;

    private final String[] categories = {
        "Plumbing", "Electrical", "Cleaning", "Painting", "Carpentry", "AC Technician", "Appliance Repair"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtFullName = findViewById(R.id.edt_fullname);
        edtEmail = findViewById(R.id.edt_email);
        edtPhone = findViewById(R.id.edt_phone);
        edtPassword = findViewById(R.id.edt_password);
        edtConfirmPassword = findViewById(R.id.edt_confirm_password);

        rgRole = findViewById(R.id.rg_role);
        rbCustomer = findViewById(R.id.rb_customer);
        rbProvider = findViewById(R.id.rb_provider);
        layoutProviderFields = findViewById(R.id.layout_provider_fields);

        edtBusinessName = findViewById(R.id.edt_business);
        edtExperience = findViewById(R.id.edt_experience);
        edtRate = findViewById(R.id.edt_rate);
        edtBio = findViewById(R.id.edt_bio);
        spinCategory = findViewById(R.id.spin_category);

        btnRegister = findViewById(R.id.btn_register);
        txtLogin = findViewById(R.id.txt_login);
        btnTierGuide = findViewById(R.id.btn_tier_guide);
        btnPasswordToggle = findViewById(R.id.btn_password_toggle);
        btnConfirmPasswordToggle = findViewById(R.id.btn_confirm_password_toggle);

        layoutPasswordFields = findViewById(R.id.layout_password_fields);
        layoutRegPhoneVerificationContainer = findViewById(R.id.layout_reg_phone_verification_container);
        layoutRegOtpVerification = findViewById(R.id.layout_reg_otp_verification);
        edtRegOtp = findViewById(R.id.edt_reg_otp);
        btnVerifyRegPhone = findViewById(R.id.btn_verify_reg_phone);
        btnVerifyRegOtp = findViewById(R.id.btn_verify_reg_otp);
        txtPhoneVerifiedBadge = findViewById(R.id.txt_phone_verified_badge);

        if (btnVerifyRegPhone != null) {
            btnVerifyRegPhone.setOnClickListener(v -> handleSendRegOtp());
        }
        if (btnVerifyRegOtp != null) {
            btnVerifyRegOtp.setOnClickListener(v -> handleVerifyRegOtp());
        }

        if (btnTierGuide != null) {
            btnTierGuide.setOnClickListener(v -> showWorkerTiersGuide());
        }

        // HTML Dynamic Coloring matching Login layout
        TextView txtTitle = findViewById(R.id.txt_title);
        TextView txtSubtitle = findViewById(R.id.txt_subtitle);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            txtTitle.setText(android.text.Html.fromHtml("Create <font color='#FFC107'>Account</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
            txtSubtitle.setText(android.text.Html.fromHtml("Sign up to stay connected with <font color='#FFC107'>WorkBee</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
            txtLogin.setText(android.text.Html.fromHtml("Already have an account? <font color='#FFC107'><b>Sign In</b></font>", android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            txtTitle.setText(android.text.Html.fromHtml("Create <font color='#FFC107'>Account</font>"));
            txtSubtitle.setText(android.text.Html.fromHtml("Sign up to stay connected with <font color='#FFC107'>WorkBee</font>"));
            txtLogin.setText(android.text.Html.fromHtml("Already have an account? <font color='#FFC107'><b>Sign In</b></font>"));
        }

        // Eye password toggle 1
        final boolean[] isPasswordVisible = {false};
        if (btnPasswordToggle != null) {
            btnPasswordToggle.setOnClickListener(v -> {
                isPasswordVisible[0] = !isPasswordVisible[0];
                if (isPasswordVisible[0]) {
                    edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    btnPasswordToggle.setImageResource(R.drawable.ic_login_eye_closed);
                } else {
                    edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    btnPasswordToggle.setImageResource(R.drawable.ic_login_eye);
                }
                edtPassword.setSelection(edtPassword.getText().length());
            });
        }

        // Eye password toggle 2
        final boolean[] isConfirmPasswordVisible = {false};
        if (btnConfirmPasswordToggle != null) {
            btnConfirmPasswordToggle.setOnClickListener(v -> {
                isConfirmPasswordVisible[0] = !isConfirmPasswordVisible[0];
                if (isConfirmPasswordVisible[0]) {
                    edtConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    btnConfirmPasswordToggle.setImageResource(R.drawable.ic_login_eye_closed);
                } else {
                    edtConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    btnConfirmPasswordToggle.setImageResource(R.drawable.ic_login_eye);
                }
                edtConfirmPassword.setSelection(edtConfirmPassword.getText().length());
            });
        }

        // Setup categories spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinCategory.setAdapter(adapter);

        // Toggle provider fields dynamically
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_provider) {
                layoutProviderFields.setVisibility(View.VISIBLE);
                if (layoutPasswordFields != null) layoutPasswordFields.setVisibility(View.GONE);
                if (layoutRegPhoneVerificationContainer != null) layoutRegPhoneVerificationContainer.setVisibility(View.VISIBLE);
                rbProvider.setTextColor(android.graphics.Color.BLACK);
                rbCustomer.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));
            } else {
                layoutProviderFields.setVisibility(View.GONE);
                if (layoutPasswordFields != null) layoutPasswordFields.setVisibility(View.VISIBLE);
                if (layoutRegPhoneVerificationContainer != null) layoutRegPhoneVerificationContainer.setVisibility(View.GONE);
                rbCustomer.setTextColor(android.graphics.Color.BLACK);
                rbProvider.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));
            }
        });

        txtLogin.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> handleRegister());
    }

    private void handleSendRegOtp() {
        String phone = edtPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone) || phone.length() < 10) {
            edtPhone.setError("Enter a valid 10-digit phone number");
            return;
        }

        showProgress("Sending OTP... 💬");
        repository.sendOtp(phone, new com.workbee.app.utils.FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                hideProgress();
                showToast("OTP sent successfully!");
                if (layoutRegOtpVerification != null) {
                    layoutRegOtpVerification.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                showToast("Failed to send OTP: " + message);
            }
        });
    }

    private void handleVerifyRegOtp() {
        String otp = edtRegOtp.getText().toString().trim();
        if (TextUtils.isEmpty(otp) || otp.length() < 6) {
            edtRegOtp.setError("Enter the 6-digit OTP code");
            return;
        }

        showProgress("Verifying OTP... 📡");
        boolean isValid = repository.verifyOtpOnly(otp);
        hideProgress();
        if (isValid) {
            isPhoneVerified = true;
            showToast("Phone verified successfully!");
            if (txtPhoneVerifiedBadge != null) {
                txtPhoneVerifiedBadge.setVisibility(View.VISIBLE);
            }
            if (layoutRegOtpVerification != null) {
                layoutRegOtpVerification.setVisibility(View.GONE);
            }
        } else {
            showToast("Invalid verification code. Please try again.");
        }
    }

    private void handleRegister() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String role = rbProvider.isChecked() ? "PROVIDER" : "CUSTOMER";

        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Full Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Phone is required");
            return;
        }

        String password = "provider_otp_auth";
        if (role.equals("CUSTOMER")) {
            password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();
            if (password.length() < 6) {
                edtPassword.setError("Password must be at least 6 characters");
                return;
            }
            if (!password.equals(confirmPassword)) {
                edtConfirmPassword.setError("Passwords do not match");
                return;
            }
        } else {
            // PROVIDER (Worker) role requires verified phone number!
            if (!isPhoneVerified) {
                showToast("Please verify your phone number via OTP first.");
                return;
            }
        }

        User user = new User(null, fullName, email, phone, role);
        Provider provider = null;

        if (role.equals("PROVIDER")) {
            String business = edtBusinessName.getText().toString().trim();
            String expStr = edtExperience.getText().toString().trim();
            String rateStr = edtRate.getText().toString().trim();
            String bio = edtBio.getText().toString().trim();
            String category = spinCategory.getSelectedItem().toString();

            if (TextUtils.isEmpty(business)) {
                edtBusinessName.setError("Business Name is required");
                return;
            }
            if (TextUtils.isEmpty(expStr)) {
                edtExperience.setError("Experience is required");
                return;
            }
            if (TextUtils.isEmpty(rateStr)) {
                edtRate.setError("Hourly rate is required");
                return;
            }

            int exp = Integer.parseInt(expStr);
            double rate = Double.parseDouble(rateStr);

            provider = new Provider(null, business, category, exp, rate, bio);
            // Default provider initial location
            user.setAddress("123 WorkBee Boulevard, Tech City");
            user.setLatitude(37.7749);
            user.setLongitude(-122.4194);
        } else {
            // Default customer initial location
            user.setAddress("456 Blossom Lane, Tech City");
            user.setLatitude(37.7749);
            user.setLongitude(-122.4194);
        }

        showProgress("Registering your account...");
        repository.register(user, provider, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(User registeredUser) {
                hideProgress();
                showToast("Registration successful! 🐝");
                
                // Fetch real location before redirecting
                pendingRedirectUser = registeredUser;
                if (LocationHelper.hasPermission(RegisterActivity.this)) {
                    fetchRealLocationThenRedirect(registeredUser);
                } else {
                    LocationHelper.requestPermission(RegisterActivity.this);
                }
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                showToast(message);
            }
        });
    }

    private void redirectUser(User user) {
        Intent intent;
        if (user.getRole().equalsIgnoreCase("ADMIN")) {
            intent = new Intent(RegisterActivity.this, AdminMainActivity.class);
        } else if (user.getRole().equalsIgnoreCase("PROVIDER")) {
            intent = new Intent(RegisterActivity.this, ProviderMainActivity.class);
        } else {
            intent = new Intent(RegisterActivity.this, CustomerMainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showWorkerTiersGuide() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_worker_tiers, null);
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.btn_close_tiers).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void fetchRealLocationThenRedirect(User user) {
        showProgress("Getting your location... 📍");
        LocationHelper locationHelper = new LocationHelper(RegisterActivity.this);
        locationHelper.fetchCurrentLocation(new LocationHelper.LocationCallback2() {
            @Override
            public void onLocationReceived(double lat, double lng, String address) {
                hideProgress();
                // Save real coords into user profile
                user.setLatitude(lat);
                user.setLongitude(lng);
                if (!address.isEmpty()) user.setAddress(address);
                repository.updateProfile(user, new FirebaseHelper.SimpleCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(String message) {}
                });
                redirectUser(user);
            }

            @Override
            public void onLocationFailed(String reason) {
                hideProgress();
                // Navigate anyway even if location fails
                redirectUser(user);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (pendingRedirectUser != null) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchRealLocationThenRedirect(pendingRedirectUser);
                } else {
                    // Permission denied — navigate without location
                    redirectUser(pendingRedirectUser);
                }
            }
        }
    }
}
