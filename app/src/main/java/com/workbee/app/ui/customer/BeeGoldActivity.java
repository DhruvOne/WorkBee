package com.workbee.app.ui.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.workbee.app.R;
import com.workbee.app.data.model.User;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.utils.FirebaseHelper;

public class BeeGoldActivity extends BaseActivity {

    private TextView txtGoldStatus, txtToggleButtonLabel;
    private FrameLayout btnToggleGold;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beegold);

        btnBack = findViewById(R.id.btn_back);
        txtGoldStatus = findViewById(R.id.txt_gold_status);
        txtToggleButtonLabel = findViewById(R.id.txt_toggle_button_label);
        btnToggleGold = findViewById(R.id.btn_toggle_gold);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        refreshUI();

        if (btnToggleGold != null) {
            btnToggleGold.setOnClickListener(v -> {
                User user = repository.getCurrentUserProfile();
                if (user != null) {
                    boolean nextStatus = !user.isGoldUser();
                    user.setGoldUser(nextStatus);
                    showProgress("Updating subscription...");
                    repository.updateProfile(user, new FirebaseHelper.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            hideProgress();
                            if (nextStatus) {
                                showToast("Welcome to WorkBee Gold! 👑🐝");
                            } else {
                                showToast("Subscription cancelled.");
                            }
                            refreshUI();
                        }

                        @Override
                        public void onFailure(String message) {
                            hideProgress();
                            showToast("Failed to update: " + message);
                        }
                    });
                }
            });
        }
    }

    private void refreshUI() {
        User user = repository.getCurrentUserProfile();
        if (user != null) {
            if (user.isGoldUser()) {
                txtGoldStatus.setText("Membership: Active 👑");
                txtGoldStatus.setTextColor(android.graphics.Color.parseColor("#FFD700"));
                txtToggleButtonLabel.setText("Cancel Gold Membership");
                btnToggleGold.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#333333")
                ));
                txtToggleButtonLabel.setTextColor(android.graphics.Color.WHITE);
            } else {
                txtGoldStatus.setText("Membership: Inactive");
                txtGoldStatus.setTextColor(android.graphics.Color.parseColor("#F44336"));
                txtToggleButtonLabel.setText("Upgrade to Gold (₹199/mo)");
                btnToggleGold.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FFD700")
                ));
                txtToggleButtonLabel.setTextColor(android.graphics.Color.BLACK);
            }
        }
    }
}
