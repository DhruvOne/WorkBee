package com.workbee.app.data.model;

import java.io.Serializable;
import java.util.Date;

public class Review implements Serializable {
    private String reviewId;
    private String bookingId;
    private String customerId;
    private String customerName;
    private String customerImageUrl;
    private String providerId;
    private double rating;
    private String comment;
    private Date createdAt;

    // Required empty constructor for Firestore
    public Review() {
        this.createdAt = new Date();
    }

    public Review(String reviewId, String bookingId, String customerId, String customerName, String providerId, double rating, String comment) {
        this();
        this.reviewId = reviewId;
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.providerId = providerId;
        this.rating = rating;
        this.comment = comment;
    }

    // Getters and Setters
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerImageUrl() { return customerImageUrl; }
    public void setCustomerImageUrl(String customerImageUrl) { this.customerImageUrl = customerImageUrl; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
