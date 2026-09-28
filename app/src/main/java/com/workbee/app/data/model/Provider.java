package com.workbee.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Provider implements Serializable {
    private String providerId; // Same as User.userId
    private String businessName;
    private String category; // e.g. Plumber, Electrician
    private int experienceYears;
    private String bio;
    private double hourlyRate;
    private double rating;
    private int reviewCount;
    private boolean isApproved; // Admin approval
    private boolean isAvailable;
    private int completedJobs;
    private List<String> skills;
    private double earnings;

    // Required empty constructor for Firestore
    public Provider() {
        this.rating = 5.0;
        this.reviewCount = 0;
        this.isApproved = false; // Default requires admin approval
        this.isAvailable = true;
        this.completedJobs = 0;
        this.skills = new ArrayList<>();
        this.earnings = 0.0;
    }

    public Provider(String providerId, String businessName, String category, int experienceYears, double hourlyRate, String bio) {
        this();
        this.providerId = providerId;
        this.businessName = businessName;
        this.category = category;
        this.experienceYears = experienceYears;
        this.hourlyRate = hourlyRate;
        this.bio = bio;
    }

    // Getters and Setters
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public int getCompletedJobs() { return completedJobs; }
    public void setCompletedJobs(int completedJobs) { this.completedJobs = completedJobs; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    private List<String> portfolioUrls = new ArrayList<>();
    private List<String> vettingBadges = new ArrayList<>();

    public List<String> getPortfolioUrls() {
        if (portfolioUrls == null) portfolioUrls = new ArrayList<>();
        return portfolioUrls;
    }
    public void setPortfolioUrls(List<String> portfolioUrls) { this.portfolioUrls = portfolioUrls; }

    public List<String> getVettingBadges() {
        if (vettingBadges == null) vettingBadges = new ArrayList<>();
        return vettingBadges;
    }
    public void setVettingBadges(List<String> vettingBadges) { this.vettingBadges = vettingBadges; }

    public double getEarnings() { return earnings; }
    public void setEarnings(double earnings) { this.earnings = earnings; }
}
