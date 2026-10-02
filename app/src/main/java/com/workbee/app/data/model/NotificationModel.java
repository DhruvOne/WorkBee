package com.workbee.app.data.model;

import java.io.Serializable;
import java.util.Date;

public class NotificationModel implements Serializable {
    private String notificationId;
    private String userId;
    private String title;
    private String body;
    private String type; // BOOKING_UPDATE | CHAT | ADMIN
    private boolean isRead;
    private Date createdAt;

    // Required empty constructor for Firestore
    public NotificationModel() {
        this.isRead = false;
        this.createdAt = new Date();
    }

    public NotificationModel(String notificationId, String userId, String title, String body, String type) {
        this();
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.type = type;
    }

    // Getters and Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
