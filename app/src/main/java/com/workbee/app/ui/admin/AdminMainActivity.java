package com.workbee.app.ui.admin;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.workbee.app.R;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.ui.customer.fragment.ProfileFragment;
import com.workbee.app.ui.admin.fragment.AdminDashboardFragment;
import com.workbee.app.ui.admin.fragment.AdminProvidersFragment;
import com.workbee.app.ui.admin.fragment.AdminCategoriesFragment;

public class AdminMainActivity extends BaseActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        bottomNav = findViewById(R.id.admin_bottom_nav);

        // Load default dashboard fragment on start
        loadFragment(new AdminDashboardFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            
            if (id == R.id.action_admin_dashboard) {
                fragment = new AdminDashboardFragment();
            } else if (id == R.id.action_admin_providers) {
                fragment = new AdminProvidersFragment();
            } else if (id == R.id.action_admin_categories) {
                fragment = new AdminCategoriesFragment();
            } else if (id == R.id.action_admin_profile) {
                fragment = new ProfileFragment();
            }

            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container_admin_fragment, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}
