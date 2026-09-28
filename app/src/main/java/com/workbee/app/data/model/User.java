package com.workbee.app.data.model;

import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String role; // CUSTOMER | PROVIDER | ADMIN
    private String profileImageUrl;
    private Date createdAt;
    private boolean isActive;
    private String address;
    private double latitude;
    private double longitude;

    // Required empty constructor for Firestore
    public User() {
        this.createdAt = new Date();
        this.isActive = true;
    }

    public User(String userId, String fullName, String email, String phone, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.createdAt = new Date();
        this.isActive = true;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    private double walletBalance = 500.0;
    private boolean isGoldUser = false;

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }

    public boolean isGoldUser() { return isGoldUser; }
    public void setGoldUser(boolean goldUser) { isGoldUser = goldUser; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
