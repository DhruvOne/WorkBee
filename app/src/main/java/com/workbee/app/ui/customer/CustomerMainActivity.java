package com.workbee.app.ui.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.workbee.app.R;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.ui.customer.fragment.BookingsFragment;
import com.workbee.app.ui.customer.fragment.HomeFragment;
import com.workbee.app.ui.customer.fragment.ProfileFragment;

public class CustomerMainActivity extends BaseActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main);

        bottomNav = findViewById(R.id.bottom_nav);

        // Load Home Fragment by default
        loadFragment(new HomeFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if (id == R.id.action_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.action_bookings) {
                fragment = new BookingsFragment();
            } else if (id == R.id.action_profile) {
                fragment = new ProfileFragment();
            }

            return loadFragment(fragment);
        });

        // Initialize Notification Bell Tray
        setupNotificationTray();
    }

    private void setupNotificationTray() {
        View btnTray = findViewById(R.id.btn_notification_tray);
        View badge = findViewById(R.id.notification_badge);

        if (btnTray != null) {
            btnTray.setOnClickListener(v -> {
                // Clear active badge visibility
                if (badge != null) {
                    badge.setVisibility(View.GONE);
                }

                // Show slide-down/up alerts drawer bottom sheet
                BottomSheetDialog dialog = new BottomSheetDialog(this);
                View sheetView = getLayoutInflater().inflate(R.layout.dialog_notification_tray, null);
                dialog.setContentView(sheetView);

                View btnClose = sheetView.findViewById(R.id.btn_close_notification_tray);
                View btnClear = sheetView.findViewById(R.id.btn_clear_all_notifications);

                if (btnClose != null) {
                    btnClose.setOnClickListener(v2 -> dialog.dismiss());
                }
                if (btnClear != null) {
                    btnClear.setOnClickListener(v2 -> {
                        Toast.makeText(this, "Alerts ledger swept! 🧹", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }

                dialog.show();
            });
        }
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container_fragment, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}
