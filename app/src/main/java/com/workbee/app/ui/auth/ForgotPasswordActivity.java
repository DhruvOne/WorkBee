package com.workbee.app.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.workbee.app.R;
import com.workbee.app.ui.common.BaseActivity;

public class ForgotPasswordActivity extends BaseActivity {

    private EditText edtEmail;
    private Button btnReset;
    private TextView txtBackLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edtEmail = findViewById(R.id.edt_email);
        btnReset = findViewById(R.id.btn_reset);
        txtBackLogin = findViewById(R.id.txt_back_login);

        btnReset.setOnClickListener(v -> handleReset());
        txtBackLogin.setOnClickListener(v -> finish());
    }

    private void handleReset() {
        String email = edtEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Email is required");
            return;
        }

        showProgress("Sending reset link...");
        if (repository.isMockMode()) {
            edtEmail.postDelayed(() -> {
                hideProgress();
                showToast("Password reset link sent to " + email);
                finish();
            }, 1000);
        } else {
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> {
                        hideProgress();
                        showToast("Password reset link sent!");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        hideProgress();
                        showToast("Failed to send reset email: " + e.getMessage());
                    });
            } catch (Exception e) {
                hideProgress();
                showToast(e.getMessage());
            }
        }
    }
}
