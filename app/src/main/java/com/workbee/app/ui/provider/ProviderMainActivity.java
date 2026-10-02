package com.workbee.app.ui.provider;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.workbee.app.R;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.ui.provider.fragment.ProviderDashboardFragment;
import com.workbee.app.ui.provider.fragment.ProviderJobsFragment;
import com.workbee.app.ui.provider.fragment.ProviderProfileFragment;

public class ProviderMainActivity extends BaseActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main); // Reuses the container layout

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.provider_bottom_menu);

        loadFragment(new ProviderDashboardFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if (id == R.id.action_provider_dashboard) {
                fragment = new ProviderDashboardFragment();
            } else if (id == R.id.action_provider_jobs) {
                fragment = new ProviderJobsFragment();
            } else if (id == R.id.action_provider_profile) {
                fragment = new ProviderProfileFragment();
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
