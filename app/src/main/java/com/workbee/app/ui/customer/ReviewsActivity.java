package com.workbee.app.ui.customer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import com.workbee.app.R;
import com.workbee.app.data.model.Booking;
import com.workbee.app.data.model.Review;
import com.workbee.app.ui.common.BaseActivity;
import com.workbee.app.utils.FirebaseHelper;

public class ReviewsActivity extends BaseActivity {

    private TextView txtProviderInfo;
    private RatingBar ratingBar;
    private EditText edtComment;
    private Button btnSubmit;

    private Booking booking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        txtProviderInfo = findViewById(R.id.txt_provider_info);
        ratingBar = findViewById(R.id.rating_bar);
        edtComment = findViewById(R.id.edt_comment);
        btnSubmit = findViewById(R.id.btn_submit_review);

        booking = (Booking) getIntent().getSerializableExtra("booking_object");

        if (booking != null) {
            txtProviderInfo.setText("How was your service with " + booking.getProviderName() + "?");
        }

        btnSubmit.setOnClickListener(v -> handleSubmitReview());
    }

    private void handleSubmitReview() {
        String comment = edtComment.getText().toString().trim();
        float rating = ratingBar.getRating();

        if (TextUtils.isEmpty(comment)) {
            edtComment.setError("Please write a short comment about the service.");
            return;
        }

        Review review = new Review(
                null,
                booking.getBookingId(),
                booking.getCustomerId(),
                booking.getCustomerName(),
                booking.getProviderId(),
                rating,
                comment
        );

        // Prepopulate customer image from active profile if present
        if (repository.getCurrentUserProfile() != null) {
            review.setCustomerImageUrl(repository.getCurrentUserProfile().getProfileImageUrl());
        }

        showProgress("Saving your verified feedback...");
        repository.addReview(review, new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                hideProgress();
                showToast("Thank you for your rating & review!");
                finish();
            }

            @Override
            public void onFailure(String message) {
                hideProgress();
                showToast("Failed to save review: " + message);
            }
        });
    }
}
