package com.workbee.app.ui.customer.fragment;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.workbee.app.R;
import com.workbee.app.data.model.User;
import com.workbee.app.data.repository.FirebaseRepository;
import com.workbee.app.ui.auth.LoginActivity;
import com.workbee.app.utils.FirebaseHelper;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private FirebaseRepository repository;
    private CircleImageView imgAvatar;
    private TextView txtRoleBadge;
    private EditText edtFullName, edtPhone, edtAddress;
    private Button btnSave, btnLogout;
    private SwitchCompat switchDarkMode;

    private TextView txtWalletBalance;
    private Button btnTopUpWallet;
    private View btnBeeGoldPremium;

    private LinearLayout leaderboardContainer;

    private TextView txtHivePoints;
    private Button btnEarnPoints, btnRedeemRewards;
    private TextView txtBadgeIcon1, txtBadgeTitle1, txtBadgeDesc1;
    private TextView txtBadgeIcon2, txtBadgeTitle2, txtBadgeDesc2;
    private TextView txtBadgeIcon3, txtBadgeTitle3, txtBadgeDesc3;
    private View btnSupportChat, btnReferFriend, btnSafetyVetting, btnGuidelines;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        repository = new FirebaseRepository(requireContext());

        imgAvatar = view.findViewById(R.id.img_avatar);
        txtRoleBadge = view.findViewById(R.id.txt_role_badge);
        edtFullName = view.findViewById(R.id.edt_fullname);
        edtPhone = view.findViewById(R.id.edt_phone);
        edtAddress = view.findViewById(R.id.edt_address);
        btnSave = view.findViewById(R.id.btn_save_profile);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        btnLogout = view.findViewById(R.id.btn_logout);

        txtHivePoints = view.findViewById(R.id.txt_hive_points);
        btnEarnPoints = view.findViewById(R.id.btn_earn_points);
        btnRedeemRewards = view.findViewById(R.id.btn_redeem_rewards);

        txtBadgeIcon1 = view.findViewById(R.id.txt_badge_icon_1);
        txtBadgeTitle1 = view.findViewById(R.id.txt_badge_title_1);
        txtBadgeDesc1 = view.findViewById(R.id.txt_badge_desc_1);

        txtBadgeIcon2 = view.findViewById(R.id.txt_badge_icon_2);
        txtBadgeTitle2 = view.findViewById(R.id.txt_badge_title_2);
        txtBadgeDesc2 = view.findViewById(R.id.txt_badge_desc_2);

        txtBadgeIcon3 = view.findViewById(R.id.txt_badge_icon_3);
        txtBadgeTitle3 = view.findViewById(R.id.txt_badge_title_3);
        txtBadgeDesc3 = view.findViewById(R.id.txt_badge_desc_3);

        btnSupportChat = view.findViewById(R.id.btn_support_chat);
        btnReferFriend = view.findViewById(R.id.btn_refer_friend);
        btnSafetyVetting = view.findViewById(R.id.btn_safety_vetting);
        btnGuidelines = view.findViewById(R.id.btn_guidelines);
        btnBeeGoldPremium = view.findViewById(R.id.btn_beegold_premium);
        txtWalletBalance = view.findViewById(R.id.txt_wallet_balance);
        btnTopUpWallet = view.findViewById(R.id.btn_top_up_wallet);

        loadProfileData();

        btnSave.setOnClickListener(v -> handleSaveProfile());
        btnLogout.setOnClickListener(v -> handleLogout());

        btnEarnPoints.setOnClickListener(v -> showEarnPointsDialog());
        btnRedeemRewards.setOnClickListener(v -> showRedeemRewardsDialog());
        btnSupportChat.setOnClickListener(v -> showSupportChatDialog());
        btnReferFriend.setOnClickListener(v -> showReferFriendDialog());
        btnSafetyVetting.setOnClickListener(v -> showSafetyVettingDialog());
        btnGuidelines.setOnClickListener(v -> showGuidelinesDialog());
        
        if (btnTopUpWallet != null) {
            btnTopUpWallet.setOnClickListener(v -> showTopUpDialog());
        }
        if (btnBeeGoldPremium != null) {
            btnBeeGoldPremium.setOnClickListener(v -> startActivity(new Intent(getContext(), com.workbee.app.ui.customer.BeeGoldActivity.class)));
        }

        SharedPreferences themePrefs = requireActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        boolean isDark = themePrefs.getBoolean("dark_theme", true);
        switchDarkMode.setChecked(isDark);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = themePrefs.edit();
            editor.putBoolean("dark_theme", isChecked);
            editor.apply();
            
            // Set AppCompatDelegate night mode immediately
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES 
                          : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            );
            
            Toast.makeText(getContext(), isChecked ? "Dark theme enabled" : "Light theme enabled", Toast.LENGTH_SHORT).show();
        });

        // Initialize and Setup Standings
        leaderboardContainer = view.findViewById(R.id.leaderboard_rows_container);
        setupLeaderboardStandings();

        return view;
    }

    private void loadProfileData() {
        // Always fetch fresh profile from current session to avoid showing mock seeded data
        String uid = repository.getCurrentUserId();
        if (uid != null) {
            repository.fetchUserProfile(uid, new FirebaseHelper.AuthCallback() {
                @Override
                public void onSuccess(User profile) {
                    if (getContext() == null) return;
                    bindProfileToViews(profile);
                }

                @Override
                public void onFailure(String message) {
                    // Fallback to cached profile if fetch fails
                    User cached = repository.getCurrentUserProfile();
                    if (cached != null) bindProfileToViews(cached);
                }
            });
        } else {
            // No session — show cached profile if available
            User profile = repository.getCurrentUserProfile();
            if (profile != null) bindProfileToViews(profile);
        }
    }

    private void bindProfileToViews(User profile) {
        edtFullName.setText(profile.getFullName());
        edtPhone.setText(profile.getPhone());
        if (profile.getAddress() != null) {
            edtAddress.setText(profile.getAddress());
        }
        txtRoleBadge.setText(profile.getRole() + (profile.isGoldUser() ? " 👑" : ""));

        if (txtWalletBalance != null) {
            txtWalletBalance.setText(String.format("₹%.2f", profile.getWalletBalance()));
        }

        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                 .load(profile.getProfileImageUrl())
                 .placeholder(R.drawable.ic_workbee_logo)
                 .into(imgAvatar);
        }

        // Dynamic badges and Hive Points based on role
        String role = profile.getRole();
        if ("PROVIDER".equalsIgnoreCase(role)) {
            txtHivePoints.setText("950 HP");
            txtBadgeIcon1.setText("🏆");
            txtBadgeTitle1.setText("Top Voted Pro");
            txtBadgeDesc1.setText("Vibe Check: Elite");

            txtBadgeIcon2.setText("⚡");
            txtBadgeTitle2.setText("100% On-Time");
            txtBadgeDesc2.setText("Response: Swift");

            txtBadgeIcon3.setText("💅");
            txtBadgeTitle3.setText("Vibe Verified");
            txtBadgeDesc3.setText("Rating: 4.9⭐");
        } else {
            txtHivePoints.setText("750 HP");
            txtBadgeIcon1.setText("👑");
            txtBadgeTitle1.setText("Super Customer");
            txtBadgeDesc1.setText("Vibe Level: Elite");

            txtBadgeIcon2.setText("⚡");
            txtBadgeTitle2.setText("Fast Booker");
            txtBadgeDesc2.setText("Reliability: Swift");

            txtBadgeIcon3.setText("💖");
            txtBadgeTitle3.setText("Top Tipper");
            txtBadgeDesc3.setText("Rating: 5.0⭐");
        }
    }

    private void handleSaveProfile() {
        String name = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            edtFullName.setError("Full name is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Phone is required");
            return;
        }

        User profile = repository.getCurrentUserProfile();
        if (profile != null) {
            profile.setFullName(name);
            profile.setPhone(phone);
            profile.setAddress(address);

            repository.updateProfile(profile, new FirebaseHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(getContext(), "Failed to update profile: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showTopUpDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Top Up Hive Wallet 🪙");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        TextView txtHelp = new TextView(requireContext());
        txtHelp.setText("Add credits to your WorkBee Wallet for seamless one-click checkout:");
        txtHelp.setTextSize(13);
        txtHelp.setTextColor(requireContext().getColor(R.color.secondary));
        txtHelp.setPadding(0, 0, 0, 20);
        layout.addView(txtHelp);

        EditText edtAmount = new EditText(requireContext());
        edtAmount.setHint("Enter amount (₹)");
        edtAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        edtAmount.setText("500");
        layout.addView(edtAmount);

        // Add Quick options row
        android.widget.LinearLayout quickRow = new android.widget.LinearLayout(requireContext());
        quickRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        quickRow.setGravity(Gravity.CENTER);
        quickRow.setPadding(0, 16, 0, 0);

        String[] quickOptions = {"100", "200", "500", "1000"};
        for (String opt : quickOptions) {
            Button btnOpt = new Button(requireContext());
            btnOpt.setText("+" + opt);
            btnOpt.setTextSize(10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lp.setMargins(4, 0, 4, 0);
            btnOpt.setLayoutParams(lp);
            btnOpt.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.primary)));
            btnOpt.setTextColor(requireContext().getColor(R.color.black));
            btnOpt.setOnClickListener(v -> edtAmount.setText(opt));
            quickRow.addView(btnOpt);
        }
        layout.addView(quickRow);

        builder.setView(layout);
        builder.setPositiveButton("Top Up Now", (dialog, which) -> {
            String amtStr = edtAmount.getText().toString().trim();
            if (!TextUtils.isEmpty(amtStr)) {
                try {
                    double amt = Double.parseDouble(amtStr);
                    User user = repository.getCurrentUserProfile();
                    if (user != null) {
                        double newBal = user.getWalletBalance() + amt;
                        user.setWalletBalance(newBal);
                        repository.updateProfile(user, new FirebaseHelper.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Wallet topped up successfully! 🪙", Toast.LENGTH_SHORT).show();
                                loadProfileData();
                            }

                            @Override
                            public void onFailure(String message) {
                                Toast.makeText(getContext(), "Failed to top up: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void handleLogout() {
        repository.logout();
        Toast.makeText(getContext(), "Logged out successfully!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showEarnPointsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("How to Earn Hive Points 🐝");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        layout.addView(createEarnRow("🤝 Refer a Friend", "Earn 50 HP instantly when they book their first job."));
        layout.addView(createEarnRow("⭐ Give high ratings", "Earn 20 HP for every 5-star review you leave or receive."));
        layout.addView(createEarnRow("🛠️ Complete bookings", "Earn 10 HP for every booking completed successfully."));

        builder.setView(layout);
        builder.setPositiveButton("Let's Go!", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private View createEarnRow(String title, String desc) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 16, 0, 16);

        TextView txtTitle = new TextView(requireContext());
        txtTitle.setText(title);
        txtTitle.setTextSize(14);
        txtTitle.setTypeface(null, Typeface.BOLD);
        txtTitle.setTextColor(requireContext().getColor(R.color.secondary));

        TextView txtDesc = new TextView(requireContext());
        txtDesc.setText(desc);
        txtDesc.setTextSize(11);
        txtDesc.setTextColor(requireContext().getColor(R.color.grey_600));
        txtDesc.setPadding(0, 4, 0, 0);

        row.addView(txtTitle);
        row.addView(txtDesc);
        return row;
    }

    private void showRedeemRewardsDialog() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_redeem_rewards, null);
        dialog.setContentView(sheetView);

        // Find views
        View cardScratch = sheetView.findViewById(R.id.card_scratch_deck);
        View layoutRevealed = sheetView.findViewById(R.id.layout_coupon_revealed);
        TextView txtCode = sheetView.findViewById(R.id.txt_revealed_code);
        View btnDismiss = sheetView.findViewById(R.id.scratch_btn_dismiss);

        // Initially scratch cover is visible, revealed layout is underneath
        layoutRevealed.setVisibility(View.INVISIBLE);

        cardScratch.setOnClickListener(v -> {
            // Shimmer / Scratch action!
            cardScratch.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .alpha(0.0f)
                .setDuration(400)
                .withEndAction(() -> {
                    cardScratch.setVisibility(View.GONE);
                    layoutRevealed.setVisibility(View.VISIBLE);
                    layoutRevealed.setAlpha(0f);
                    layoutRevealed.setScaleX(0.8f);
                    layoutRevealed.setScaleY(0.8f);
                    layoutRevealed.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start();

                    // Automatically copy code to clipboard
                    copyToClipboard("BEESCRATCH30");
                    Toast.makeText(getContext(), "Coupon BEESCRATCH30 copied! 📋", Toast.LENGTH_SHORT).show();
                })
                .start();
        });

        btnDismiss.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("WorkBee Code", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    private void showReferFriendDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Refer your Friends 🎁");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView txtLabel = new TextView(requireContext());
        txtLabel.setText("Share the WorkBee vibe and earn 50 Hive Points!");
        txtLabel.setTextSize(13);
        txtLabel.setTextColor(requireContext().getColor(R.color.secondary));
        txtLabel.setPadding(0, 0, 0, 20);
        layout.addView(txtLabel);

        TextView txtCode = new TextView(requireContext());
        txtCode.setText("WORKBEE-GENZ-99");
        txtCode.setTextSize(18);
        txtCode.setGravity(Gravity.CENTER);
        txtCode.setTypeface(null, Typeface.BOLD);
        txtCode.setTextColor(requireContext().getColor(R.color.primary_dark));
        txtCode.setPadding(20, 20, 20, 20);

        GradientDrawable border = new GradientDrawable();
        border.setColor(requireContext().getColor(R.color.semi_transparent_yellow));
        border.setCornerRadius(16);
        border.setStroke(2, requireContext().getColor(R.color.primary));
        txtCode.setBackground(border);

        layout.addView(txtCode);

        builder.setView(layout);
        builder.setPositiveButton("Copy Code", (dialog, which) -> {
            copyToClipboard("WORKBEE-GENZ-99");
            Toast.makeText(getContext(), "Referral code copied!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showSafetyVettingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Safety Vetting Status 🛡️");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView txtContent = new TextView(requireContext());
        txtContent.setText("Your WorkBee profile is 100% vetted and verified:\n\n" +
                "🛡️ Identity Verification: PASSED\n" +
                "💅 Community Vibe Check: ELITE\n" +
                "🔒 Account Protection: SECURE\n\n" +
                "You are a trusted member of the WorkBee community.");
        txtContent.setTextSize(13);
        txtContent.setTextColor(requireContext().getColor(R.color.secondary));
        layout.addView(txtContent);

        builder.setView(layout);
        builder.setPositiveButton("Awesome!", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showGuidelinesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Hive Guidelines & Manifesto 📜");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView txtContent = new TextView(requireContext());
        txtContent.setText("Welcome to the Honeycomb. Here are our core vibes:\n\n" +
                "🐝 1. Be Kind: Respect every member of our hive.\n\n" +
                "🐝 2. Stay Safe: Keep communication and payments securely within the app.\n\n" +
                "🐝 3. Be Reliable: Keep appointments and show up with professional vibes.");
        txtContent.setTextSize(13);
        txtContent.setTextColor(requireContext().getColor(R.color.secondary));
        layout.addView(txtContent);

        builder.setView(layout);
        builder.setPositiveButton("I Promise!", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showSupportChatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Support Bee Chat 🐝");

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        // Scrollable Message Container
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 450); // fixed height for dialog
        scrollView.setLayoutParams(scrollLp);
        
        LinearLayout chatFeed = new LinearLayout(requireContext());
        chatFeed.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(chatFeed);
        root.addView(scrollView);

        // Quick Reply layout
        LinearLayout quickReplies = new LinearLayout(requireContext());
        quickReplies.setOrientation(LinearLayout.VERTICAL);
        quickReplies.setPadding(0, 12, 0, 12);
        root.addView(quickReplies);

        // User Input Layout (EditText + Send Button)
        LinearLayout inputLayout = new LinearLayout(requireContext());
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        EditText edtMsg = new EditText(requireContext());
        edtMsg.setHint("Type a message...");
        edtMsg.setTextSize(13);
        LinearLayout.LayoutParams edtLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        edtMsg.setLayoutParams(edtLp);

        Button btnSend = new Button(requireContext());
        btnSend.setText("Send");
        btnSend.setTextSize(11);
        btnSend.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.primary)));
        btnSend.setTextColor(requireContext().getColor(R.color.black));

        inputLayout.addView(edtMsg);
        inputLayout.addView(btnSend);
        root.addView(inputLayout);

        // Local helper class to add bubbles
        class ChatBubbleAdder {
            void add(String msg, boolean isUs) {
                TextView bubble = new TextView(requireContext());
                bubble.setText(msg);
                bubble.setTextSize(12);
                bubble.setPadding(16, 12, 16, 12);
                
                LinearLayout.LayoutParams bubbleLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bubbleLp.setMargins(0, 8, 0, 8);
                
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(16);
                if (isUs) {
                    bubbleLp.gravity = Gravity.END;
                    bg.setColor(requireContext().getColor(R.color.primary));
                    bubble.setTextColor(requireContext().getColor(R.color.black));
                } else {
                    bubbleLp.gravity = Gravity.START;
                    bg.setColor(requireContext().getColor(R.color.grey_100));
                    bubble.setTextColor(requireContext().getColor(R.color.secondary));
                }
                bubble.setBackground(bg);
                bubble.setLayoutParams(bubbleLp);
                
                chatFeed.addView(bubble);
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            }
        }
        
        ChatBubbleAdder bubbleAdder = new ChatBubbleAdder();

        // Local helper class to reply
        class BotReplier {
            void reply(String userQuery) {
                new Handler().postDelayed(() -> {
                    String ans = "Got it! Let me buzz our specialists to look into that. 🐝🐝";
                    if (userQuery.contains("track")) {
                        ans = "Simply navigate to the 'Bookings' tab in the bottom bar to see real-time updates! ⚡";
                    } else if (userQuery.contains("point") || userQuery.contains("hp")) {
                        ans = "Hive Points can be redeemed for cool discount coupons! Check out the Redeem button on your profile. 🐝";
                    } else if (userQuery.contains("late") || userQuery.contains("worker")) {
                        ans = "Oh no! Don't worry, you can check the booking status inside Bookings. If they are extremely late, reach us at care@workbee.app! 🛡️";
                    }
                    bubbleAdder.add(ans, false);
                }, 800);
            }
        }
        
        BotReplier replier = new BotReplier();

        // Welcome message
        bubbleAdder.add("Hey there! I am Support Bee, your personal helper. How's the vibe today? 🐝", false);

        // Load Quick Replies
        String[] replies = {
                "How do I track my booking? 📅",
                "How do Hive Points work? 🐝",
                "Why is my worker late? ⏳"
        };

        for (String repText : replies) {
            Button quickBtn = new Button(requireContext());
            quickBtn.setText(repText);
            quickBtn.setTextSize(10);
            quickBtn.setAllCaps(false);
            quickBtn.setPadding(8, 4, 8, 4);
            quickBtn.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.grey_100)));
            quickBtn.setTextColor(requireContext().getColor(R.color.secondary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 4, 0, 4);
            quickBtn.setLayoutParams(lp);

            quickBtn.setOnClickListener(v -> {
                bubbleAdder.add(repText, true);
                quickReplies.removeView(quickBtn);
                replier.reply(repText.toLowerCase());
            });
            quickReplies.addView(quickBtn);
        }

        btnSend.setOnClickListener(v -> {
            String text = edtMsg.getText().toString().trim();
            if (!text.isEmpty()) {
                bubbleAdder.add(text, true);
                edtMsg.setText("");
                replier.reply(text.toLowerCase());
            }
        });

        builder.setView(root);
        builder.setNegativeButton("Close Support", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void setupLeaderboardStandings() {
        if (leaderboardContainer == null) return;
        leaderboardContainer.removeAllViews();

        if (getContext() == null) return;
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // Display weekly premium rankings
        addLeaderboardRow(inflater, 1, "👑", "Sarah K. (Cleaning Pro)", "Platinum Tier", "1480 HP", "#FFD700");
        addLeaderboardRow(inflater, 2, "🐝", "David M. (Plumbing Expert)", "Gold Tier", "1250 HP", "#C0C0C0");
        addLeaderboardRow(inflater, 3, "✨", "You (Active Booker)", "Gold Tier", "750 HP", "#CD7F32");
    }

    private void addLeaderboardRow(LayoutInflater inflater, int rank, String emoji, String name, String tier, String hp, String strokeColorHex) {
        View rowView = inflater.inflate(R.layout.item_leaderboard_row, leaderboardContainer, false);
        
        TextView lblRankNum = rowView.findViewById(R.id.lbl_rank_num);
        TextView lblRankEmoji = rowView.findViewById(R.id.lbl_rank_emoji);
        TextView lblRankName = rowView.findViewById(R.id.lbl_rank_name);
        TextView lblRankTier = rowView.findViewById(R.id.lbl_rank_tier);
        TextView lblRankHp = rowView.findViewById(R.id.lbl_rank_hp);
        
        if (lblRankNum != null) lblRankNum.setText(String.valueOf(rank));
        if (lblRankEmoji != null) lblRankEmoji.setText(emoji);
        if (lblRankName != null) lblRankName.setText(name);
        if (lblRankTier != null) lblRankTier.setText(tier);
        if (lblRankHp != null) lblRankHp.setText(hp);

        if (rowView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) rowView;
            card.setStrokeColor(ColorStateList.valueOf(android.graphics.Color.parseColor(strokeColorHex)));
            card.setStrokeWidth(3);
        }

        leaderboardContainer.addView(rowView);
    }
}
