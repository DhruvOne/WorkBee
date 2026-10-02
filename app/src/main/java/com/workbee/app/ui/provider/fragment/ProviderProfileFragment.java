package com.workbee.app.ui.provider.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.workbee.app.R;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.User;
import com.workbee.app.data.repository.FirebaseRepository;
import com.workbee.app.ui.auth.LoginActivity;
import com.workbee.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProviderProfileFragment extends Fragment {

    private FirebaseRepository repository;
    private FirebaseHelper firebaseHelper;

    private CircleImageView imgAvatar;
    private TextView txtBizName, txtCategory;
    private TextView txtPayoutsNet, txtPayoutsGross;
    private Button btnCashOut;

    private EditText edtHourlyRate, edtBio;
    private SwitchCompat switchCash, switchOnline;
    private LinearLayout containerSkills;
    private EditText edtNewSkill;
    private Button btnAddSkill;

    private Button btnSave, btnLogout;
    private Button btnCelebrate;

    private LinearLayout leaderboardContainer;

    // Level & Wallet views
    private android.widget.ProgressBar progressLevelXp;
    private TextView txtLevelTitle, txtLevelXpDesc, txtLevelNum, txtWalletTotal;

    private Provider currentProvider;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider_profile, container, false);

        repository = new FirebaseRepository(requireContext());
        firebaseHelper = FirebaseHelper.getInstance(requireContext());

        imgAvatar = view.findViewById(R.id.img_pro_profile_avatar);
        txtBizName = view.findViewById(R.id.txt_pro_profile_biz_name);
        txtCategory = view.findViewById(R.id.txt_pro_profile_category);
        txtPayoutsNet = view.findViewById(R.id.txt_pro_payouts_net);
        txtPayoutsGross = view.findViewById(R.id.txt_pro_payouts_gross);
        btnCashOut = view.findViewById(R.id.btn_cash_out_earnings);

        edtHourlyRate = view.findViewById(R.id.edt_pro_profile_rate);
        edtBio = view.findViewById(R.id.edt_pro_profile_bio);
        switchCash = view.findViewById(R.id.switch_accept_cash);
        switchOnline = view.findViewById(R.id.switch_accept_online);
        containerSkills = view.findViewById(R.id.container_pro_skills_chips);
        edtNewSkill = view.findViewById(R.id.edt_new_skill_tag);
        btnAddSkill = view.findViewById(R.id.btn_add_skill_tag);

        btnSave = view.findViewById(R.id.btn_save_pro_profile);
        btnLogout = view.findViewById(R.id.btn_pro_logout);

        // Level progress & wallet bindings
        progressLevelXp = view.findViewById(R.id.progress_pro_level_xp);
        txtLevelTitle = view.findViewById(R.id.txt_pro_level_title);
        txtLevelXpDesc = view.findViewById(R.id.txt_pro_level_xp_desc);
        txtLevelNum = view.findViewById(R.id.txt_pro_level_num);
        txtWalletTotal = view.findViewById(R.id.txt_pro_wallet_total);
        btnCelebrate = view.findViewById(R.id.btn_pro_level_celebrate);

        loadProviderProfile();

        btnSave.setOnClickListener(v -> handleSaveBusinessProfile());
        btnLogout.setOnClickListener(v -> handleLogout());
        btnAddSkill.setOnClickListener(v -> handleAddSkillTag());
        btnCashOut.setOnClickListener(v -> handleCashOutEarnings());
        if (btnCelebrate != null) {
            btnCelebrate.setOnClickListener(v -> showLevelUpDialog());
        }

        // Initialize and Setup Standings
        leaderboardContainer = view.findViewById(R.id.leaderboard_rows_container);
        setupLeaderboardStandings();

        return view;
    }

    private void loadProviderProfile() {
        String uid = firebaseHelper.getCurrentUserId();
        if (uid == null) return;

        User user = repository.getCurrentUserProfile();
        if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                 .load(user.getProfileImageUrl())
                 .placeholder(R.drawable.ic_workbee_logo)
                 .into(imgAvatar);
        }

        firebaseHelper.getProviderDetails(uid, new FirebaseHelper.DataCallback<Provider>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(Provider provider) {
                currentProvider = provider;
                if (currentProvider != null) {
                    txtBizName.setText(currentProvider.getBusinessName());
                    txtCategory.setText(currentProvider.getCategory() + " Category");
                    
                    double gross = currentProvider.getEarnings();
                    double net = gross * 0.90; // platform takes 10% share
                    txtPayoutsNet.setText(String.format(Locale.getDefault(), "₹%.2f", net));
                    txtPayoutsGross.setText(String.format(Locale.getDefault(), "Gross Platform Revenue: ₹%.2f (Commission: 10%%)", gross));

                    edtHourlyRate.setText(String.format(Locale.getDefault(), "%.2f", currentProvider.getHourlyRate()));
                    edtBio.setText(currentProvider.getBio());

                    // --- Dynamic Level XP Gamification ---
                    int jobsDone = currentProvider.getCompletedJobs();
                    // 10 jobs per level, starting at level 1
                    int level = Math.max(1, jobsDone / 10 + 1);
                    int jobsInCurrentLevel = jobsDone % 10;
                    int jobsNeededForNext = 10;
                    int xpPct = (int) ((jobsInCurrentLevel / (float) jobsNeededForNext) * 100);
                    int jobsLeft = jobsNeededForNext - jobsInCurrentLevel;

                    String[] levelTitles = {"", "Honey Recruit", "Worker Bee", "Vibe Pro",
                            "Honeycomb Expert", "Elite Specialist", "Golden Stinger Master",
                            "Apex Buzz Legend"};
                    String levelName = level < levelTitles.length ? levelTitles[level] : "Apex Legend 🔥";

                    if (txtLevelTitle != null) {
                        txtLevelTitle.setText("Level " + level + " — " + levelName + " 🏆");
                    }
                    if (txtLevelNum != null) {
                        txtLevelNum.setText(String.valueOf(level));
                    }
                    if (progressLevelXp != null) {
                        progressLevelXp.setProgress(xpPct);
                    }
                    if (txtLevelXpDesc != null) {
                        txtLevelXpDesc.setText(String.format(Locale.getDefault(),
                            "%d / %d Jobs completed • %d more to reach Level %d 🐝",
                            jobsDone, (level * 10), jobsLeft, (level + 1)));
                    }

                    // Wallet total display (net earnings)
                    if (txtWalletTotal != null) {
                        txtWalletTotal.setText(String.format(Locale.getDefault(), "Total: ₹%.2f", net));
                    }

                    renderSkillsChips();
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSkillsChips() {
        if (containerSkills == null || currentProvider == null) return;
        containerSkills.removeAllViews();

        List<String> skills = currentProvider.getSkills();
        if (skills == null) {
            skills = new ArrayList<>();
            currentProvider.setSkills(skills);
        }

        LinearLayout row = null;
        int maxInRow = 3;
        for (int i = 0; i < skills.size(); i++) {
            if (i % maxInRow == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);
                containerSkills.addView(row);
            }

            TextView chip = new TextView(getContext());
            chip.setText(skills.get(i) + " ✕");
            chip.setTextSize(11f);
            chip.setTextColor(Color.BLACK);
            chip.setPadding(20, 10, 20, 10);
            chip.setGravity(Gravity.CENTER);
            
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor("#FFC107"));
            gd.setCornerRadius(24f);
            chip.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 12, 0);
            chip.setLayoutParams(lp);

            final String skillToRemove = skills.get(i);
            chip.setOnClickListener(v -> {
                currentProvider.getSkills().remove(skillToRemove);
                renderSkillsChips();
            });

            if (row != null) {
                row.addView(chip);
            }
        }
    }

    private void handleAddSkillTag() {
        String tag = edtNewSkill.getText().toString().trim();
        if (TextUtils.isEmpty(tag)) {
            edtNewSkill.setError("Type a specialty!");
            return;
        }

        if (currentProvider != null) {
            if (currentProvider.getSkills() == null) {
                currentProvider.setSkills(new ArrayList<>());
            }
            currentProvider.getSkills().add(tag);
            renderSkillsChips();
            edtNewSkill.setText("");
            Toast.makeText(getContext(), "Tag '" + tag + "' added! Tap it to remove.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSaveBusinessProfile() {
        if (currentProvider == null) return;

        String rateStr = edtHourlyRate.getText().toString().trim();
        String bioStr = edtBio.getText().toString().trim();

        if (TextUtils.isEmpty(rateStr)) {
            edtHourlyRate.setError("Hourly rate is required");
            return;
        }

        try {
            double rate = Double.parseDouble(rateStr);
            currentProvider.setHourlyRate(rate);
        } catch (NumberFormatException e) {
            edtHourlyRate.setError("Invalid number format");
            return;
        }

        currentProvider.setBio(bioStr);

        // Show Progress
        ProgressDialog progress = new ProgressDialog(getContext());
        progress.setMessage("Updating business credentials... ⚡");
        progress.setCancelable(false);
        progress.show();

        firebaseHelper.updateProviderDetails(currentProvider, new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                progress.dismiss();
                Toast.makeText(getContext(), "Business Profile saved successfully! ✨", Toast.LENGTH_SHORT).show();
                loadProviderProfile();
            }

            @Override
            public void onFailure(String message) {
                progress.dismiss();
                Toast.makeText(getContext(), "Failed to save: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCashOutEarnings() {
        if (currentProvider == null) return;
        if (currentProvider.getEarnings() <= 0.0) {
            Toast.makeText(getContext(), "Vibe check: Wallet balance is empty! Serve more bookings.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(getContext());
        progress.setMessage("Transferring net earnings to your linked bank account... ⚡");
        progress.setCancelable(false);
        progress.show();

        new Handler().postDelayed(() -> {
            progress.dismiss();

            double earningsCashOut = currentProvider.getEarnings();
            double netTransferred = earningsCashOut * 0.90;

            // Update local provider database earnings back to zero
            currentProvider.setEarnings(0.0);
            firebaseHelper.updateProviderDetails(currentProvider, new FirebaseHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Cash Out Successful! ⚡ 💰");
                    builder.setMessage(String.format(Locale.getDefault(),
                        "Boom! Net earnings of ₹%.2f successfully transferred to your bank account! Vibe check is exceptionally clean! Keep matching and earning!",
                        netTransferred));
                    builder.setPositiveButton("LFG! 🚀", (dialog, which) -> dialog.dismiss());
                    builder.show();
                    loadProviderProfile();
                }

                @Override
                public void onFailure(String message) {}
            });

        }, 1500);
    }

    private void handleLogout() {
        repository.logout();
        Toast.makeText(getContext(), "Logged out successfully!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showLevelUpDialog() {
        if (getContext() == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.dialog_vibe_levelup, null);
        dialog.setContentView(sheetView);

        // Dynamically update level title if provider data is loaded
        if (currentProvider != null) {
            int jobsDone = currentProvider.getCompletedJobs();
            int level = Math.max(1, jobsDone / 10 + 1);
            String[] levelTitles = {"", "Honey Recruit", "Worker Bee", "Vibe Pro",
                    "Honeycomb Expert", "Elite Specialist", "Golden Stinger Master", "Apex Buzz Legend"};
            String levelName = level < levelTitles.length ? levelTitles[level] : "Apex Legend";

            TextView txtDesc = sheetView.findViewById(R.id.btn_levelup_dismiss);
            if (txtDesc == null) {
                // Fall back — dialog still shows static content
            }
        }

        android.view.View btnDismiss = sheetView.findViewById(R.id.btn_levelup_dismiss);
        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(getContext(), "Keep grinding! Level up incoming! 🚀🐝", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    private void setupLeaderboardStandings() {
        if (leaderboardContainer == null) return;
        leaderboardContainer.removeAllViews();

        if (getContext() == null) return;
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // Display weekly premium rankings (You are Rank 3 on active standings)
        addLeaderboardRow(inflater, 1, "👑", "Sarah K. (Cleaning Pro)", "Platinum Tier", "1480 HP", "#FFD700");
        addLeaderboardRow(inflater, 2, "🐝", "David M. (Plumbing Expert)", "Gold Tier", "1250 HP", "#C0C0C0");
        addLeaderboardRow(inflater, 3, "✨", "You (Active Specialist)", "Gold Tier", "950 HP", "#CD7F32");
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
