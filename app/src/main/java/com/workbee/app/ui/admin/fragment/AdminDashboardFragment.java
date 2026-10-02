package com.workbee.app.ui.admin.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;
import com.workbee.app.data.model.Booking;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.User;
import com.workbee.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminDashboardFragment extends Fragment {

    private TextView txtTotalUsers, txtTotalProviders, txtTotalBookings, txtRevenue;
    private RecyclerView recyclerRecent;
    private TextView txtEmpty;

    private FirebaseHelper firebaseHelper;
    private final List<Booking> recentBookingsList = new ArrayList<>();
    private RecentActivityAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        firebaseHelper = FirebaseHelper.getInstance(requireContext());

        txtTotalUsers = view.findViewById(R.id.txt_admin_total_users);
        txtTotalProviders = view.findViewById(R.id.txt_admin_total_providers);
        txtTotalBookings = view.findViewById(R.id.txt_admin_total_bookings);
        txtRevenue = view.findViewById(R.id.txt_admin_revenue);
        recyclerRecent = view.findViewById(R.id.recycler_admin_recent);
        txtEmpty = view.findViewById(R.id.txt_admin_recent_empty);

        recyclerRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecentActivityAdapter(requireContext(), recentBookingsList);
        recyclerRecent.setAdapter(adapter);

        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabAdminAi = view.findViewById(R.id.fab_admin_ai);
        fabAdminAi.setOnClickListener(v -> showAdminAiDialog());

        loadPlatformStats();

        return view;
    }

    private void loadPlatformStats() {
        // 1. Fetch Users & Providers
        firebaseHelper.getAllUsers(new FirebaseHelper.ListCallback<User>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(List<User> list) {
                int totalMembers = list.size();
                txtTotalUsers.setText(String.valueOf(totalMembers));
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error fetching users: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        firebaseHelper.getAllProviders(new FirebaseHelper.ListCallback<Provider>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(List<Provider> list) {
                int activeProviders = 0;
                for (Provider p : list) {
                    if (p.isApproved()) {
                        activeProviders++;
                    }
                }
                txtTotalProviders.setText(String.valueOf(activeProviders));
            }

            @Override
            public void onFailure(String message) {}
        });

        // 2. Fetch Bookings and Revenue
        firebaseHelper.getAllBookings(new FirebaseHelper.ListCallback<Booking>() {
            @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
            @Override
            public void onSuccess(List<Booking> list) {
                int totalBookings = list.size();
                txtTotalBookings.setText(String.valueOf(totalBookings));

                double totalRev = 0.0;
                recentBookingsList.clear();
                recentBookingsList.addAll(list);

                for (Booking b : list) {
                    if ("COMPLETED".equalsIgnoreCase(b.getStatus())) {
                        // Platform charges 10% fee on bookings
                        totalRev += b.getTotalPrice() * 0.10;
                    }
                }

                txtRevenue.setText(String.format(Locale.getDefault(), "₹%.2f", totalRev));

                if (recentBookingsList.isEmpty()) {
                    recyclerRecent.setVisibility(View.GONE);
                    txtEmpty.setVisibility(View.VISIBLE);
                } else {
                    recyclerRecent.setVisibility(View.VISIBLE);
                    txtEmpty.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error fetching bookings: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // High-contrast clean list adapter for system transactions log
    private static class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {
        private final Context context;
        private final List<Booking> logs;

        public RecentActivityAdapter(Context context, List<Booking> logs) {
            this.context = context;
            this.logs = logs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
            return new ViewHolder(v);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Booking b = logs.get(position);
            holder.txtCategory.setText(b.getCategory() + " service");
            holder.txtProvider.setText("Customer: " + b.getCustomerName() + " ➔ Provider: " + b.getProviderName());
            holder.txtPrice.setText(String.format(Locale.getDefault(), "₹%.2f", b.getTotalPrice()));
            holder.txtDate.setText("Date: " + b.getDate());
            holder.txtSlot.setText("Time: " + b.getTimeSlot());
            holder.txtStatus.setText(b.getStatus().toUpperCase());

            // Status Styling
            String status = b.getStatus().toUpperCase();
            int bg;
            if (status.equals("COMPLETED")) {
                bg = R.drawable.bg_status_completed;
            } else if (status.equals("PENDING")) {
                bg = R.drawable.bg_status_pending;
            } else if (status.equals("CANCELLED") || status.equals("REJECTED")) {
                bg = R.drawable.bg_status_cancelled;
            } else {
                bg = R.drawable.bg_status_active;
            }
            holder.txtStatus.setBackground(ContextCompat.getDrawable(context, bg));
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtCategory, txtProvider, txtPrice, txtDate, txtSlot, txtStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtCategory = itemView.findViewById(R.id.txt_booking_category);
                txtProvider = itemView.findViewById(R.id.txt_booking_provider);
                txtPrice = itemView.findViewById(R.id.txt_booking_price);
                txtDate = itemView.findViewById(R.id.txt_booking_date);
                txtSlot = itemView.findViewById(R.id.txt_booking_slot);
                txtStatus = itemView.findViewById(R.id.txt_booking_status);
            }
        }
    }

    private void showAdminAiDialog() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_admin_ai, null);
        dialog.setContentView(sheetView);

        android.widget.ScrollView scrollChat = sheetView.findViewById(R.id.scroll_admin_chat);
        android.widget.LinearLayout layoutChatHistory = sheetView.findViewById(R.id.layout_admin_chat_history);
        android.widget.EditText edtChatInput = sheetView.findViewById(R.id.edt_admin_chat_input);
        android.widget.ImageButton btnSendChat = sheetView.findViewById(R.id.btn_send_admin_chat);
        android.widget.ImageButton btnCloseChat = sheetView.findViewById(R.id.btn_close_admin_chat);

        com.google.android.material.card.MaterialCardView chipMetric = sheetView.findViewById(R.id.chip_metric_summary);
        com.google.android.material.card.MaterialCardView chipVetting = sheetView.findViewById(R.id.chip_vetting_recs);
        com.google.android.material.card.MaterialCardView chipPricing = sheetView.findViewById(R.id.chip_price_opt);

        // Greeting
        addAdminMessage(layoutChatHistory, "Welcome back, Admin! 🐝 📈 I'm BeeAdmin, your platform intelligence advisor. Tap a chip to check platform metrics, provider vetting logs, or optimize pricing algorithms!", false, scrollChat);

        // Chip Clicks
        chipMetric.setOnClickListener(v -> handleAdminQuery(layoutChatHistory, "📊 Metric Summary", scrollChat));
        chipVetting.setOnClickListener(v -> handleAdminQuery(layoutChatHistory, "🤝 Vetting Report", scrollChat));
        chipPricing.setOnClickListener(v -> handleAdminQuery(layoutChatHistory, "💰 Optimize Prices", scrollChat));

        btnSendChat.setOnClickListener(v -> {
            String msg = edtChatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                edtChatInput.setText("");
                handleAdminQuery(layoutChatHistory, msg, scrollChat);
            }
        });

        btnCloseChat.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleAdminQuery(android.widget.LinearLayout layoutChatHistory, String text, android.widget.ScrollView scrollChat) {
        addAdminMessage(layoutChatHistory, text, true, scrollChat);

        // Add a temporary typing bubble
        android.widget.LinearLayout typingContainer = new android.widget.LinearLayout(getContext());
        typingContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        typingContainer.setGravity(android.view.Gravity.START);
        typingContainer.setPadding(0, 8, 0, 8);

        android.widget.TextView typingBubble = new android.widget.TextView(getContext());
        typingBubble.setText("BeeAdmin is thinking...");
        typingBubble.setTextSize(12f);
        typingBubble.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"));
        typingBubble.setPadding(16, 12, 16, 12);
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(android.graphics.Color.parseColor("#121212"));
        gd.setCornerRadius(32f);
        typingBubble.setBackground(gd);
        typingContainer.addView(typingBubble);
        layoutChatHistory.addView(typingContainer);

        scrollChat.post(() -> scrollChat.fullScroll(android.view.View.FOCUS_DOWN));

        new android.os.Handler().postDelayed(() -> {
            layoutChatHistory.removeView(typingContainer);

            String query = text.toLowerCase();
            if (query.contains("metric") || query.contains("summary") || query.contains("stats")) {
                // Query metrics asynchronously
                firebaseHelper.getAllUsers(new FirebaseHelper.ListCallback<User>() {
                    @Override
                    public void onSuccess(List<User> usersList) {
                        firebaseHelper.getAllProviders(new FirebaseHelper.ListCallback<Provider>() {
                            @Override
                            public void onSuccess(List<Provider> providersList) {
                                firebaseHelper.getAllBookings(new FirebaseHelper.ListCallback<Booking>() {
                                    @Override
                                    public void onSuccess(List<Booking> bookingsList) {
                                        int totalUsers = usersList.size();
                                        int activeProviders = 0;
                                        for (Provider p : providersList) {
                                            if (p.isApproved()) activeProviders++;
                                        }
                                        int totalBookings = bookingsList.size();
                                        double revFee = 0.0;
                                        for (Booking b : bookingsList) {
                                            if ("COMPLETED".equalsIgnoreCase(b.getStatus())) {
                                                revFee += b.getTotalPrice() * 0.10;
                                            }
                                        }

                                        String response = String.format(Locale.getDefault(),
                                            "Platform check: We have exactly %d total users registered, with %d active providers holding down the fort. Total bookings completed: %d, driving ₹%s in platform fee earnings! Platform vibe is exceptionally strong! 🚀",
                                            totalUsers, activeProviders, totalBookings, String.format(Locale.getDefault(), "%.2f", revFee));
                                        addAdminMessage(layoutChatHistory, response, false, scrollChat);
                                    }
                                    @Override
                                    public void onFailure(String msg) { addAdminMessage(layoutChatHistory, "Failed to load bookings metrics: " + msg, false, scrollChat); }
                                });
                            }
                            @Override
                            public void onFailure(String msg) { addAdminMessage(layoutChatHistory, "Failed to load providers metrics: " + msg, false, scrollChat); }
                        });
                    }
                    @Override
                    public void onFailure(String msg) { addAdminMessage(layoutChatHistory, "Failed to load users metrics: " + msg, false, scrollChat); }
                });
            } else if (query.contains("vetting") || query.contains("report") || query.contains("jack") || query.contains("approve")) {
                // Find unapproved providers
                firebaseHelper.getAllProviders(new FirebaseHelper.ListCallback<Provider>() {
                    @Override
                    public void onSuccess(List<Provider> list) {
                        List<Provider> unapproved = new ArrayList<>();
                        for (Provider p : list) {
                            if (!p.isApproved()) {
                                unapproved.add(p);
                            }
                        }
                        if (unapproved.isEmpty()) {
                            addAdminMessage(layoutChatHistory, "Vetting Vibe Check 🤝: All active providers are fully approved and verified. Vibe is exceptionally clean, no pending backlogs!", false, scrollChat);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Vetting Alert! 🤝 I detected pending applications:\n");
                            for (Provider p : unapproved) {
                                sb.append(String.format(Locale.getDefault(), "- **%s** (%s category). Vibe Check: He/She holds a solid %.1f base rating with %d years of business experience!\n",
                                    p.getBusinessName(), p.getCategory(), p.getRating(), p.getExperienceYears()));
                            }
                            sb.append("\nRecommendation: APPROVE these profiles immediately to expand community matches, pure excellence!");
                            addAdminMessage(layoutChatHistory, sb.toString(), false, scrollChat);
                        }
                    }
                    @Override
                    public void onFailure(String message) {
                        addAdminMessage(layoutChatHistory, "Failed to inspect pending provider vetting files.", false, scrollChat);
                    }
                });
            } else if (query.contains("price") || query.contains("optimize") || query.contains("surge") || query.contains("cost")) {
                addAdminMessage(layoutChatHistory, "Pricing Engine Optimizer 💰: House Maids & Cleaners are currently experiencing high booking volumes (~85% active load). Recommendation: Apply a dynamic +10% surge pricing adjustment to the 'House Maid' base category to maximize system margins for the weekend!", false, scrollChat);
            } else {
                addAdminMessage(layoutChatHistory, "I'm processing that vibe! 🧠 You can ask me for a 'Metric Summary', a 'Vetting Report' on pending providers, or 'Optimize Prices' to activate surge multiplier calculations!", false, scrollChat);
            }
        }, 800);
    }

    private void addAdminMessage(android.widget.LinearLayout chatHistoryContainer, String text, boolean isUser, android.widget.ScrollView scrollView) {
        if (getContext() == null) return;
        
        android.widget.LinearLayout bubbleContainer = new android.widget.LinearLayout(getContext());
        bubbleContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        bubbleContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bubbleContainer.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);
        bubbleContainer.setPadding(0, 8, 0, 8);

        android.widget.TextView bubble = new android.widget.TextView(getContext());
        android.widget.LinearLayout.LayoutParams bubbleParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        bubbleParams.setMargins(isUser ? 40 : 0, 0, isUser ? 0 : 40, 0);
        bubble.setLayoutParams(bubbleParams);
        bubble.setText(text);
        bubble.setTextSize(13.5f);
        bubble.setTextColor(isUser ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
        bubble.setPadding(24, 16, 24, 16);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(android.graphics.Color.parseColor(isUser ? "#FFC107" : "#212121"));
        
        float r = 32f;
        if (isUser) {
            gd.setCornerRadii(new float[]{r, r, r, r, 0f, 0f, r, r});
        } else {
            gd.setCornerRadii(new float[]{r, r, r, r, r, r, 0f, 0f});
        }
        bubble.setBackground(gd);
        
        bubbleContainer.addView(bubble);
        chatHistoryContainer.addView(bubbleContainer);

        scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
    }
}
