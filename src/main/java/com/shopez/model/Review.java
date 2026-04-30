package com.shopez.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Review {

    private int reviewId;
    private int customerId;
    private int productId;
    private String customerName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    public Review() {
    }

    public Review(int reviewId, int customerId, int productId, String customerName,
                  int rating, String comment, LocalDateTime createdAt) {
        this.reviewId = reviewId;
        this.customerId = customerId;
        this.productId = productId;
        this.customerName = customerName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Review{rating=" + rating + "/5, by='" + customerName + "', comment='" + comment + "'"
                + ", date=" + (createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "N/A") + "}";
    }
}
