package com.shopez.model;

import java.time.LocalDateTime;

public abstract class Product {

    protected int productId;
    protected String name;
    protected String description;
    protected double price;
    protected int stockQty;
    protected String category;
    protected int sellerId;
    protected String sellerStoreName;
    protected boolean isActive;
    protected LocalDateTime createdAt;
    protected String imagePath;

    public Product() {
    }

    public abstract String getProductType();

    public abstract double calculateFinalPrice();

    public boolean isInStock() {
        return stockQty > 0;
    }

    public void applyDiscount(double percentage) {
        if (percentage > 0 && percentage <= 100) {
            this.price = this.price * (1 - percentage / 100.0);
        }
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStockQty() {
        return stockQty;
    }

    public void setStockQty(int stockQty) {
        this.stockQty = stockQty;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerStoreName() {
        return sellerStoreName;
    }

    public void setSellerStoreName(String sellerStoreName) {
        this.sellerStoreName = sellerStoreName;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public String toString() {
        return getProductType() + "{id=" + productId + ", name='" + name + "', price=" + price + ", stock=" + stockQty + "}";
    }
}
