package com.workbee.app.ui.provider.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.workbee.app.R;
import com.workbee.app.data.model.Booking;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.User;
import com.workbee.app.ui.adapter.ProviderJobRequestAdapter;
import com.workbee.app.ui.adapter.ProviderJobsAdapter;
import com.workbee.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProviderDashboardFragment extends Fragment implements ProviderJobRequestAdapter.OnRequestListener, ProviderJobsAdapter.OnJobActionListener {

    private TextView txtWelcome, txtAvailabilityStatus;
    private SwitchCompat switchAvailability;
    private android.widget.ImageView viewStatusIndicatorDot;
    private TextView txtEarnings, txtJobsCompleted, txtRating;
    private RecyclerView recyclerRequests;
    private View layoutEmpty;
    
    // Schedule Planner Calendar views
    private android.widget.CalendarView calendarPlanner;
    private TextView txtSelectedDateLabel;
    private RecyclerView recyclerScheduledJobs;
    private TextView txtNoScheduledJobs;
    private final List<Booking> scheduledJobsList = new ArrayList<>();
    private ProviderJobsAdapter scheduledJobsAdapter;
    private String selectedDateStr; // YYYY-MM-DD
    
    private android.widget.ProgressBar progressVibeMilestone;
    private TextView txtVibeProgressDesc;

    private String activeDispatchedBookingId = null;

    private FirebaseHelper firebaseHelper;
    private Provider currentProvider;
    private final List<Booking> pendingRequests = new ArrayList<>();
    private ProviderJobRequestAdapter adapter;
    private final Handler pollHandler = new Handler();
    private Runnable pollRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider_dashboard, container, false);

        firebaseHelper = FirebaseHelper.getInstance(requireContext());

        // Initialize views
        txtWelcome = view.findViewById(R.id.txt_provider_welcome);
        txtAvailabilityStatus = view.findViewById(R.id.txt_availability_status);
        switchAvailability = view.findViewById(R.id.switch_availability);
        viewStatusIndicatorDot = view.findViewById(R.id.view_status_indicator_dot);
        txtEarnings = view.findViewById(R.id.txt_total_earnings);
        txtJobsCompleted = view.findViewById(R.id.txt_jobs_completed);
        txtRating = view.findViewById(R.id.txt_average_rating);
        recyclerRequests = view.findViewById(R.id.recycler_requests);
        layoutEmpty = view.findViewById(R.id.layout_empty_requests);
        
        progressVibeMilestone = view.findViewById(R.id.progress_vibe_milestone);
        txtVibeProgressDesc = view.findViewById(R.id.txt_vibe_progress_desc);

        recyclerRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProviderJobRequestAdapter(requireContext(), pendingRequests, this);
        recyclerRequests.setAdapter(adapter);

        // Schedule Planner Calendar View Binding
        calendarPlanner = view.findViewById(R.id.calendar_planner);
        txtSelectedDateLabel = view.findViewById(R.id.txt_selected_date_label);
        recyclerScheduledJobs = view.findViewById(R.id.recycler_scheduled_jobs);
        txtNoScheduledJobs = view.findViewById(R.id.txt_no_scheduled_jobs);

        recyclerScheduledJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        scheduledJobsAdapter = new ProviderJobsAdapter(requireContext(), scheduledJobsList, this);
        recyclerScheduledJobs.setAdapter(scheduledJobsAdapter);

        // Default date to today's date formatted as YYYY-MM-DD
        java.util.Calendar cal = java.util.Calendar.getInstance();
        selectedDateStr = String.format(Locale.getDefault(), "%d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));
        updateSelectedDateLabel(cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));

        calendarPlanner.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDateStr = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            updateSelectedDateLabel(year, month + 1, dayOfMonth);
            loadScheduledJobsForDate(selectedDateStr);
        });

        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabProAi = view.findViewById(R.id.fab_pro_ai);
        fabProAi.setOnClickListener(v -> showProAiDialog());

        View btnMenu = view.findViewById(R.id.btn_provider_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> showOptionsMenu(v));
        }

        loadDashboardData();

        return view;
    }

    private void loadDashboardData() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        // Welcome name
        User profile = firebaseHelper.getCurrentUserProfile();
        if (profile != null) {
            txtWelcome.setText(profile.getFullName());
        }

        // Fetch Provider model
        firebaseHelper.getProviderDetails(uid, new FirebaseHelper.DataCallback<Provider>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(Provider provider) {
                currentProvider = provider;
                if (currentProvider != null) {
                    txtWelcome.setText(currentProvider.getBusinessName());
                    txtEarnings.setText(String.format(Locale.getDefault(), "₹%.2f", currentProvider.getEarnings()));
                    txtJobsCompleted.setText(String.valueOf(currentProvider.getCompletedJobs()));
                    txtRating.setText(String.format(Locale.getDefault(), "%.1f ★", currentProvider.getRating()));

                    // Dynamic Vibe level milestones
                    int completed = currentProvider.getCompletedJobs();
                    int target = 50;
                    if (completed >= 50) {
                        target = 100;
                    }
                    int pct = (int) (((double) completed / target) * 100);
                    if (progressVibeMilestone != null) {
                        progressVibeMilestone.setProgress(pct);
                    }
                    if (txtVibeProgressDesc != null) {
                        txtVibeProgressDesc.setText(String.format(Locale.getDefault(), 
                            "Completed: %d / %d Jobs • %d left for Lvl 6 Golden Stinger Status!", 
                            completed, target, target - completed));
                    }

                    // Availability Switch
                    switchAvailability.setOnCheckedChangeListener(null);
                    switchAvailability.setChecked(currentProvider.isAvailable());
                    updateAvailabilityText(currentProvider.isAvailable());
                    setupAvailabilitySwitch();
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Failed to load provider profile: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        // Load Pending Bookings
        loadPendingBookings(uid);
        // Load Scheduled Jobs for selected date
        loadScheduledJobsForDate(selectedDateStr);
    }

    private void loadPendingBookings(String providerId) {
        firebaseHelper.getProviderBookings(providerId, new FirebaseHelper.ListCallback<Booking>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSuccess(List<Booking> bookings) {
                pendingRequests.clear();
                for (Booking b : bookings) {
                    if ("PENDING".equalsIgnoreCase(b.getStatus())) {
                        pendingRequests.add(b);
                    }
                }

                if (pendingRequests.isEmpty()) {
                    recyclerRequests.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                    activeDispatchedBookingId = null;
                } else {
                    recyclerRequests.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();

                    // Dispatch radar notification overlay
                    Booking first = pendingRequests.get(0);
                    if (activeDispatchedBookingId == null || !activeDispatchedBookingId.equals(first.getBookingId())) {
                        activeDispatchedBookingId = first.getBookingId();
                        showDispatchIncomingOverlay(first);
                    }
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Failed to load requests: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAvailabilitySwitch() {
        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentProvider == null) return;
            currentProvider.setAvailable(isChecked);
            updateAvailabilityText(isChecked);

            firebaseHelper.updateProviderDetails(currentProvider, new FirebaseHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    String status = isChecked ? "Online & Available" : "Offline";
                    Toast.makeText(getContext(), "Status updated to " + status, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(getContext(), "Failed to update availability: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateAvailabilityText(boolean isAvailable) {
        if (isAvailable) {
            txtAvailabilityStatus.setText("Available for Service bookings");
            if (viewStatusIndicatorDot != null) {
                viewStatusIndicatorDot.setImageResource(R.drawable.bg_status_dot_online);
            }
        } else {
            txtAvailabilityStatus.setText("Offline (No new incoming bookings)");
            if (viewStatusIndicatorDot != null) {
                viewStatusIndicatorDot.setImageResource(R.drawable.bg_status_dot_offline);
            }
        }
    }

    @Override
    public void onAccept(Booking booking) {
        firebaseHelper.updateBookingStatus(booking.getBookingId(), "ACCEPTED", new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getContext() == null) return;
                android.widget.Toast.makeText(getContext(), "Service Request Accepted! Starting navigation... 🗺️", android.widget.Toast.LENGTH_SHORT).show();

                // Auto-launch live tracking map for the worker
                android.content.Intent trackIntent = new android.content.Intent(getContext(),
                        com.workbee.app.ui.provider.WorkerTrackingActivity.class);
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_BOOKING_ID,       booking.getBookingId());
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_LAT,     booking.getLatitude());
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_LNG,     booking.getLongitude());
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_NAME,    booking.getCustomerName());
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_ADDRESS, booking.getAddress());
                startActivity(trackIntent);

                loadDashboardData(); // Refresh list & earnings stats
            }

            @Override
            public void onFailure(String message) {
                android.widget.Toast.makeText(getContext(), "Failed to accept: " + message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onReject(Booking booking) {
        firebaseHelper.updateBookingStatus(booking.getBookingId(), "REJECTED", new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Service Request Declined.", Toast.LENGTH_SHORT).show();
                loadDashboardData(); // Refresh list
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Failed to decline: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSelectedDateLabel(int year, int month, int dayOfMonth) {
        if (txtSelectedDateLabel != null) {
            txtSelectedDateLabel.setText(String.format(Locale.getDefault(), "Jobs Scheduled on %02d/%02d/%d:", dayOfMonth, month, year));
        }
    }

    private void loadScheduledJobsForDate(String dateStr) {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        firebaseHelper.getProviderBookings(uid, new FirebaseHelper.ListCallback<Booking>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSuccess(List<Booking> bookings) {
                scheduledJobsList.clear();
                for (Booking b : bookings) {
                    if (dateStr.equals(b.getDate())) {
                        String status = b.getStatus().toUpperCase();
                        if (status.equals("ACCEPTED") || status.equals("ON_THE_WAY") || status.equals("IN_PROGRESS") || status.equals("COMPLETED")) {
                            scheduledJobsList.add(b);
                        }
                    }
                }

                if (scheduledJobsList.isEmpty()) {
                    recyclerScheduledJobs.setVisibility(View.GONE);
                    txtNoScheduledJobs.setVisibility(View.VISIBLE);
                } else {
                    recyclerScheduledJobs.setVisibility(View.VISIBLE);
                    txtNoScheduledJobs.setVisibility(View.GONE);
                    scheduledJobsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load scheduled jobs: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onUpdateJobStatus(Booking booking, String nextStatus) {
        firebaseHelper.updateBookingStatus(booking.getBookingId(), nextStatus, new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Status updated to " + nextStatus, Toast.LENGTH_SHORT).show();

                // If ON_THE_WAY, launch navigation activity
                if ("ON_THE_WAY".equals(nextStatus)) {
                    android.content.Intent trackIntent = new android.content.Intent(getContext(),
                            com.workbee.app.ui.provider.WorkerTrackingActivity.class);
                    trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_BOOKING_ID,       booking.getBookingId());
                    trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_LAT,     booking.getLatitude());
                    trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_LNG,     booking.getLongitude());
                    trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_NAME,    booking.getCustomerName());
                    trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_ADDRESS, booking.getAddress());
                    startActivity(trackIntent);
                }

                loadDashboardData();
            }

            @Override
            public void onFailure(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to update status: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showProAiDialog() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_pro_ai, null);
        dialog.setContentView(sheetView);

        android.widget.ScrollView scrollProChat = sheetView.findViewById(R.id.scroll_pro_chat);
        android.widget.LinearLayout layoutProChatHistory = sheetView.findViewById(R.id.layout_pro_chat_history);
        android.widget.EditText edtProChatInput = sheetView.findViewById(R.id.edt_pro_chat_input);
        android.widget.ImageButton btnSendProChat = sheetView.findViewById(R.id.btn_send_pro_chat);
        android.widget.ImageButton btnCloseProChat = sheetView.findViewById(R.id.btn_close_pro_chat);

        com.google.android.material.card.MaterialCardView chipOptProfile = sheetView.findViewById(R.id.chip_opt_profile);
        com.google.android.material.card.MaterialCardView chipDraftReply = sheetView.findViewById(R.id.chip_draft_reply);
        com.google.android.material.card.MaterialCardView chipEliteVibe = sheetView.findViewById(R.id.chip_elite_vibe);

        // Welcome message
        addProMessage(layoutProChatHistory, "Yo! I'm ProAI. 🐝 Your smart earnings & business helper! Need tips on optimizing your profile, checking your level milestones, or drafting review responses? Ask me!", false, scrollProChat);

        // Chip click listeners
        chipOptProfile.setOnClickListener(v -> handleProUserMessage(layoutProChatHistory, "📈 Optimize Profile", scrollProChat));
        chipDraftReply.setOnClickListener(v -> handleProUserMessage(layoutProChatHistory, "📝 Draft Review Reply", scrollProChat));
        chipEliteVibe.setOnClickListener(v -> handleProUserMessage(layoutProChatHistory, "🍯 Elite Vibe Check", scrollProChat));

        btnSendProChat.setOnClickListener(v -> {
            String msg = edtProChatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                edtProChatInput.setText("");
                handleProUserMessage(layoutProChatHistory, msg, scrollProChat);
            }
        });

        btnCloseProChat.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleProUserMessage(android.widget.LinearLayout layoutProChatHistory, String text, android.widget.ScrollView scrollProChat) {
        addProMessage(layoutProChatHistory, text, true, scrollProChat);

        // Add typing bubble
        android.widget.LinearLayout typingContainer = new android.widget.LinearLayout(getContext());
        typingContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        typingContainer.setGravity(android.view.Gravity.START);
        typingContainer.setPadding(0, 8, 0, 8);

        android.widget.TextView typingBubble = new android.widget.TextView(getContext());
        typingBubble.setText("ProAI is analyzing data...");
        typingBubble.setTextSize(12f);
        typingBubble.setTextColor(android.graphics.Color.parseColor("#80FFFFFF"));
        typingBubble.setPadding(16, 12, 16, 12);
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(android.graphics.Color.parseColor("#121212"));
        gd.setCornerRadius(32f);
        typingBubble.setBackground(gd);
        typingContainer.addView(typingBubble);
        layoutProChatHistory.addView(typingContainer);

        scrollProChat.post(() -> scrollProChat.fullScroll(android.view.View.FOCUS_DOWN));

        // Delay response to simulate AI thinking
        new android.os.Handler().postDelayed(() -> {
            layoutProChatHistory.removeView(typingContainer);

            String query = text.toLowerCase();
            if (query.contains("opt") || query.contains("profile") || query.contains("optimize")) {
                addProMessage(layoutProChatHistory, "Chompin' at the bit to double your earnings? 📈 Residential house maids and cooking pros are seeing a massive 30% spike in local bookings! I suggest adding 'Elite Dusting & Deep Mop Care Specialist' to your bio, and raising your base rate. Money move, no cap!", false, scrollProChat);
            } else if (query.contains("draft") || query.contains("reply") || query.contains("review")) {
                addProMessage(layoutProChatHistory, "Let's secure that 5-star reputation! 💅 Click below to copy a customized, premium Gen Z reply to Sarah Jenkins' latest review: 'Tysm Sarah, serving clean vibes is our passion! Absolute pleasure working with you!'", false, scrollProChat);
                addProActionCard(layoutProChatHistory, "Copy Gen Z Reply 📋", "Tysm Sarah, serving clean vibes is our passion! Absolute pleasure working with you!", scrollProChat);
            } else if (query.contains("elite") || query.contains("vibe") || query.contains("level") || query.contains("points")) {
                double earnings = currentProvider != null ? currentProvider.getEarnings() : 2058.00;
                int jobs = currentProvider != null ? currentProvider.getCompletedJobs() : 42;
                double rating = currentProvider != null ? currentProvider.getRating() : 4.8;
                
                String status = String.format(Locale.getDefault(), "Business vibe check: You've pocketed ₹%.2f across %d completed jobs with a solid %.1f ★ rating! 🌟 You are currently Honeycomb Elite Level 2 - complete 8 more jobs to unlock Level 3 and claim a 0%% service charge week!", earnings, jobs, rating);
                addProMessage(layoutProChatHistory, status, false, scrollProChat);
            } else {
                addProMessage(layoutProChatHistory, "No cap, I'm analyzing that business vibe! 🧠 Ask me to optimize your profile, draft review replies, or check your Level tier status!", false, scrollProChat);
            }
        }, 800);
    }

    private void addProMessage(android.widget.LinearLayout chatHistoryContainer, String text, boolean isUser, android.widget.ScrollView scrollView) {
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

    private void addProActionCard(android.widget.LinearLayout chatHistoryContainer, String label, String textToCopy, android.widget.ScrollView scrollView) {
        if (getContext() == null) return;

        android.widget.LinearLayout buttonContainer = new android.widget.LinearLayout(getContext());
        buttonContainer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        buttonContainer.setGravity(android.view.Gravity.START);
        buttonContainer.setPadding(12, 4, 12, 12);

        com.google.android.material.button.MaterialButton button = new com.google.android.material.button.MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonStyle);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        button.setLayoutParams(params);
        button.setText(label);
        button.setTextSize(11.5f);
        button.setTextColor(android.graphics.Color.BLACK);
        button.setBackgroundColor(android.graphics.Color.parseColor("#FFC107"));
        button.setCornerRadius(18);
        button.setAllCaps(false);

        button.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("ProAI Response", textToCopy);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Draft response copied to clipboard! 📋", Toast.LENGTH_SHORT).show();
            }
        });

        buttonContainer.addView(button);
        chatHistoryContainer.addView(buttonContainer);

        scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
    }

    private void showDispatchIncomingOverlay(Booking booking) {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_uber_incoming, null);
        dialog.setContentView(sheetView);
        dialog.setCancelable(false);

        android.widget.ProgressBar progressCountdown = sheetView.findViewById(R.id.progress_dispatch_countdown);
        TextView txtSeconds = sheetView.findViewById(R.id.txt_dispatch_seconds);
        TextView txtClient = sheetView.findViewById(R.id.txt_dispatch_client);
        TextView txtAddress = sheetView.findViewById(R.id.txt_dispatch_address);
        TextView txtDesc = sheetView.findViewById(R.id.txt_dispatch_desc);
        TextView txtPayout = sheetView.findViewById(R.id.txt_dispatch_payout);

        View btnDecline = sheetView.findViewById(R.id.btn_dispatch_decline);
        View btnAccept = sheetView.findViewById(R.id.btn_dispatch_accept);

        txtClient.setText("Client: " + booking.getCustomerName());
        txtAddress.setText(booking.getAddress());
        txtDesc.setText("Description: " + booking.getDescription());
        txtPayout.setText(String.format(Locale.getDefault(), "₹%.2f", booking.getTotalPrice() * 0.90));

        // Start 30 seconds countdown ticking
        final Handler countdownHandler = new Handler();
        final int[] totalSecs = {30};
        final Runnable countdownRunnable = new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                totalSecs[0]--;
                if (txtSeconds != null) {
                    txtSeconds.setText(totalSecs[0] + "s");
                }
                if (progressCountdown != null) {
                    int pct = (int) (((double) totalSecs[0] / 30) * 100);
                    progressCountdown.setProgress(pct);
                }

                if (totalSecs[0] <= 0) {
                    countdownHandler.removeCallbacksAndMessages(null);
                    Toast.makeText(getContext(), "Dispatch request expired! ⏳", Toast.LENGTH_SHORT).show();
                    declineBookingDispatch(booking, dialog);
                } else {
                    countdownHandler.postDelayed(this, 1000);
                }
            }
        };
        countdownHandler.postDelayed(countdownRunnable, 1000);

        btnAccept.setOnClickListener(v -> {
            countdownHandler.removeCallbacksAndMessages(null);
            acceptBookingDispatch(booking, dialog);
        });

        btnDecline.setOnClickListener(v -> {
            countdownHandler.removeCallbacksAndMessages(null);
            declineBookingDispatch(booking, dialog);
        });

        dialog.show();
    }

    private void acceptBookingDispatch(Booking booking, com.google.android.material.bottomsheet.BottomSheetDialog dialog) {
        firebaseHelper.updateBookingStatus(booking.getBookingId(), "ACCEPTED", new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Nearby Job Accepted! Starting navigation... 🗺️", Toast.LENGTH_SHORT).show();

                // Auto-launch live tracking map for the worker
                android.content.Intent trackIntent = new android.content.Intent(getContext(),
                        com.workbee.app.ui.provider.WorkerTrackingActivity.class);
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_BOOKING_ID,       booking.getBookingId());
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_LAT,     booking.getLatitude());
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_LNG,     booking.getLongitude());
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_NAME,    booking.getCustomerName());
                trackIntent.putExtra(com.workbee.app.ui.provider.WorkerTrackingActivity.EXTRA_CUSTOMER_ADDRESS, booking.getAddress());
                startActivity(trackIntent);

                loadDashboardData();
            }

            @Override
            public void onFailure(String message) {
                dialog.dismiss();
                Toast.makeText(getContext(), "Error accepting job: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void declineBookingDispatch(Booking booking, com.google.android.material.bottomsheet.BottomSheetDialog dialog) {
        firebaseHelper.updateBookingStatus(booking.getBookingId(), "REJECTED", new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
                Toast.makeText(getContext(), "Nearby dispatch declined.", Toast.LENGTH_SHORT).show();
                loadDashboardData();
            }

            @Override
            public void onFailure(String message) {
                dialog.dismiss();
                loadDashboardData();
            }
        });
    }

    private void showOptionsMenu(View anchor) {
        if (getContext() == null) return;

        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "🍯 Honeycomb Milestones Status");
        popup.getMenu().add(0, 2, 1, "🐝 Open ProAI Helper Console");
        popup.getMenu().add(0, 3, 2, "⚡ Go Online / Offline Toggle");
        popup.getMenu().add(0, 4, 3, "🚪 Sign Out of WorkBee");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    int completed = currentProvider != null ? currentProvider.getCompletedJobs() : 42;
                    Toast.makeText(getContext(), "Vibe Milestone Level 5 Status: " + completed + " Jobs Completed! Honeycomb commission is active! 🍯", Toast.LENGTH_LONG).show();
                    return true;
                case 2:
                    showProAiDialog();
                    return true;
                case 3:
                    if (switchAvailability != null) {
                        switchAvailability.setChecked(!switchAvailability.isChecked());
                    }
                    return true;
                case 4:
                    if (firebaseHelper != null) {
                        firebaseHelper.logout();
                        Toast.makeText(getContext(), "Logged out of WorkBee. See ya! 🐝", Toast.LENGTH_SHORT).show();
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }
                    return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    private void startPolling() {
        if (pollRunnable != null) return;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                String uid = firebaseHelper.getCurrentUserId();
                if (uid != null) {
                    loadPendingBookings(uid);
                }
                pollHandler.postDelayed(this, 3000); // Refresh bookings list every 3 seconds
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void stopPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }
}
