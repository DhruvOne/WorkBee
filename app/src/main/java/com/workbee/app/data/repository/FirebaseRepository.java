package com.workbee.app.data.repository;

import android.content.Context;

import com.workbee.app.data.model.Booking;
import com.workbee.app.data.model.Category;
import com.workbee.app.data.model.NotificationModel;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.Review;
import com.workbee.app.data.model.User;
import com.workbee.app.utils.FirebaseHelper;

public class FirebaseRepository {
    private final FirebaseHelper firebaseHelper;

    public FirebaseRepository(Context context) {
        this.firebaseHelper = FirebaseHelper.getInstance(context);
    }

    public boolean isMockMode() {
        return firebaseHelper.isMockMode();
    }

    public String getCurrentUserId() {
        return firebaseHelper.getCurrentUserId();
    }

    public void login(String email, String password, FirebaseHelper.AuthCallback callback) {
        firebaseHelper.login(email, password, callback);
    }

    public void register(User user, Provider provider, String password, FirebaseHelper.AuthCallback callback) {
        firebaseHelper.register(user, provider, password, callback);
    }

    public void logout() {
        firebaseHelper.logout();
    }

    public void fetchUserProfile(String uid, FirebaseHelper.AuthCallback callback) {
        firebaseHelper.fetchUserProfile(uid, callback);
    }

    public User getCurrentUserProfile() {
        return firebaseHelper.getCurrentUserProfile();
    }

    public void updateProfile(User user, FirebaseHelper.SimpleCallback callback) {
        firebaseHelper.updateProfile(user, callback);
    }

    public void getCategories(FirebaseHelper.ListCallback<Category> callback) {
        firebaseHelper.getCategories(callback);
    }

    public void getProviders(String category, FirebaseHelper.ListCallback<Provider> callback) {
        firebaseHelper.getProviders(category, callback);
    }

    public void getAllProviders(FirebaseHelper.ListCallback<Provider> callback) {
        firebaseHelper.getAllProviders(callback);
    }

    public void getProviderDetails(String providerId, FirebaseHelper.DataCallback<Provider> callback) {
        firebaseHelper.getProviderDetails(providerId, callback);
    }

    public void updateProviderDetails(Provider provider, FirebaseHelper.SimpleCallback callback) {
        firebaseHelper.updateProviderDetails(provider, callback);
    }

    public void createBooking(Booking booking, FirebaseHelper.SimpleCallback callback) {
        firebaseHelper.createBooking(booking, callback);
    }

    public void getCustomerBookings(String customerId, FirebaseHelper.ListCallback<Booking> callback) {
        firebaseHelper.getCustomerBookings(customerId, callback);
    }

    public void getProviderBookings(String providerId, FirebaseHelper.ListCallback<Booking> callback) {
        firebaseHelper.getProviderBookings(providerId, callback);
    }

    public void getAllBookings(FirebaseHelper.ListCallback<Booking> callback) {
        firebaseHelper.getAllBookings(callback);
    }

    public void updateBookingStatus(String bookingId, String status, FirebaseHelper.SimpleCallback callback) {
        firebaseHelper.updateBookingStatus(bookingId, status, callback);
    }

    public void getReviews(String providerId, FirebaseHelper.ListCallback<Review> callback) {
        firebaseHelper.getReviews(providerId, callback);
    }

    public void addReview(Review review, FirebaseHelper.SimpleCallback callback) {
        firebaseHelper.addReview(review, callback);
    }

    public void getNotifications(String userId, FirebaseHelper.ListCallback<NotificationModel> callback) {
        firebaseHelper.getNotifications(userId, callback);
    }

    public void getAllUsers(FirebaseHelper.ListCallback<User> callback) {
        firebaseHelper.getAllUsers(callback);
    }

    public void updateUserStatus(String userId, boolean active, FirebaseHelper.SimpleCallback callback) {
        firebaseHelper.updateUserStatus(userId, active, callback);
    }

    public void sendOtp(String phone, FirebaseHelper.SimpleCallback callback) {
        firebaseHelper.sendOtp(phone, callback);
    }

    public void loginWithPhone(String phone, String otp, FirebaseHelper.AuthCallback callback) {
        firebaseHelper.loginWithPhone(phone, otp, callback);
    }

    public boolean verifyOtpOnly(String otp) {
        return firebaseHelper.verifyOtpOnly(otp);
    }
}
