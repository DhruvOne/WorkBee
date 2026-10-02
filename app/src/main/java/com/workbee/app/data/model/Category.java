package com.workbee.app.data.model;

import java.io.Serializable;

public class Category implements Serializable {
    private String categoryId;
    private String name;
    private String description;
    private double basePrice;
    private String iconName; // Drawable vector name or URL

    // Required empty constructor for Firestore
    public Category() {}

    public Category(String categoryId, String name, String description, double basePrice, String iconName) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.iconName = iconName;
    }

    // Getters and Setters
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}
