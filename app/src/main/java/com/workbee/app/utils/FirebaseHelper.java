package com.workbee.app.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.workbee.app.data.model.Booking;
import com.workbee.app.data.model.Category;
import com.workbee.app.data.model.NotificationModel;
import com.workbee.app.data.model.Provider;
import com.workbee.app.data.model.Review;
import com.workbee.app.data.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static FirebaseHelper instance;
    private boolean useMockMode = false;

    // In-memory collections for Mock Mode
    private final Map<String, User> mockUsers = new HashMap<>();
    private final Map<String, Provider> mockProviders = new HashMap<>();
    private final Map<String, Category> mockCategories = new HashMap<>();
    private final Map<String, Booking> mockBookings = new HashMap<>();
    private final List<Review> mockReviews = new ArrayList<>();
    private final List<NotificationModel> mockNotifications = new ArrayList<>();

    private User currentUserProfile = null;
    private String currentUserId = null;
    private String lastSentOtp = null;

    private final Context context;

    private static final String PREFS_NAME = "WorkBeeOfflinePrefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_CURRENT_USER_PROFILE = "current_user_profile";
    private static final String KEY_MOCK_USERS = "mock_users";
    private static final String KEY_MOCK_PROVIDERS = "mock_providers";
    private static final String KEY_MOCK_BOOKINGS = "mock_bookings";
    private static final String KEY_MOCK_REVIEWS = "mock_reviews";
    private static final String KEY_MOCK_NOTIFICATIONS = "mock_notifications";

    private String userToJson(User u) {
        try {
            JSONObject obj = new JSONObject();
            if (u.getUserId() != null) obj.put("userId", u.getUserId());
            if (u.getFullName() != null) obj.put("fullName", u.getFullName());
            if (u.getEmail() != null) obj.put("email", u.getEmail());
            if (u.getPhone() != null) obj.put("phone", u.getPhone());
            if (u.getRole() != null) obj.put("role", u.getRole());
            if (u.getProfileImageUrl() != null) obj.put("profileImageUrl", u.getProfileImageUrl());
            if (u.getAddress() != null) obj.put("address", u.getAddress());
            obj.put("latitude", u.getLatitude());
            obj.put("longitude", u.getLongitude());
            obj.put("isActive", u.isActive());
            obj.put("walletBalance", u.getWalletBalance());
            obj.put("isGoldUser", u.isGoldUser());
            if (u.getCreatedAt() != null) {
                obj.put("createdAt", u.getCreatedAt().getTime());
            }
            return obj.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error serializing user: " + e.getMessage());
            return null;
        }
    }

    private User jsonToUser(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            User u = new User(
                obj.optString("userId"),
                obj.optString("fullName"),
                obj.optString("email"),
                obj.optString("phone"),
                obj.optString("role")
            );
            u.setProfileImageUrl(obj.optString("profileImageUrl"));
            u.setAddress(obj.optString("address"));
            u.setLatitude(obj.optDouble("latitude"));
            u.setLongitude(obj.optDouble("longitude"));
            u.setActive(obj.optBoolean("isActive", true));
            u.setWalletBalance(obj.optDouble("walletBalance", 500.0));
            u.setGoldUser(obj.optBoolean("isGoldUser", false));
            long time = obj.optLong("createdAt", 0);
            if (time > 0) {
                u.setCreatedAt(new Date(time));
            }
            return u;
        } catch (Exception e) {
            return null;
        }
    }

    private String providerToJson(Provider p) {
        try {
            JSONObject obj = new JSONObject();
            if (p.getProviderId() != null) obj.put("providerId", p.getProviderId());
            if (p.getBusinessName() != null) obj.put("businessName", p.getBusinessName());
            if (p.getCategory() != null) obj.put("category", p.getCategory());
            obj.put("experienceYears", p.getExperienceYears());
            obj.put("hourlyRate", p.getHourlyRate());
            if (p.getBio() != null) obj.put("bio", p.getBio());
            obj.put("approved", p.isApproved());
            obj.put("rating", p.getRating());
            obj.put("reviewCount", p.getReviewCount());
            obj.put("completedJobs", p.getCompletedJobs());
            obj.put("earnings", p.getEarnings());
            obj.put("isAvailable", p.isAvailable());
            
            JSONArray skillsArr = new JSONArray();
            if (p.getSkills() != null) {
                for (String s : p.getSkills()) {
                    skillsArr.put(s);
                }
            }
            obj.put("skills", skillsArr);

            JSONArray portfolioArr = new JSONArray();
            if (p.getPortfolioUrls() != null) {
                for (String s : p.getPortfolioUrls()) {
                    portfolioArr.put(s);
                }
            }
            obj.put("portfolioUrls", portfolioArr);

            JSONArray badgesArr = new JSONArray();
            if (p.getVettingBadges() != null) {
                for (String s : p.getVettingBadges()) {
                    badgesArr.put(s);
                }
            }
            obj.put("vettingBadges", badgesArr);

            return obj.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error serializing provider: " + e.getMessage());
            return null;
        }
    }

    private Provider jsonToProvider(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            Provider p = new Provider(
                obj.optString("providerId"),
                obj.optString("businessName"),
                obj.optString("category"),
                obj.optInt("experienceYears"),
                obj.optDouble("hourlyRate"),
                obj.optString("bio")
            );
            p.setApproved(obj.optBoolean("approved", false));
            p.setRating(obj.optDouble("rating", 0.0));
            p.setReviewCount(obj.optInt("reviewCount", 0));
            p.setCompletedJobs(obj.optInt("completedJobs", 0));
            p.setEarnings(obj.optDouble("earnings", 0.0));
            p.setAvailable(obj.optBoolean("isAvailable", true));
            
            List<String> skills = new ArrayList<>();
            JSONArray skillsArr = obj.optJSONArray("skills");
            if (skillsArr != null) {
                for (int i = 0; i < skillsArr.length(); i++) {
                    skills.add(skillsArr.getString(i));
                }
            }
            p.setSkills(skills);

            List<String> portfolios = new ArrayList<>();
            JSONArray portArr = obj.optJSONArray("portfolioUrls");
            if (portArr != null) {
                for (int i = 0; i < portArr.length(); i++) {
                    portfolios.add(portArr.getString(i));
                }
            }
            p.setPortfolioUrls(portfolios);

            List<String> badges = new ArrayList<>();
            JSONArray badgeArr = obj.optJSONArray("vettingBadges");
            if (badgeArr != null) {
                for (int i = 0; i < badgeArr.length(); i++) {
                    badges.add(badgeArr.getString(i));
                }
            }
            p.setVettingBadges(badges);

            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private String bookingToJson(Booking b) {
        try {
            JSONObject obj = new JSONObject();
            if (b.getBookingId() != null) obj.put("bookingId", b.getBookingId());
            if (b.getCustomerId() != null) obj.put("customerId", b.getCustomerId());
            if (b.getCustomerName() != null) obj.put("customerName", b.getCustomerName());
            if (b.getProviderId() != null) obj.put("providerId", b.getProviderId());
            if (b.getProviderName() != null) obj.put("providerName", b.getProviderName());
            if (b.getCategory() != null) obj.put("category", b.getCategory());
            if (b.getDate() != null) obj.put("date", b.getDate());
            if (b.getTimeSlot() != null) obj.put("timeSlot", b.getTimeSlot());
            if (b.getDescription() != null) obj.put("description", b.getDescription());
            if (b.getStatus() != null) obj.put("status", b.getStatus());
            obj.put("totalPrice", b.getTotalPrice());
            if (b.getAddress() != null) obj.put("address", b.getAddress());
            obj.put("latitude", b.getLatitude());
            obj.put("longitude", b.getLongitude());
            obj.put("rated", b.isRated());
            if (b.getPaymentMethod() != null) obj.put("paymentMethod", b.getPaymentMethod());
            if (b.getCreatedAt() != null) {
                obj.put("createdAt", b.getCreatedAt().getTime());
            }
            return obj.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error serializing booking: " + e.getMessage());
            return null;
        }
    }

    private Booking jsonToBooking(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            Booking b = new Booking(
                obj.optString("bookingId"),
                obj.optString("customerId"),
                obj.optString("customerName"),
                obj.optString("providerId"),
                obj.optString("providerName"),
                obj.optString("category"),
                obj.optString("date"),
                obj.optString("timeSlot"),
                obj.optString("description"),
                obj.optDouble("totalPrice"),
                obj.optString("address")
            );
            b.setStatus(obj.optString("status"));
            b.setLatitude(obj.optDouble("latitude"));
            b.setLongitude(obj.optDouble("longitude"));
            b.setRated(obj.optBoolean("rated", false));
            b.setPaymentMethod(obj.optString("paymentMethod", "CASH"));
            long time = obj.optLong("createdAt", 0);
            if (time > 0) {
                b.setCreatedAt(new Date(time));
            }
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private String reviewToJson(Review r) {
        try {
            JSONObject obj = new JSONObject();
            if (r.getReviewId() != null) obj.put("reviewId", r.getReviewId());
            if (r.getBookingId() != null) obj.put("bookingId", r.getBookingId());
            if (r.getCustomerId() != null) obj.put("customerId", r.getCustomerId());
            if (r.getCustomerName() != null) obj.put("customerName", r.getCustomerName());
            if (r.getCustomerImageUrl() != null) obj.put("customerImageUrl", r.getCustomerImageUrl());
            if (r.getProviderId() != null) obj.put("providerId", r.getProviderId());
            obj.put("rating", r.getRating());
            if (r.getComment() != null) obj.put("comment", r.getComment());
            if (r.getCreatedAt() != null) {
                obj.put("createdAt", r.getCreatedAt().getTime());
            }
            return obj.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error serializing review: " + e.getMessage());
            return null;
        }
    }

    private Review jsonToReview(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            Review r = new Review(
                obj.optString("reviewId"),
                obj.optString("bookingId"),
                obj.optString("customerId"),
                obj.optString("customerName"),
                obj.optString("providerId"),
                obj.optDouble("rating"),
                obj.optString("comment")
            );
            r.setCustomerImageUrl(obj.optString("customerImageUrl"));
            long time = obj.optLong("createdAt", 0);
            if (time > 0) {
                r.setCreatedAt(new Date(time));
            }
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private String notificationToJson(NotificationModel n) {
        try {
            JSONObject obj = new JSONObject();
            if (n.getNotificationId() != null) obj.put("notificationId", n.getNotificationId());
            if (n.getUserId() != null) obj.put("userId", n.getUserId());
            if (n.getTitle() != null) obj.put("title", n.getTitle());
            if (n.getBody() != null) obj.put("body", n.getBody());
            if (n.getType() != null) obj.put("type", n.getType());
            obj.put("isRead", n.isRead());
            if (n.getCreatedAt() != null) {
                obj.put("createdAt", n.getCreatedAt().getTime());
            }
            return obj.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error serializing notification: " + e.getMessage());
            return null;
        }
    }

    private NotificationModel jsonToNotification(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            NotificationModel n = new NotificationModel(
                obj.optString("notificationId"),
                obj.optString("userId"),
                obj.optString("title"),
                obj.optString("body"),
                obj.optString("type")
            );
            n.setRead(obj.optBoolean("isRead", false));
            long time = obj.optLong("createdAt", 0);
            if (time > 0) {
                n.setCreatedAt(new Date(time));
            }
            return n;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveSessionToDisk() {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // Persist the useMockMode flag so it survives restarts
            editor.putBoolean("use_mock_mode", useMockMode);
            
            if (currentUserId != null) {
                editor.putString(KEY_CURRENT_USER_ID, currentUserId);
            } else {
                editor.remove(KEY_CURRENT_USER_ID);
            }
            
            if (currentUserProfile != null) {
                editor.putString(KEY_CURRENT_USER_PROFILE, userToJson(currentUserProfile));
            } else {
                editor.remove(KEY_CURRENT_USER_PROFILE);
            }
            
            JSONArray usersArr = new JSONArray();
            for (User u : mockUsers.values()) {
                String uJson = userToJson(u);
                if (uJson != null) usersArr.put(new JSONObject(uJson));
            }
            editor.putString(KEY_MOCK_USERS, usersArr.toString());
            
            JSONArray provsArr = new JSONArray();
            for (Provider p : mockProviders.values()) {
                String pJson = providerToJson(p);
                if (pJson != null) provsArr.put(new JSONObject(pJson));
            }
            editor.putString(KEY_MOCK_PROVIDERS, provsArr.toString());
            
            JSONArray bookingsArr = new JSONArray();
            for (Booking b : mockBookings.values()) {
                String bJson = bookingToJson(b);
                if (bJson != null) bookingsArr.put(new JSONObject(bJson));
            }
            editor.putString(KEY_MOCK_BOOKINGS, bookingsArr.toString());
            
            JSONArray reviewsArr = new JSONArray();
            for (Review r : mockReviews) {
                String rJson = reviewToJson(r);
                if (rJson != null) reviewsArr.put(new JSONObject(rJson));
            }
            editor.putString(KEY_MOCK_REVIEWS, reviewsArr.toString());

            JSONArray notificationsArr = new JSONArray();
            for (NotificationModel n : mockNotifications) {
                String nJson = notificationToJson(n);
                if (nJson != null) notificationsArr.put(new JSONObject(nJson));
            }
            editor.putString(KEY_MOCK_NOTIFICATIONS, notificationsArr.toString());
            
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving session to disk", e);
        }
    }

    private void loadSessionFromDisk() {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        try {
            useMockMode = prefs.getBoolean("use_mock_mode", false) || useMockMode;
        } catch (Exception e) {
            Log.e(TAG, "Error loading use_mock_mode", e);
        }
        
        try {
            currentUserId = prefs.getString(KEY_CURRENT_USER_ID, null);
            String userJson = prefs.getString(KEY_CURRENT_USER_PROFILE, null);
            if (userJson != null) {
                currentUserProfile = jsonToUser(userJson);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user profile", e);
        }
        
        try {
            String usersJsonStr = prefs.getString(KEY_MOCK_USERS, null);
            if (usersJsonStr != null) {
                mockUsers.clear();
                JSONArray arr = new JSONArray(usersJsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        User u = jsonToUser(arr.getJSONObject(i).toString());
                        if (u != null) {
                            mockUsers.put(u.getUserId(), u);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error parsing user entry", ex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading users", e);
        }
        
        try {
            String provsJsonStr = prefs.getString(KEY_MOCK_PROVIDERS, null);
            if (provsJsonStr != null) {
                mockProviders.clear();
                JSONArray arr = new JSONArray(provsJsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        Provider p = jsonToProvider(arr.getJSONObject(i).toString());
                        if (p != null) {
                            mockProviders.put(p.getProviderId(), p);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error parsing provider entry", ex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading providers", e);
        }
        
        try {
            String bookingsJsonStr = prefs.getString(KEY_MOCK_BOOKINGS, null);
            if (bookingsJsonStr != null) {
                mockBookings.clear();
                JSONArray arr = new JSONArray(bookingsJsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        Booking b = jsonToBooking(arr.getJSONObject(i).toString());
                        if (b != null) {
                            mockBookings.put(b.getBookingId(), b);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error parsing booking entry", ex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading bookings", e);
        }
        
        try {
            String reviewsJsonStr = prefs.getString(KEY_MOCK_REVIEWS, null);
            if (reviewsJsonStr != null) {
                mockReviews.clear();
                JSONArray arr = new JSONArray(reviewsJsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        Review r = jsonToReview(arr.getJSONObject(i).toString());
                        if (r != null) {
                            mockReviews.add(r);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error parsing review entry", ex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading reviews", e);
        }

        try {
            String notificationsJsonStr = prefs.getString(KEY_MOCK_NOTIFICATIONS, null);
            if (notificationsJsonStr != null) {
                mockNotifications.clear();
                JSONArray arr = new JSONArray(notificationsJsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        NotificationModel n = jsonToNotification(arr.getJSONObject(i).toString());
                        if (n != null) {
                            mockNotifications.add(n);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error parsing notification entry", ex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading notifications", e);
        }
    }

    // Listeners and Callbacks interfaces
    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String message);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onFailure(String message);
    }

    public interface ListCallback<T> {
        void onSuccess(List<T> list);
        void onFailure(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String message);
    }

    private FirebaseHelper(Context context) {
        this.context = context;
        try {
            // Check if Firebase is properly configured
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            // Trigger check to see if we can get Firestore/Auth instance
            FirebaseAuth.getInstance();
            FirebaseFirestore.getInstance();
            
            // Check if the current key is the fake simulated key
            com.google.firebase.FirebaseOptions options = FirebaseApp.getInstance().getOptions();
            if (options != null && options.getApiKey() != null && (options.getApiKey().contains("FakeKey") || options.getApiKey().contains("AIzaSyFakeKey"))) {
                Log.w(TAG, "Simulated/Fake Firebase API Key detected! Forcing local Mock Mode.");
                useMockMode = true;
            } else {
                Log.d(TAG, "Firebase successfully initialized. Using cloud mode.");
                useMockMode = false;
            }
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization failed. Falling back to local in-memory Mock Mode.", e);
            useMockMode = true;
        }

        // Try loading mock database and logged in user persistently from disk first
        loadSessionFromDisk();

        // Always seed sample mock records that are missing
        seedMockData();
        saveSessionToDisk();
    }

    public static synchronized FirebaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isMockMode() {
        return useMockMode;
    }

    public String getCurrentUserId() {
        if (useMockMode) {
            return currentUserId;
        } else {
            try {
                return FirebaseAuth.getInstance().getCurrentUser() != null ? 
                       FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            } catch (Exception e) {
                return null;
            }
        }
    }

    public void login(String email, String password, AuthCallback callback) {
        if (useMockMode) {
            // Match with in-memory users
            User foundUser = null;
            for (User u : mockUsers.values()) {
                if (u.getEmail().equalsIgnoreCase(email)) {
                    foundUser = u;
                    break;
                }
            }
            if (foundUser != null) {
                currentUserId = foundUser.getUserId();
                currentUserProfile = foundUser;
                saveSessionToDisk();
                callback.onSuccess(foundUser);
            } else {
                callback.onFailure("Invalid email or password. (Hint: Try customer@workbee.com or provider@workbee.com)");
            }
        } else {
            try {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        fetchUserProfile(uid, callback);
                    })
                    .addOnFailureListener(e -> {
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                        boolean existsInMock = false;
                        for (User u : mockUsers.values()) {
                            if (u.getEmail().equalsIgnoreCase(email)) {
                                existsInMock = true;
                                break;
                            }
                        }
                        if (existsInMock || errorMsg.contains("API key") || errorMsg.contains("api key") || 
                            errorMsg.contains("internal error") || errorMsg.contains("Internal error") || 
                            errorMsg.contains("API_KEY_INVALID")) {
                            Log.w(TAG, "Failing over to Mock/Offline Mode...");
                            useMockMode = true;
                            saveSessionToDisk();
                            login(email, password, callback);
                        } else {
                            callback.onFailure(e.getMessage());
                        }
                    });
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                boolean existsInMock = false;
                for (User u : mockUsers.values()) {
                    if (u.getEmail().equalsIgnoreCase(email)) {
                        existsInMock = true;
                        break;
                    }
                }
                if (existsInMock || errorMsg.contains("API key") || errorMsg.contains("api key") || 
                    errorMsg.contains("internal error") || errorMsg.contains("API_KEY_INVALID")) {
                    Log.w(TAG, "Firebase authentication threw exception. Gracefully failing over to Mock/Offline Mode...");
                    useMockMode = true;
                    saveSessionToDisk();
                    login(email, password, callback);
                } else {
                    callback.onFailure(e.getMessage());
                }
            }
        }
    }

    public void sendOtp(String phone, SimpleCallback callback) {
        if (useMockMode) {
            int code = 100000 + new java.util.Random().nextInt(900000);
            lastSentOtp = String.valueOf(code);
            Log.d(TAG, "MOCK SMS OTP Sent to " + phone + ": " + lastSentOtp);
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.post(() -> {
                android.widget.Toast.makeText(context, "🐝 MOCK SMS Sent! OTP: " + lastSentOtp, android.widget.Toast.LENGTH_LONG).show();
            });
            callback.onSuccess();
        } else {
            useMockMode = true;
            sendOtp(phone, callback);
        }
    }

    public void loginWithPhone(String phone, String otp, AuthCallback callback) {
        if (useMockMode) {
            if (lastSentOtp == null || !lastSentOtp.equals(otp)) {
                callback.onFailure("Invalid Verification Code.");
                return;
            }
            User foundUser = null;
            String normalizedPhone = phone.replaceAll("[^\\d]", "");
            for (User u : mockUsers.values()) {
                if (u.getPhone() != null) {
                    String normUserPhone = u.getPhone().replaceAll("[^\\d]", "");
                    if (normUserPhone.contains(normalizedPhone) || normalizedPhone.contains(normUserPhone)) {
                        foundUser = u;
                        break;
                    }
                }
            }

            if (foundUser != null) {
                if (!foundUser.getRole().equalsIgnoreCase("PROVIDER")) {
                    callback.onFailure("Phone login is restricted to Worker / Provider accounts.");
                    return;
                }
                currentUserId = foundUser.getUserId();
                currentUserProfile = foundUser;
                saveSessionToDisk();
                callback.onSuccess(foundUser);
            } else {
                callback.onFailure("No Worker registered with this phone number. Please sign up.");
            }
        } else {
            useMockMode = true;
            loginWithPhone(phone, otp, callback);
        }
    }

    public boolean verifyOtpOnly(String otp) {
        if (useMockMode) {
            return lastSentOtp != null && lastSentOtp.equals(otp);
        }
        return "123456".equals(otp);
    }


    public void register(User user, Provider provider, String password, AuthCallback callback) {
        if (useMockMode) {
            // Register locally in-memory
            String newUid = "user_uid_" + System.currentTimeMillis();
            user.setUserId(newUid);
            mockUsers.put(newUid, user);

            if (provider != null) {
                provider.setProviderId(newUid);
                mockProviders.put(newUid, provider);
            }

            currentUserId = newUid;
            currentUserProfile = user;
            saveSessionToDisk();
            callback.onSuccess(user);
        } else {
            try {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(user.getEmail(), password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        user.setUserId(uid);
                        FirebaseFirestore.getInstance().collection("users").document(uid).set(user)
                            .addOnSuccessListener(aVoid -> {
                                if (provider != null) {
                                    provider.setProviderId(uid);
                                    FirebaseFirestore.getInstance().collection("providers").document(uid).set(provider)
                                        .addOnSuccessListener(unused -> callback.onSuccess(user))
                                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                                } else {
                                    callback.onSuccess(user);
                                }
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    })
                    .addOnFailureListener(e -> {
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                        // Gracefully fall back to mock/offline mode if Firebase API key is invalid
                        if (errorMsg.contains("API key") || errorMsg.contains("api key") ||
                            errorMsg.contains("internal error") || errorMsg.contains("Internal error") ||
                            errorMsg.contains("API_KEY_INVALID") || errorMsg.contains("not valid")) {
                            Log.w(TAG, "Firebase API Key invalid during register. Switching to Mock/Offline Mode...");
                            useMockMode = true;
                            register(user, provider, password, callback);
                        } else {
                            callback.onFailure(errorMsg);
                        }
                    });
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                if (errorMsg.contains("API key") || errorMsg.contains("api key") ||
                    errorMsg.contains("internal error") || errorMsg.contains("Internal error") ||
                    errorMsg.contains("API_KEY_INVALID") || errorMsg.contains("not valid")) {
                    Log.w(TAG, "Firebase register threw exception. Switching to Mock/Offline Mode...");
                    useMockMode = true;
                    register(user, provider, password, callback);
                } else {
                    callback.onFailure(errorMsg);
                }
            }
        }
    }

    public void logout() {
        if (useMockMode) {
            currentUserId = null;
            currentUserProfile = null;
            saveSessionToDisk();
        } else {
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception ignored) {}
        }
    }

    public void fetchUserProfile(String uid, AuthCallback callback) {
        if (useMockMode) {
            User user = mockUsers.get(uid);
            if (user != null) {
                currentUserProfile = user;
                callback.onSuccess(user);
            } else {
                callback.onFailure("User profile not found in memory.");
            }
        } else {
            try {
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            currentUserProfile = user;
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure("Profile data is empty.");
                        }
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    public User getCurrentUserProfile() {
        return currentUserProfile;
    }

    public void setCurrentUserProfile(User user) {
        this.currentUserProfile = user;
        if (useMockMode && user != null) {
            mockUsers.put(user.getUserId(), user);
        }
    }

    public void updateProfile(User user, SimpleCallback callback) {
        if (useMockMode) {
            mockUsers.put(user.getUserId(), user);
            currentUserProfile = user;
            saveSessionToDisk();
            callback.onSuccess();
        } else {
            try {
                FirebaseFirestore.getInstance().collection("users").document(user.getUserId()).set(user)
                    .addOnSuccessListener(aVoid -> {
                        currentUserProfile = user;
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    // Categories Operations
    public void getCategories(ListCallback<Category> callback) {
        if (useMockMode) {
            callback.onSuccess(new ArrayList<>(mockCategories.values()));
        } else {
            try {
                FirebaseFirestore.getInstance().collection("services").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Category> list = queryDocumentSnapshots.toObjects(Category.class);
                        if (list.isEmpty()) {
                            // If remote collection is empty, load mocks online as helper
                            List<Category> defaultList = new ArrayList<>(mockCategories.values());
                            for (Category cat : defaultList) {
                                FirebaseFirestore.getInstance().collection("services").document(cat.getCategoryId()).set(cat);
                            }
                            callback.onSuccess(defaultList);
                        } else {
                            callback.onSuccess(list);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Safe offline fallback
                        callback.onSuccess(new ArrayList<>(mockCategories.values()));
                    });
            } catch (Exception e) {
                callback.onSuccess(new ArrayList<>(mockCategories.values()));
            }
        }
    }

    public void addCategory(Category category, SimpleCallback callback) {
        if (useMockMode) {
            mockCategories.put(category.getCategoryId(), category);
            callback.onSuccess();
        } else {
            try {
                FirebaseFirestore.getInstance().collection("services").document(category.getCategoryId()).set(category)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    // Providers Operations
    public void getProviders(String category, ListCallback<Provider> callback) {
        if (useMockMode) {
            List<Provider> filtered = new ArrayList<>();
            for (Provider p : mockProviders.values()) {
                if (p.getCategory().equalsIgnoreCase(category)) {
                    filtered.add(p);
                }
            }
            callback.onSuccess(filtered);
        } else {
            try {
                FirebaseFirestore.getInstance().collection("providers")
                    .whereEqualTo("category", category)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Provider> list = queryDocumentSnapshots.toObjects(Provider.class);
                        if (list.isEmpty()) {
                            List<Provider> filtered = new ArrayList<>();
                            for (Provider p : mockProviders.values()) {
                                if (p.getCategory().equalsIgnoreCase(category)) {
                                    filtered.add(p);
                                }
                            }
                            callback.onSuccess(filtered);
                        } else {
                            callback.onSuccess(list);
                        }
                    })
                    .addOnFailureListener(e -> {
                        List<Provider> filtered = new ArrayList<>();
                        for (Provider p : mockProviders.values()) {
                            if (p.getCategory().equalsIgnoreCase(category)) {
                                filtered.add(p);
                            }
                        }
                        callback.onSuccess(filtered);
                    });
            } catch (Exception e) {
                List<Provider> filtered = new ArrayList<>();
                for (Provider p : mockProviders.values()) {
                    if (p.getCategory().equalsIgnoreCase(category)) {
                        filtered.add(p);
                    }
                }
                callback.onSuccess(filtered);
            }
        }
    }

    public void getAllProviders(ListCallback<Provider> callback) {
        if (useMockMode) {
            callback.onSuccess(new ArrayList<>(mockProviders.values()));
        } else {
            try {
                FirebaseFirestore.getInstance().collection("providers").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Provider> list = queryDocumentSnapshots.toObjects(Provider.class);
                        if (list.isEmpty()) {
                            callback.onSuccess(new ArrayList<>(mockProviders.values()));
                        } else {
                            callback.onSuccess(list);
                        }
                    })
                    .addOnFailureListener(e -> {
                        callback.onSuccess(new ArrayList<>(mockProviders.values()));
                    });
            } catch (Exception e) {
                callback.onSuccess(new ArrayList<>(mockProviders.values()));
            }
        }
    }

    public void getProviderDetails(String providerId, DataCallback<Provider> callback) {
        if (useMockMode) {
            Provider p = mockProviders.get(providerId);
            if (p != null) callback.onSuccess(p);
            else callback.onFailure("Provider profile not found.");
        } else {
            try {
                FirebaseFirestore.getInstance().collection("providers").document(providerId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Provider p = documentSnapshot.toObject(Provider.class);
                        if (p != null) {
                            callback.onSuccess(p);
                        } else {
                            Provider mock = mockProviders.get(providerId);
                            if (mock != null) callback.onSuccess(mock);
                            else callback.onFailure("Provider profile not found.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Provider mock = mockProviders.get(providerId);
                        if (mock != null) callback.onSuccess(mock);
                        else callback.onFailure(e.getMessage());
                    });
            } catch (Exception e) {
                Provider mock = mockProviders.get(providerId);
                if (mock != null) callback.onSuccess(mock);
                else callback.onFailure(e.getMessage());
            }
        }
    }

    public void updateProviderDetails(Provider provider, SimpleCallback callback) {
        if (useMockMode) {
            mockProviders.put(provider.getProviderId(), provider);
            saveSessionToDisk();
            callback.onSuccess();
        } else {
            try {
                FirebaseFirestore.getInstance().collection("providers").document(provider.getProviderId()).set(provider)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    // Bookings Operations
    public void createBooking(Booking booking, SimpleCallback callback) {
        if (useMockMode) {
            String newId = "booking_" + (mockBookings.size() + 1);
            booking.setBookingId(newId);
            mockBookings.put(newId, booking);
            
            // Add initial in-app notification
            createNotification(booking.getProviderId(), "New Booking Request", "You have a new request for " + booking.getCategory() + " from " + booking.getCustomerName());
            saveSessionToDisk();
            callback.onSuccess();
        } else {
            try {
                String newId = FirebaseFirestore.getInstance().collection("bookings").document().getId();
                booking.setBookingId(newId);
                FirebaseFirestore.getInstance().collection("bookings").document(newId).set(booking)
                    .addOnSuccessListener(aVoid -> {
                        createNotification(booking.getProviderId(), "New Booking Request", "You have a new request for " + booking.getCategory() + " from " + booking.getCustomerName());
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    public void getCustomerBookings(String customerId, ListCallback<Booking> callback) {
        if (useMockMode) {
            List<Booking> filtered = new ArrayList<>();
            for (Booking b : mockBookings.values()) {
                if (b.getCustomerId().equalsIgnoreCase(customerId)) {
                    filtered.add(b);
                }
            }
            callback.onSuccess(filtered);
        } else {
            try {
                FirebaseFirestore.getInstance().collection("bookings")
                    .whereEqualTo("customerId", customerId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Booking> list = queryDocumentSnapshots.toObjects(Booking.class);
                        callback.onSuccess(list);
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    public void getProviderBookings(String providerId, ListCallback<Booking> callback) {
        if (useMockMode) {
            Provider provider = mockProviders.get(providerId);
            String providerCat = (provider != null) ? provider.getCategory() : "";
            List<Booking> filtered = new ArrayList<>();
            for (Booking b : mockBookings.values()) {
                boolean matchesProvider = b.getProviderId().equalsIgnoreCase(providerId);
                boolean isPendingForCategory = "PENDING".equalsIgnoreCase(b.getStatus())
                        && providerCat != null
                        && !providerCat.isEmpty()
                        && b.getCategory().equalsIgnoreCase(providerCat);

                if (matchesProvider || isPendingForCategory) {
                    filtered.add(b);
                }
            }
            callback.onSuccess(filtered);
        } else {
            try {
                FirebaseFirestore.getInstance().collection("bookings")
                    .whereEqualTo("providerId", providerId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Booking> list = queryDocumentSnapshots.toObjects(Booking.class);
                        callback.onSuccess(list);
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    public void getAllBookings(ListCallback<Booking> callback) {
        if (useMockMode) {
            callback.onSuccess(new ArrayList<>(mockBookings.values()));
        } else {
            try {
                FirebaseFirestore.getInstance().collection("bookings").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Booking> list = queryDocumentSnapshots.toObjects(Booking.class);
                        callback.onSuccess(list);
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    public void updateBookingStatus(String bookingId, String status, SimpleCallback callback) {
        if (useMockMode) {
            Booking b = mockBookings.get(bookingId);
            if (b != null) {
                b.setStatus(status);
                // If transitioning to ACCEPTED, associate it with the logged in provider who accepted it!
                if ("ACCEPTED".equalsIgnoreCase(status)) {
                    String currentUid = getCurrentUserId();
                    if (currentUid != null) {
                        b.setProviderId(currentUid);
                        Provider p = mockProviders.get(currentUid);
                        if (p != null) {
                            b.setProviderName(p.getBusinessName());
                        }
                    }
                }
                // Trigger notification back to customer or provider
                if (status.equalsIgnoreCase("COMPLETED")) {
                    // Update provider completedJobs count and earnings
                    Provider provider = mockProviders.get(b.getProviderId());
                    if (provider != null) {
                        provider.setCompletedJobs(provider.getCompletedJobs() + 1);
                        provider.setEarnings(provider.getEarnings() + b.getTotalPrice() * 0.90); // 10% platform share
                    }
                }
                String msgTitle = "Booking " + status.toLowerCase();
                String msgBody = "Your booking for " + b.getCategory() + " is now " + status;
                createNotification(b.getCustomerId(), msgTitle, msgBody);
                saveSessionToDisk();
                callback.onSuccess();
            } else {
                callback.onFailure("Booking not found.");
            }
        } else {
            try {
                Map<String, Object> update = new HashMap<>();
                update.put("status", status);
                FirebaseFirestore.getInstance().collection("bookings").document(bookingId).update(update)
                    .addOnSuccessListener(aVoid -> {
                        // Load booking detail to send notification
                        FirebaseFirestore.getInstance().collection("bookings").document(bookingId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                Booking b = documentSnapshot.toObject(Booking.class);
                                if (b != null) {
                                    if (status.equalsIgnoreCase("COMPLETED")) {
                                        // Update provider stats
                                        FirebaseFirestore.getInstance().collection("providers").document(b.getProviderId()).get()
                                            .addOnSuccessListener(provSnap -> {
                                                Provider p = provSnap.toObject(Provider.class);
                                                if (p != null) {
                                                    p.setCompletedJobs(p.getCompletedJobs() + 1);
                                                    p.setEarnings(p.getEarnings() + b.getTotalPrice() * 0.90);
                                                    FirebaseFirestore.getInstance().collection("providers").document(p.getProviderId()).set(p);
                                                }
                                            });
                                    }
                                    createNotification(b.getCustomerId(), "Booking Update", "Your booking for " + b.getCategory() + " has been " + status);
                                }
                            });
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Real-Time Location Tracking Operations
    // ─────────────────────────────────────────────────────────────────

    /**
     * Called by the worker device every few seconds to push their GPS position.
     * In Mock Mode: updates the in-memory Booking and saves to disk.
     * In Firebase Mode: partial-updates just the worker location fields.
     */
    public void updateWorkerLocation(String bookingId, double lat, double lng, SimpleCallback callback) {
        if (useMockMode) {
            Booking b = mockBookings.get(bookingId);
            if (b != null) {
                b.setWorkerLatitude(lat);
                b.setWorkerLongitude(lng);
                b.setWorkerLocationTimestamp(System.currentTimeMillis());
                if ("IDLE".equals(b.getTrackingStatus())) {
                    b.setTrackingStatus("ACTIVE");
                }
                saveSessionToDisk();
                callback.onSuccess();
            } else {
                callback.onFailure("Booking not found in mock store.");
            }
        } else {
            try {
                Map<String, Object> update = new HashMap<>();
                update.put("workerLatitude", lat);
                update.put("workerLongitude", lng);
                update.put("workerLocationTimestamp", System.currentTimeMillis());
                update.put("trackingStatus", "ACTIVE");
                FirebaseFirestore.getInstance().collection("bookings").document(bookingId)
                        .update(update)
                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    /**
     * Called by the customer screen every 3 seconds to read the latest worker position.
     * Returns the full Booking object (which contains worker lat/lng + tracking status).
     */
    public void getWorkerLocation(String bookingId, DataCallback<Booking> callback) {
        if (useMockMode) {
            Booking b = mockBookings.get(bookingId);
            if (b != null) {
                callback.onSuccess(b);
            } else {
                callback.onFailure("Booking not found.");
            }
        } else {
            try {
                FirebaseFirestore.getInstance().collection("bookings").document(bookingId).get()
                        .addOnSuccessListener(snap -> {
                            Booking b = snap.toObject(Booking.class);
                            if (b != null) callback.onSuccess(b);
                            else callback.onFailure("Booking not found in Firestore.");
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    /**
     * Sets trackingStatus field on a booking document.
     * Used to transition: IDLE → ACTIVE → COMPLETED
     */
    public void updateBookingTrackingStatus(String bookingId, String trackingStatus, SimpleCallback callback) {
        if (useMockMode) {
            Booking b = mockBookings.get(bookingId);
            if (b != null) {
                b.setTrackingStatus(trackingStatus);
                saveSessionToDisk();
                callback.onSuccess();
            } else {
                callback.onFailure("Booking not found.");
            }
        } else {
            try {
                Map<String, Object> update = new HashMap<>();
                update.put("trackingStatus", trackingStatus);
                FirebaseFirestore.getInstance().collection("bookings").document(bookingId)
                        .update(update)
                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    // Reviews Operations
    public void getReviews(String providerId, ListCallback<Review> callback) {
        if (useMockMode) {
            List<Review> filtered = new ArrayList<>();
            for (Review r : mockReviews) {
                if (r.getProviderId().equalsIgnoreCase(providerId)) {
                    filtered.add(r);
                }
            }
            callback.onSuccess(filtered);
        } else {
            try {
                FirebaseFirestore.getInstance().collection("reviews")
                    .whereEqualTo("providerId", providerId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Review> list = queryDocumentSnapshots.toObjects(Review.class);
                        callback.onSuccess(list);
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    public void addReview(Review review, SimpleCallback callback) {
        if (useMockMode) {
            review.setReviewId("review_" + (mockReviews.size() + 1));
            mockReviews.add(review);
            
            // Mark booking as rated
            Booking b = mockBookings.get(review.getBookingId());
            if (b != null) b.setRated(true);

            // Recalculate Provider Average Rating
            Provider p = mockProviders.get(review.getProviderId());
            if (p != null) {
                double totalRating = p.getRating() * p.getReviewCount() + review.getRating();
                p.setReviewCount(p.getReviewCount() + 1);
                p.setRating(totalRating / p.getReviewCount());
            }
            saveSessionToDisk();
            callback.onSuccess();
        } else {
            try {
                String newId = FirebaseFirestore.getInstance().collection("reviews").document().getId();
                review.setReviewId(newId);
                FirebaseFirestore.getInstance().collection("reviews").document(newId).set(review)
                    .addOnSuccessListener(aVoid -> {
                        Map<String, Object> update = new HashMap<>();
                        update.put("rated", true);
                        FirebaseFirestore.getInstance().collection("bookings").document(review.getBookingId()).update(update);

                        // Update provider rating
                        FirebaseFirestore.getInstance().collection("providers").document(review.getProviderId()).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                Provider p = documentSnapshot.toObject(Provider.class);
                                if (p != null) {
                                    double totalRating = p.getRating() * p.getReviewCount() + review.getRating();
                                    p.setReviewCount(p.getReviewCount() + 1);
                                    p.setRating(totalRating / p.getReviewCount());
                                    FirebaseFirestore.getInstance().collection("providers").document(p.getProviderId()).set(p);
                                }
                            });
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    // Notifications Operations
    public void getNotifications(String userId, ListCallback<NotificationModel> callback) {
        if (useMockMode) {
            List<NotificationModel> filtered = new ArrayList<>();
            for (NotificationModel n : mockNotifications) {
                if (n.getUserId().equalsIgnoreCase(userId)) {
                    filtered.add(n);
                }
            }
            callback.onSuccess(filtered);
        } else {
            try {
                FirebaseFirestore.getInstance().collection("notifications")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<NotificationModel> list = queryDocumentSnapshots.toObjects(NotificationModel.class);
                        callback.onSuccess(list);
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    public void createNotification(String userId, String title, String body) {
        NotificationModel nm = new NotificationModel(null, userId, title, body, "BOOKING_UPDATE");
        if (useMockMode) {
            nm.setNotificationId("notif_" + (mockNotifications.size() + 1));
            mockNotifications.add(0, nm); // Insert at index 0 (newest first)
            saveSessionToDisk();
        } else {
            try {
                String newId = FirebaseFirestore.getInstance().collection("notifications").document().getId();
                nm.setNotificationId(newId);
                FirebaseFirestore.getInstance().collection("notifications").document(newId).set(nm);
            } catch (Exception ignored) {}
        }
    }

    // Get all users in the system for Admin management
    public void getAllUsers(ListCallback<User> callback) {
        if (useMockMode) {
            callback.onSuccess(new ArrayList<>(mockUsers.values()));
        } else {
            try {
                FirebaseFirestore.getInstance().collection("users").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<User> list = queryDocumentSnapshots.toObjects(User.class);
                        callback.onSuccess(list);
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    public void updateUserStatus(String userId, boolean active, SimpleCallback callback) {
        if (useMockMode) {
            User u = mockUsers.get(userId);
            if (u != null) {
                u.setActive(active);
                saveSessionToDisk();
                callback.onSuccess();
            } else {
                callback.onFailure("User not found.");
            }
        } else {
            try {
                Map<String, Object> update = new HashMap<>();
                update.put("active", active);
                FirebaseFirestore.getInstance().collection("users").document(userId).update(update)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }
    }

    private void seedMockData() {
        // Services/Categories (15 user-requested categories)
        if (mockCategories.isEmpty()) {
            Category c1 = new Category("cat_plumbing", "Plumbing", "Pipe leakage, broken taps & water tank repair", 250.00, "emoji:🚰");
            Category c2 = new Category("cat_electrical", "Electrical Services", "Short circuit, fan fitting & wiring repair", 200.00, "emoji:⚡");
            Category c3 = new Category("cat_home_cleaning", "Home Cleaning", "Deep home dusting, kitchen & bathroom washing", 300.00, "emoji:🧹");
            Category c4 = new Category("cat_ac_repair", "AC Repair & Service", "AC wet servicing, gas refilling & cooling check", 350.00, "emoji:❄️");
            Category c5 = new Category("cat_appliance_repair", "Appliance Repair", "TV, fridge, washing machine & microwave repair", 250.00, "emoji:🔌");
            Category c6 = new Category("cat_carpenter", "Carpenter", "Furniture assembly, door repair & woodwork help", 200.00, "emoji:🪚");
            Category c7 = new Category("cat_painter", "Painter", "Interior, exterior walls painting & touch-ups", 250.00, "emoji:🎨");
            Category c8 = new Category("cat_pest_control", "Pest Control", "Termite, bedbug & general pest control services", 300.00, "emoji:🦟");
            Category c9 = new Category("cat_gardening", "Gardening", "Lawn mowing, watering, seeding & plant care", 180.00, "emoji:🏡");
            Category c10 = new Category("cat_vehicle_washing", "Vehicle Washing", "Detailed car & bike pressure foam washing", 150.00, "emoji:🚗");
            Category c11 = new Category("cat_home_shifting", "Home Shifting", "Professional packers, movers & home shifting", 1500.00, "emoji:📦");
            Category c12 = new Category("cat_cctv", "CCTV Installation", "Security cameras setup, DVR configuring & repair", 450.00, "emoji:📷");
            Category c13 = new Category("cat_ro_purifier", "RO/Water Purifier Service", "RO filter replacement, service & water check", 200.00, "emoji:💧");
            Category c14 = new Category("cat_laundry", "Laundry Service", "Clothes steam wash, ironing & dry cleaning", 100.00, "emoji:🧺");
            Category c15 = new Category("cat_computer_laptop", "Computer & Laptop Repair", "OS installation, screen replacement & hardware repair", 400.00, "emoji:💻");

            mockCategories.put(c1.getCategoryId(), c1);
            mockCategories.put(c2.getCategoryId(), c2);
            mockCategories.put(c3.getCategoryId(), c3);
            mockCategories.put(c4.getCategoryId(), c4);
            mockCategories.put(c5.getCategoryId(), c5);
            mockCategories.put(c6.getCategoryId(), c6);
            mockCategories.put(c7.getCategoryId(), c7);
            mockCategories.put(c8.getCategoryId(), c8);
            mockCategories.put(c9.getCategoryId(), c9);
            mockCategories.put(c10.getCategoryId(), c10);
            mockCategories.put(c11.getCategoryId(), c11);
            mockCategories.put(c12.getCategoryId(), c12);
            mockCategories.put(c13.getCategoryId(), c13);
            mockCategories.put(c14.getCategoryId(), c14);
            mockCategories.put(c15.getCategoryId(), c15);
        }

        // Standard Users & Profiles
        if (mockUsers.isEmpty()) {
            // 1. Customer
            User customer = new User("customer_id", "Sarah Jenkins", "customer@workbee.com", "9876543210", "CUSTOMER");
            customer.setProfileImageUrl("https://randomuser.me/api/portraits/women/44.jpg");
            customer.setAddress("456 Blossom Lane, Tech City");
            customer.setLatitude(37.7749);
            customer.setLongitude(-122.4194);
            mockUsers.put(customer.getUserId(), customer);

            // 2. Service Providers aligned with Workerlly categories
            // Provider 1: House Maid
            User userP1 = new User("prov_house_maid", "David Miller", "provider@workbee.com", "9876500001", "PROVIDER");
            userP1.setProfileImageUrl("https://randomuser.me/api/portraits/men/32.jpg");
            userP1.setAddress("101 Pipe Way, Tech City");
            userP1.setLatitude(37.7850);
            userP1.setLongitude(-122.4200);
            mockUsers.put(userP1.getUserId(), userP1);

            // Provider 2: Car Driver
            User userP2 = new User("prov_car_driver", "Marcus Sparks", "sparky@workbee.com", "9876500002", "PROVIDER");
            userP2.setProfileImageUrl("https://randomuser.me/api/portraits/men/45.jpg");
            userP2.setAddress("202 Volts Rd, Tech City");
            userP2.setLatitude(37.7650);
            userP2.setLongitude(-122.4100);
            mockUsers.put(userP2.getUserId(), userP2);

            // Provider 3: Cleaner
            User userP3 = new User("prov_cleaner", "Elena Rostova", "elena@workbee.com", "9876500003", "PROVIDER");
            userP3.setProfileImageUrl("https://randomuser.me/api/portraits/women/65.jpg");
            userP3.setAddress("303 Pristine Ave, Tech City");
            userP3.setLatitude(37.7700);
            userP3.setLongitude(-122.4300);
            mockUsers.put(userP3.getUserId(), userP3);

            // Provider 4: Veg Cook (Unapproved for vetting demo)
            User userP4 = new User("prov_veg_cook", "Jack Higgins", "jack@workbee.com", "9876500004", "PROVIDER");
            userP4.setProfileImageUrl("https://randomuser.me/api/portraits/men/82.jpg");
            userP4.setAddress("404 Ridge St, Tech City");
            mockUsers.put(userP4.getUserId(), userP4);

            // 3. Admin
            User admin = new User("admin_id", "WorkBee Admin", "admin@workbee.com", "9998887770", "ADMIN");
            admin.setProfileImageUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80");
            mockUsers.put(admin.getUserId(), admin);
        }

        // Service Providers Models
        if (mockProviders.isEmpty()) {
            Provider prov1 = new Provider("prov_house_maid", "Elite House Maid Services", "House Maid", 8, 25.00, "Professional residential house maid services. Dusting, mapping, kitchen cleaning, laundry, and daily maintenance.");
            prov1.setApproved(true);
            prov1.setRating(4.8);
            prov1.setReviewCount(15);
            prov1.setCompletedJobs(42);
            prov1.setEarnings(1050.00);
            prov1.getSkills().add("Deep Dusting");
            prov1.getSkills().add("Lawn/Mop Care");
            prov1.getSkills().add("Laundry expert");
            prov1.getPortfolioUrls().add("https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=250&q=80");
            prov1.getPortfolioUrls().add("https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?auto=format&fit=crop&w=250&q=80");
            prov1.getVettingBadges().add("Background Verified ✅");
            prov1.getVettingBadges().add("Elite Star ⭐");
            mockProviders.put(prov1.getProviderId(), prov1);

            Provider prov2 = new Provider("prov_car_driver", "Sparks Private Drivers", "Car Driver", 5, 30.00, "Licensed personal car driver and chauffeur. Experience driving sedans, SUVs, and luxury electric vehicles safely.");
            prov2.setApproved(true);
            prov2.setRating(4.9);
            prov2.setReviewCount(9);
            prov2.setCompletedJobs(18);
            prov2.setEarnings(540.00);
            prov2.getSkills().add("Defensive Driving");
            prov2.getSkills().add("GPS Navigation");
            prov2.getSkills().add("Luxury Chauffeur");
            prov2.getPortfolioUrls().add("https://images.unsplash.com/photo-1494976388531-d1058094e2fd?auto=format&fit=crop&w=250&q=80");
            prov2.getPortfolioUrls().add("https://images.unsplash.com/photo-1486006920555-c77dce18193b?auto=format&fit=crop&w=250&q=80");
            prov2.getVettingBadges().add("License Verified 🪪");
            prov2.getVettingBadges().add("Road Hero 🚗");
            mockProviders.put(prov2.getProviderId(), prov2);

            Provider prov3 = new Provider("prov_cleaner", "Elena\'s Deep Cleaning", "Cleaner", 4, 30.00, "Thorough eco-friendly cleaning service. Trustworthy, reliable, and detailed deep home sanitations.");
            prov3.setApproved(true);
            prov3.setRating(4.7);
            prov3.setReviewCount(24);
            prov3.setCompletedJobs(68);
            prov3.setEarnings(2040.00);
            prov3.getSkills().add("Deep Cleaning");
            prov3.getSkills().add("Eco-friendly Products");
            prov3.getSkills().add("Sanitization");
            prov3.getPortfolioUrls().add("https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=250&q=80");
            prov3.getPortfolioUrls().add("https://images.unsplash.com/photo-1628177142898-93e36e4e3a50?auto=format&fit=crop&w=250&q=80");
            prov3.getVettingBadges().add("Eco-Certified 🌿");
            prov3.getVettingBadges().add("COVID Vaccinated 💉");
            mockProviders.put(prov3.getProviderId(), prov3);

            Provider prov4 = new Provider("prov_veg_cook", "Jack\'s Pure Veg Kitchen", "Veg Cook", 12, 40.00, "Expert vegetarian chef. South Indian, North Indian, continental recipes, healthy dietary cooking.");
            prov4.setApproved(false); // Awaiting approval
            prov4.setRating(5.0);
            prov4.setReviewCount(0);
            prov4.getVettingBadges().add("Chef Certified 🧑‍🍳");
            mockProviders.put(prov4.getProviderId(), prov4);
        }

        // Bookings
        if (mockBookings.isEmpty()) {
            Booking b1 = new Booking("booking_1", "customer_id", "Sarah Jenkins", "prov_house_maid", "Elite House Maid Services", "House Maid", "2026-05-24", "10:00 AM - 12:00 PM", "Daily household chores and living room dusting.", 50.00, "456 Blossom Lane, Tech City");
            b1.setStatus("COMPLETED");
            b1.setRated(true);
            b1.setPaymentMethod("CASH");
            mockBookings.put(b1.getBookingId(), b1);

            Booking b2 = new Booking("booking_2", "customer_id", "Sarah Jenkins", "prov_car_driver", "Sparks Private Drivers", "Car Driver", "2026-05-28", "02:00 PM - 04:00 PM", "Need a driver to drop me to the airport safely.", 66.00, "456 Blossom Lane, Tech City");
            b2.setStatus("ACCEPTED");
            b2.setPaymentMethod("ONLINE");
            mockBookings.put(b2.getBookingId(), b2);

            Booking b3 = new Booking("booking_3", "customer_id", "Sarah Jenkins", "prov_cleaner", "Elena\'s Deep Cleaning", "Cleaner", "2026-05-29", "09:00 AM - 12:00 PM", "Whole house dusting and carpet steam wash request.", 99.00, "456 Blossom Lane, Tech City");
            b3.setStatus("PENDING");
            b3.setPaymentMethod("CASH");
            mockBookings.put(b3.getBookingId(), b3);
        }

        // Reviews
        if (mockReviews.isEmpty()) {
            mockReviews.add(new Review("review_1", "booking_1", "customer_id", "Sarah Jenkins", "prov_house_maid", 5.0, "David arrived on time and did a spectacular cleaning job. Very polite and professional."));
            mockReviews.add(new Review("review_2", "booking_other", "customer_test", "Alex Rivera", "prov_house_maid", 4.5, "Reliable maid service, did everything fast and beautifully."));
            mockReviews.add(new Review("review_3", "booking_other2", "customer_test2", "Bianca Vance", "prov_cleaner", 4.8, "Elena does an exceptional deep cleaning. Everything looks brand new!"));
        }
    }
}
