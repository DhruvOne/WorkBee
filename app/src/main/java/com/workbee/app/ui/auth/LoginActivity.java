package com.workbee.app.ui.auth;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.workbee.app.R;
import com.workbee.app.data.model.User;
import com.workbee.app.ui.admin.AdminMainActivity;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.ui.customer.CustomerMainActivity;
import com.workbee.app.ui.provider.ProviderMainActivity;
import com.workbee.app.utils.FirebaseHelper;
import com.workbee.app.utils.LocationHelper;

public class LoginActivity extends BaseActivity {

    private EditText edtEmail, edtPassword;
    private View btnLogin;
    private View btnGoogle;
    private TextView txtForgotPwd, txtRegister;

    // Worker OTP views
    private View layoutEmailLoginFields, layoutWorkerOtpFields, layoutOtpVerification, btnVerifyOtp;
    private EditText edtWorkerPhone, edtOtpCode;
    private android.widget.Button btnSendOtp;
    private TextView txtOtpTimer;
    private View btnChangeLanguage;
    private android.os.CountDownTimer resendTimer = null;

    // Hold logged-in user while we wait for location permission result
    private User pendingRedirectUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Completely disable Autofill for the entire window/decor view programmatically to prevent any auto-population
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }

        edtEmail    = findViewById(R.id.edt_login_field_usr);
        edtPassword = findViewById(R.id.edt_login_field_pwd);
        btnLogin    = findViewById(R.id.btn_login);
        btnGoogle   = findViewById(R.id.btn_google);
        txtForgotPwd = findViewById(R.id.txt_forgot_pwd);
        txtRegister  = findViewById(R.id.txt_register);

        // Worker OTP View bindings
        layoutEmailLoginFields = findViewById(R.id.layout_email_login_fields);
        layoutWorkerOtpFields  = findViewById(R.id.layout_worker_otp_fields);
        edtWorkerPhone         = findViewById(R.id.edt_worker_phone);
        btnSendOtp             = findViewById(R.id.btn_send_otp);
        layoutOtpVerification  = findViewById(R.id.layout_otp_verification);
        edtOtpCode             = findViewById(R.id.edt_otp_code);
        btnVerifyOtp           = findViewById(R.id.btn_verify_otp);
        txtOtpTimer            = findViewById(R.id.txt_otp_timer);
        btnChangeLanguage      = findViewById(R.id.btn_change_language);

        if (btnChangeLanguage != null) {
            btnChangeLanguage.setOnClickListener(v -> showLanguageSelectionDialog());
        }
        if (btnSendOtp != null) {
            btnSendOtp.setOnClickListener(v -> handleSendOtp());
        }
        if (btnVerifyOtp != null) {
            btnVerifyOtp.setOnClickListener(v -> handleVerifyOtp());
        }

        // Clear pre-filled demo credentials — user must type their own
        edtEmail.setText("");
        edtPassword.setText("");
        edtEmail.setHint("Enter your email");
        edtPassword.setHint("Enter your password");

        // Programmatically disable Autofill to prevent the system from auto-populating credentials
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            edtEmail.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            edtPassword.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }

        // Post a delayed runnable to ensure the fields are cleared even if Autofill triggers asynchronously
        for (int delay : new int[]{100, 300, 600, 1200}) {
            edtEmail.postDelayed(() -> {
                edtEmail.setText("");
                edtPassword.setText("");
            }, delay);
        }

        // Role Segmented Switcher — no longer auto-fills demo email
        android.widget.RadioGroup rgLoginRole    = findViewById(R.id.rg_login_role);
        android.widget.RadioButton rbLoginCustomer = findViewById(R.id.rb_login_customer);
        android.widget.RadioButton rbLoginProvider = findViewById(R.id.rb_login_provider);

        rgLoginRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_login_provider) {
                rbLoginProvider.setTextColor(android.graphics.Color.BLACK);
                rbLoginCustomer.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));
                if (layoutEmailLoginFields != null) layoutEmailLoginFields.setVisibility(View.GONE);
                if (layoutWorkerOtpFields != null) layoutWorkerOtpFields.setVisibility(View.VISIBLE);
                if (txtForgotPwd != null) txtForgotPwd.setVisibility(View.GONE);
            } else {
                rbLoginCustomer.setTextColor(android.graphics.Color.BLACK);
                rbLoginProvider.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));
                if (layoutEmailLoginFields != null) layoutEmailLoginFields.setVisibility(View.VISIBLE);
                if (layoutWorkerOtpFields != null) layoutWorkerOtpFields.setVisibility(View.GONE);
                if (txtForgotPwd != null) txtForgotPwd.setVisibility(View.VISIBLE);
            }
        });

        // HTML Dynamic Styling
        TextView txtTitle    = findViewById(R.id.txt_title);
        TextView txtSubtitle = findViewById(R.id.txt_subtitle);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            txtTitle.setText(android.text.Html.fromHtml("Welcome <font color='#FFC107'>Back</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
            txtSubtitle.setText(android.text.Html.fromHtml("Sign in to stay connected with <font color='#FFC107'>WorkBee</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
            txtRegister.setText(android.text.Html.fromHtml("Don't have an account? <font color='#FFC107'><b>Register</b></font>", android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            txtTitle.setText(android.text.Html.fromHtml("Welcome <font color='#FFC107'>Back</font>"));
            txtSubtitle.setText(android.text.Html.fromHtml("Sign in to stay connected with <font color='#FFC107'>WorkBee</font>"));
            txtRegister.setText(android.text.Html.fromHtml("Don't have an account? <font color='#FFC107'><b>Register</b></font>"));
        }

        // Password Eye Toggle
        ImageButton btnPasswordToggle = findViewById(R.id.btn_password_toggle);
        final boolean[] isPasswordVisible = {false};
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

        btnLogin.setOnClickListener(v -> handleLogin());
        txtRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        txtForgotPwd.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        // Google Login — now disabled (shows message instead of demo login)
        btnGoogle.setOnClickListener(v ->
            Toast.makeText(this, "Google Sign-In requires a real Firebase project. Please use email/password.", Toast.LENGTH_LONG).show()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Aggressively and repeatedly clear fields to defeat any delayed system/browser autofill attempts
        for (int delay : new int[]{50, 150, 300, 600, 1000, 1500}) {
            if (edtEmail != null) {
                edtEmail.postDelayed(() -> {
                    edtEmail.setText("");
                    edtPassword.setText("");
                }, delay);
            }
        }
    }

    private void handleLogin() {
        String email    = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Password is required");
            return;
        }

        showProgress("Signing in...");
        repository.login(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                hideProgress();
                if (!user.isActive()) {
                    showToast("Your account is currently disabled by Admin.");
                    repository.logout();
                    return;
                }
                showToast("Welcome, " + user.getFullName() + "! 🐝");

                // ── Fetch real GPS location before navigating ──
                pendingRedirectUser = user;
                if (LocationHelper.hasPermission(LoginActivity.this)) {
                    fetchRealLocationThenRedirect(user);
                } else {
                    // Request permission — redirect happens in onRequestPermissionsResult
                    LocationHelper.requestPermission(LoginActivity.this);
                }
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                showToast(message);
            }
        });
    }

    /**
     * Fetch real GPS location, update user profile with it, then navigate.
     */
    private void fetchRealLocationThenRedirect(User user) {
        showProgress("Getting your location... 📍");
        LocationHelper locationHelper = new LocationHelper(LoginActivity.this);
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

    private void redirectUser(User user) {
        pendingRedirectUser = null;
        Intent intent;
        if (user.getRole().equalsIgnoreCase("ADMIN")) {
            intent = new Intent(LoginActivity.this, AdminMainActivity.class);
        } else if (user.getRole().equalsIgnoreCase("PROVIDER")) {
            intent = new Intent(LoginActivity.this, ProviderMainActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, CustomerMainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void showLanguageSelectionDialog() {
        String[] languages = {"English", "हिन्दी (Hindi)", "Español (Spanish)"};
        final String[] codes = {"en", "hi", "es"};
        
        int currentSelection = 0;
        String currentLang = com.workbee.app.utils.LanguageHelper.getLocalePreference(this);
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) {
                currentSelection = i;
                break;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Choose Language / भाषा चुनें / Seleccione el idioma")
            .setSingleChoiceItems(languages, currentSelection, (dialog, which) -> {
                com.workbee.app.utils.LanguageHelper.setLocalePreference(LoginActivity.this, codes[which]);
                dialog.dismiss();
                showToast("Language updated! 🐝");
                recreate();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void handleSendOtp() {
        String phone = edtWorkerPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone) || phone.length() < 10) {
            edtWorkerPhone.setError("Enter a valid 10-digit phone number");
            return;
        }

        showProgress("Sending OTP... 💬");
        repository.sendOtp(phone, new com.workbee.app.utils.FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                hideProgress();
                showToast("OTP sent successfully!");
                if (layoutOtpVerification != null) {
                    layoutOtpVerification.setVisibility(View.VISIBLE);
                }
                startOtpTimer();
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                showToast("Failed to send OTP: " + message);
            }
        });
    }

    private void startOtpTimer() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        btnSendOtp.setEnabled(false);
        resendTimer = new android.os.CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (txtOtpTimer != null) {
                    txtOtpTimer.setText(String.format(java.util.Locale.getDefault(), "Resend OTP in %ds", millisUntilFinished / 1000));
                }
            }

            @Override
            public void onFinish() {
                btnSendOtp.setEnabled(true);
                if (txtOtpTimer != null) {
                    txtOtpTimer.setText("You can resend OTP now.");
                }
            }
        }.start();
    }

    private void handleVerifyOtp() {
        String phone = edtWorkerPhone.getText().toString().trim();
        String otp = edtOtpCode.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            edtWorkerPhone.setError("Phone number is required");
            return;
        }
        if (TextUtils.isEmpty(otp) || otp.length() < 6) {
            edtOtpCode.setError("Enter the 6-digit OTP code");
            return;
        }

        showProgress("Verifying OTP... 📡");
        repository.loginWithPhone(phone, otp, new com.workbee.app.utils.FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                hideProgress();
                if (!user.isActive()) {
                    showToast("Your account is currently disabled by Admin.");
                    repository.logout();
                    return;
                }
                showToast("Welcome back, " + user.getFullName() + "! 🐝");
                
                pendingRedirectUser = user;
                if (com.workbee.app.utils.LocationHelper.hasPermission(LoginActivity.this)) {
                    fetchRealLocationThenRedirect(user);
                } else {
                    com.workbee.app.utils.LocationHelper.requestPermission(LoginActivity.this);
                }
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                showToast(message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        super.onDestroy();
    }
}
