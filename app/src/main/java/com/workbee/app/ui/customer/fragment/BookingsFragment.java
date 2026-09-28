package com.workbee.app.ui.customer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.workbee.app.data.repository.FirebaseRepository;
import com.workbee.app.ui.adapter.BookingAdapter;
import com.workbee.app.ui.customer.ReviewsActivity;
import com.workbee.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class BookingsFragment extends Fragment {

    private FirebaseRepository repository;
    private TabLayout tabLayout;
    private RecyclerView recycler;
    private LinearLayout layoutEmpty;
    private TextView txtEmptyDesc;
    
    private BookingAdapter adapter;
    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> filteredBookings = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookings, container, false);
        repository = new FirebaseRepository(requireContext());

        tabLayout = view.findViewById(R.id.tab_layout);
        recycler = view.findViewById(R.id.recycler_bookings);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        txtEmptyDesc = view.findViewById(R.id.txt_empty_desc);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterAndDisplay(tabLayout.getSelectedTabPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBookings();
    }

    private void loadBookings() {
        String uid = repository.getCurrentUserId();
        if (uid == null) return;

        repository.getCustomerBookings(uid, new FirebaseHelper.ListCallback<Booking>() {
            @Override
            public void onSuccess(List<Booking> list) {
                allBookings.clear();
                allBookings.addAll(list);
                filterAndDisplay(tabLayout.getSelectedTabPosition());
            }

            @Override
            public void onFailure(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void filterAndDisplay(int tabIndex) {
        filteredBookings.clear();
        for (Booking b : allBookings) {
            String status = b.getStatus().toUpperCase();
            if (tabIndex == 0) { // Active
                if (status.equals("PENDING") || status.equals("ACCEPTED") || 
                    status.equals("ON_THE_WAY") || status.equals("IN_PROGRESS")) {
                    filteredBookings.add(b);
                }
            } else if (tabIndex == 1) { // Completed
                if (status.equals("COMPLETED")) {
                    filteredBookings.add(b);
                }
            } else { // Cancelled
                if (status.equals("CANCELLED") || status.equals("REJECTED")) {
                    filteredBookings.add(b);
                }
            }
        }

        if (filteredBookings.isEmpty()) {
            recycler.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            if (tabIndex == 0) {
                txtEmptyDesc.setText("You have no ongoing or active booking requests.");
            } else if (tabIndex == 1) {
                txtEmptyDesc.setText("Your completed home services will be listed here.");
            } else {
                txtEmptyDesc.setText("You have no cancelled or rejected service requests.");
            }
        } else {
            recycler.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);

            adapter = new BookingAdapter(getContext(), filteredBookings, new BookingAdapter.OnBookingActionListener() {
                @Override
                public void onRateClick(Booking booking) {
                    Intent intent = new Intent(getContext(), ReviewsActivity.class);
                    intent.putExtra("booking_object", booking);
                    startActivity(intent);
                }

                @Override
                public void onCancelClick(Booking booking) {
                    // Quick cancel request
                    repository.updateBookingStatus(booking.getBookingId(), "CANCELLED", new FirebaseHelper.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Booking cancelled", Toast.LENGTH_SHORT).show();
                            loadBookings();
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            recycler.setAdapter(adapter);
        }
    }
}
