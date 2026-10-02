package com.workbee.app.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.workbee.app.R;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.Review;
import com.workbee.app.data.model.User;
import com.workbee.app.ui.adapter.ReviewAdapter;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProviderDetailsActivity extends BaseActivity {

    private Toolbar toolbar;
    private CircleImageView imgAvatar;
    private TextView txtBannerCategory, txtBusiness, txtName, txtRate, txtRating, txtExperience, txtJobs, txtBio;
    private RecyclerView recyclerReviews;
    private Button btnBookNow;

    private android.widget.LinearLayout layoutPortfolio, layoutVettingBadges;
    private TextView lblPortfolio, lblVettingBadges;
    private android.view.View scrollPortfolio, scrollVettingBadges;

    private String providerId;
    private Provider currentProvider;
    private ReviewAdapter reviewAdapter;
    private final List<Review> reviewList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_details);

        toolbar = findViewById(R.id.toolbar);
        imgAvatar = findViewById(R.id.img_provider);
        txtBannerCategory = findViewById(R.id.txt_banner_category);
        txtBusiness = findViewById(R.id.txt_business);
        txtName = findViewById(R.id.txt_name);
        txtRate = findViewById(R.id.txt_rate);
        txtRating = findViewById(R.id.txt_rating);
        txtExperience = findViewById(R.id.txt_experience);
        txtJobs = findViewById(R.id.txt_jobs);
        txtBio = findViewById(R.id.txt_bio);
        recyclerReviews = findViewById(R.id.recycler_reviews);
        btnBookNow = findViewById(R.id.btn_book_now);

        layoutPortfolio = findViewById(R.id.layout_portfolio);
        layoutVettingBadges = findViewById(R.id.layout_vetting_badges);
        lblPortfolio = findViewById(R.id.lbl_portfolio);
        lblVettingBadges = findViewById(R.id.lbl_vetting_badges);
        scrollPortfolio = findViewById(R.id.scroll_portfolio);
        scrollVettingBadges = findViewById(R.id.scroll_vetting_badges);

        providerId = getIntent().getStringExtra("provider_id");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerReviews.setLayoutManager(new LinearLayoutManager(this));

        btnBookNow.setOnClickListener(v -> {
            if (currentProvider != null) {
                Intent intent = new Intent(ProviderDetailsActivity.this, BookingActivity.class);
                intent.putExtra("provider_object", currentProvider);
                startActivity(intent);
            }
        });

        loadProviderDetails();
        loadReviews();
    }

    private void loadProviderDetails() {
        showProgress("Retrieving profile details...");
        repository.getProviderDetails(providerId, new FirebaseHelper.DataCallback<Provider>() {
            @Override
            public void onSuccess(Provider p) {
                hideProgress();
                currentProvider = p;

                txtBannerCategory.setText(p.getCategory().toUpperCase() + " EXPERT");
                txtBusiness.setText(p.getBusinessName());
                txtRate.setText(String.format(Locale.getDefault(), "₹%.0f", p.getHourlyRate()));
                txtRating.setText(String.format(Locale.getDefault(), "%.1f ★", p.getRating()));
                txtExperience.setText(p.getExperienceYears() + " Years");
                txtJobs.setText(p.getCompletedJobs() + " Completed");
                txtBio.setText(p.getBio());

                // Populate vetting badges
                if (p.getVettingBadges() != null && !p.getVettingBadges().isEmpty()) {
                    lblVettingBadges.setVisibility(android.view.View.VISIBLE);
                    scrollVettingBadges.setVisibility(android.view.View.VISIBLE);
                    layoutVettingBadges.removeAllViews();
                    for (String badge : p.getVettingBadges()) {
                        TextView chip = new TextView(ProviderDetailsActivity.this);
                        chip.setText(badge);
                        chip.setTextSize(11f);
                        chip.setTextColor(android.graphics.Color.BLACK);
                        chip.setPadding(24, 12, 24, 12);
                        
                        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                        gd.setColor(android.graphics.Color.parseColor("#FFC107"));
                        gd.setCornerRadius(24f);
                        chip.setBackground(gd);

                        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 12, 0);
                        chip.setLayoutParams(lp);
                        layoutVettingBadges.addView(chip);
                    }
                } else {
                    lblVettingBadges.setVisibility(android.view.View.GONE);
                    scrollVettingBadges.setVisibility(android.view.View.GONE);
                }

                // Populate portfolio images
                if (p.getPortfolioUrls() != null && !p.getPortfolioUrls().isEmpty()) {
                    lblPortfolio.setVisibility(android.view.View.VISIBLE);
                    scrollPortfolio.setVisibility(android.view.View.VISIBLE);
                    layoutPortfolio.removeAllViews();
                    for (String url : p.getPortfolioUrls()) {
                        com.google.android.material.imageview.ShapeableImageView imageView = new com.google.android.material.imageview.ShapeableImageView(ProviderDetailsActivity.this);
                        imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                        
                        // Style shape with rounded corners
                        imageView.setShapeAppearanceModel(
                            imageView.getShapeAppearanceModel().toBuilder()
                                .setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, 24f)
                                .build()
                        );

                        int widthHeight = (int) (140 * getResources().getDisplayMetrics().density);
                        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(widthHeight, widthHeight);
                        lp.setMargins(0, 0, 12, 0);
                        imageView.setLayoutParams(lp);

                        Glide.with(ProviderDetailsActivity.this)
                             .load(url)
                             .placeholder(R.drawable.ic_workbee_logo)
                             .into(imageView);

                        layoutPortfolio.addView(imageView);
                    }
                } else {
                    lblPortfolio.setVisibility(android.view.View.GONE);
                    scrollPortfolio.setVisibility(android.view.View.GONE);
                }

                // Fetch real name and profile picture
                repository.fetchUserProfile(p.getProviderId(), new FirebaseHelper.AuthCallback() {
                    @Override
                    public void onSuccess(User user) {
                        txtName.setText(user.getFullName());
                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            Glide.with(ProviderDetailsActivity.this)
                                 .load(user.getProfileImageUrl())
                                 .placeholder(R.drawable.ic_workbee_logo)
                                 .into(imgAvatar);
                        }
                    }

                    @Override
                    public void onFailure(String message) {}
                });
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                Toast.makeText(ProviderDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReviews() {
        repository.getReviews(providerId, new FirebaseHelper.ListCallback<Review>() {
            @Override
            public void onSuccess(List<Review> list) {
                reviewList.clear();
                reviewList.addAll(list);
                reviewAdapter = new ReviewAdapter(ProviderDetailsActivity.this, reviewList);
                recyclerReviews.setAdapter(reviewAdapter);
            }

            @Override
            public void onFailure(String message) {}
        });
    }
}
