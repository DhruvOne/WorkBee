package com.workbee.app.data.model;

import java.io.Serializable;
import java.util.Date;

public class Booking implements Serializable {
    private String bookingId;
    private String customerId;
    private String customerName;
    private String providerId;
    private String providerName;
    private String category;
    private String date; // YYYY-MM-DD
    private String timeSlot;
    private String description;
    private String status; // PENDING | ACCEPTED | REJECTED | ON_THE_WAY | IN_PROGRESS | COMPLETED | CANCELLED
    private double totalPrice;
    private String address;
    private double latitude;
    private double longitude;
    private boolean rated;
    private Date createdAt;
    private String paymentMethod; // CASH | ONLINE

    // Real-time tracking fields
    private double workerLatitude;          // Updated live by the worker's device
    private double workerLongitude;         // Updated live by the worker's device
    private long   workerLocationTimestamp; // Epoch ms of last update
    private String trackingStatus;          // IDLE | ACTIVE | COMPLETED

    // Required empty constructor for Firestore
    public Booking() {
        this.status = "PENDING";
        this.rated = false;
        this.createdAt = new Date();
        this.paymentMethod = "CASH";
        this.trackingStatus = "IDLE";
        this.workerLatitude = 0.0;
        this.workerLongitude = 0.0;
        this.workerLocationTimestamp = 0L;
    }

    public Booking(String bookingId, String customerId, String customerName, String providerId, String providerName,
                   String category, String date, String timeSlot, String description, double totalPrice, String address) {
        this();
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.providerId = providerId;
        this.providerName = providerName;
        this.category = category;
        this.date = date;
        this.timeSlot = timeSlot;
        this.description = description;
        this.totalPrice = totalPrice;
        this.address = address;
    }

    // Getters and Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isRated() { return rated; }
    public void setRated(boolean rated) { this.rated = rated; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    // Live tracking getters and setters
    public double getWorkerLatitude() { return workerLatitude; }
    public void setWorkerLatitude(double workerLatitude) { this.workerLatitude = workerLatitude; }

    public double getWorkerLongitude() { return workerLongitude; }
    public void setWorkerLongitude(double workerLongitude) { this.workerLongitude = workerLongitude; }

    public long getWorkerLocationTimestamp() { return workerLocationTimestamp; }
    public void setWorkerLocationTimestamp(long workerLocationTimestamp) { this.workerLocationTimestamp = workerLocationTimestamp; }

    public String getTrackingStatus() { return trackingStatus; }
    public void setTrackingStatus(String trackingStatus) { this.trackingStatus = trackingStatus; }

    private String promoCode;
    private double discountAmount;

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
}
