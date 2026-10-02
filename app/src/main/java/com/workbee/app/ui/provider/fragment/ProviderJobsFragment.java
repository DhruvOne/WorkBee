package com.workbee.app.ui.provider.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.workbee.app.R;
import com.workbee.app.data.model.Booking;
import com.workbee.app.ui.adapter.ProviderJobsAdapter;
import com.workbee.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class ProviderJobsFragment extends Fragment implements ProviderJobsAdapter.OnJobActionListener {

    private TabLayout tabLayout;
    private RecyclerView recyclerJobs;
    private View layoutEmpty;

    // Analytics and Search views
    private TextView txtActiveCount, txtActiveRevenue, txtCompletedEarnings;
    private EditText edtSearch;
    private ImageButton btnClearSearch;

    private FirebaseHelper firebaseHelper;
    private final List<Booking> allJobs = new ArrayList<>();
    private final List<Booking> filteredJobs = new ArrayList<>();
    private ProviderJobsAdapter adapter;
    private boolean showActiveTab = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider_jobs, container, false);

        firebaseHelper = FirebaseHelper.getInstance(requireContext());

        // Initialize views
        tabLayout = view.findViewById(R.id.tab_layout_jobs);
        recyclerJobs = view.findViewById(R.id.recycler_provider_jobs);
        layoutEmpty = view.findViewById(R.id.layout_empty_jobs);

        // Find stats and search views
        txtActiveCount = view.findViewById(R.id.txt_jobs_active_count);
        txtActiveRevenue = view.findViewById(R.id.txt_jobs_active_revenue);
        txtCompletedEarnings = view.findViewById(R.id.txt_jobs_completed_earnings);
        edtSearch = view.findViewById(R.id.edt_search_jobs);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);

        // Set up search bar TextWatcher
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    btnClearSearch.setVisibility(View.GONE);
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                }
                filterAndDisplayJobs();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            edtSearch.setText("");
            filterAndDisplayJobs();
        });

        recyclerJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProviderJobsAdapter(requireContext(), filteredJobs, this);
        recyclerJobs.setAdapter(adapter);

        setupTabs();
        loadJobs();

        return view;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Active Jobs"));
        tabLayout.addTab(tabLayout.newTab().setText("History"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showActiveTab = (tab.getPosition() == 0);
                filterAndDisplayJobs();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadJobs() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getProviderBookings(uid, new FirebaseHelper.ListCallback<Booking>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                allJobs.clear();
                allJobs.addAll(bookings);
                filterAndDisplayJobs();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Failed to load jobs: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterAndDisplayJobs() {
        filteredJobs.clear();
        String query = edtSearch != null ? edtSearch.getText().toString().trim().toLowerCase() : "";

        int activeCount = 0;
        double activeRevenue = 0;
        double completedEarnings = 0;

        for (Booking b : allJobs) {
            String status = b.getStatus().toUpperCase();
            boolean isActive = status.equals("ACCEPTED") || status.equals("PENDING") 
                    || status.equals("ON_THE_WAY") || status.equals("IN_PROGRESS");

            // Calculate active and completed stats globally over all provider bookings
            if (isActive) {
                activeCount++;
                activeRevenue += b.getTotalPrice();
            } else if (status.equals("COMPLETED")) {
                completedEarnings += b.getTotalPrice() * 0.90; // net provider share is 90%
            }

            // Tab visibility check
            boolean tabMatches = false;
            if (showActiveTab) {
                if (isActive) tabMatches = true;
            } else {
                if (!isActive) tabMatches = true;
            }

            if (tabMatches) {
                if (query.isEmpty()) {
                    filteredJobs.add(b);
                } else {
                    boolean queryMatches = b.getCategory().toLowerCase().contains(query)
                            || b.getCustomerName().toLowerCase().contains(query)
                            || b.getDescription().toLowerCase().contains(query)
                            || b.getAddress().toLowerCase().contains(query);
                    if (queryMatches) {
                        filteredJobs.add(b);
                    }
                }
            }
        }

        // Update analytics views dynamically
        if (txtActiveCount != null) txtActiveCount.setText(String.valueOf(activeCount));
        if (txtActiveRevenue != null) txtActiveRevenue.setText(String.format(java.util.Locale.getDefault(), "₹%.2f", activeRevenue));
        if (txtCompletedEarnings != null) txtCompletedEarnings.setText(String.format(java.util.Locale.getDefault(), "₹%.2f", completedEarnings));

        if (filteredJobs.isEmpty()) {
            recyclerJobs.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerJobs.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onUpdateJobStatus(Booking booking, String nextStatus) {
        firebaseHelper.updateBookingStatus(booking.getBookingId(), nextStatus, new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                if ("COMPLETED".equalsIgnoreCase(nextStatus)) {
                    Toast.makeText(getContext(), "Congratulations! Job completed successfully. 🐝", Toast.LENGTH_LONG).show();
                } else if ("ON_THE_WAY".equalsIgnoreCase(nextStatus)) {
                    Toast.makeText(getContext(), "You are now on the way! Drive safely. 🚗", Toast.LENGTH_SHORT).show();
                } else if ("IN_PROGRESS".equalsIgnoreCase(nextStatus)) {
                    Toast.makeText(getContext(), "Service started! Clean vibes only. 🛠️", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Job status updated to " + nextStatus, Toast.LENGTH_SHORT).show();
                }
                loadJobs(); // Reload to refresh list, update statistics, and shift tabs if completed
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Failed to update job status: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
