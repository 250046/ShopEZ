package com.shopez.model;

import java.time.LocalDateTime;

public class Seller extends User {

    private String storeName;
    private boolean isApproved;
    private double rating;

    public Seller() {
        this.role = "SELLER";
    }

    public Seller(int userId, String name, String email, String passwordHash,
                  LocalDateTime createdAt, String storeName, boolean isApproved, double rating) {
        super(userId, name, email, passwordHash, "SELLER", createdAt);
        this.storeName = storeName;
        this.isApproved = isApproved;
        this.rating = rating;
    }

    @Override
    public String getDashboardTitle() {
        return "Seller Dashboard";
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
