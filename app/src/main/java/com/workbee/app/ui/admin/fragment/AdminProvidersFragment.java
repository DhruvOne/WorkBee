package com.workbee.app.ui.admin.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.workbee.app.R;
import com.workbee.app.data.model.Provider;
import com.workbee.app.ui.adapter.AdminVettingAdapter;
import com.workbee.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class AdminProvidersFragment extends Fragment implements AdminVettingAdapter.OnVettingActionListener {

    private TabLayout tabLayout;
    private RecyclerView recyclerProviders;
    private View layoutEmpty;
    private TextView txtEmptyDesc;

    private FirebaseHelper firebaseHelper;
    private final List<Provider> allProviders = new ArrayList<>();
    private final List<Provider> filteredProviders = new ArrayList<>();
    private AdminVettingAdapter adapter;
    private boolean showPendingTab = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_providers, container, false);

        firebaseHelper = FirebaseHelper.getInstance(requireContext());

        tabLayout = view.findViewById(R.id.tab_layout_admin_prov);
        recyclerProviders = view.findViewById(R.id.recycler_admin_providers);
        layoutEmpty = view.findViewById(R.id.layout_admin_prov_empty);
        txtEmptyDesc = view.findViewById(R.id.txt_admin_prov_empty_desc);

        recyclerProviders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminVettingAdapter(requireContext(), filteredProviders, this);
        recyclerProviders.setAdapter(adapter);

        setupTabs();
        loadProviders();

        return view;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Pending Approvals"));
        tabLayout.addTab(tabLayout.newTab().setText("Active Partners"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPendingTab = (tab.getPosition() == 0);
                filterAndDisplayProviders();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadProviders() {
        firebaseHelper.getAllProviders(new FirebaseHelper.ListCallback<Provider>() {
            @Override
            public void onSuccess(List<Provider> list) {
                allProviders.clear();
                allProviders.addAll(list);
                filterAndDisplayProviders();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Failed to load providers: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterAndDisplayProviders() {
        filteredProviders.clear();
        for (Provider p : allProviders) {
            if (showPendingTab) {
                if (!p.isApproved()) {
                    filteredProviders.add(p);
                }
            } else {
                if (p.isApproved()) {
                    filteredProviders.add(p);
                }
            }
        }

        if (filteredProviders.isEmpty()) {
            recyclerProviders.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            if (showPendingTab) {
                txtEmptyDesc.setText("No providers are currently awaiting approval.");
            } else {
                txtEmptyDesc.setText("No active provider partners registered on the platform.");
            }
        } else {
            recyclerProviders.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onApprove(Provider provider) {
        provider.setApproved(true);
        firebaseHelper.updateProviderDetails(provider, new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), provider.getBusinessName() + " has been approved!", Toast.LENGTH_LONG).show();
                loadProviders(); // Refresh vetting queue
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Approval failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSuspend(Provider provider) {
        provider.setApproved(false);
        firebaseHelper.updateProviderDetails(provider, new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                String verb = showPendingTab ? "declined" : "suspended";
                Toast.makeText(getContext(), provider.getBusinessName() + " was successfully " + verb + ".", Toast.LENGTH_LONG).show();
                loadProviders(); // Refresh list
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Operation failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
