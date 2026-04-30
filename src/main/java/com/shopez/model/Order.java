package com.shopez.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Order {

    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }

    private int orderId;
    private int customerId;
    private List<OrderItem> items;
    private OrderStatus status;
    private double totalAmount;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order() {
        this.items = new ArrayList<>();
        this.status = OrderStatus.PENDING;
    }

    public double calculateTotal() {
        double total = 0;
        for (OrderItem item : items) {
            total += item.getSubtotal();
        }
        this.totalAmount = total;
        return total;
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String generateInvoiceSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Order Invoice #").append(orderId).append(" ===\n");
        sb.append("Date: ").append(createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A").append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Shipping: ").append(shippingAddress).append("\n\n");
        sb.append("Items:\n");
        for (OrderItem item : items) {
            sb.append("  - ").append(item.getProduct().getName())
                    .append(" x").append(item.getQuantity())
                    .append(" @ $").append(String.format("%.2f", item.getPriceAtPurchase()))
                    .append(" = $").append(String.format("%.2f", item.getSubtotal()))
                    .append("\n");
        }
        sb.append("\nTotal: $").append(String.format("%.2f", totalAmount));
        return sb.toString();
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
