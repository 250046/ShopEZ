package com.shopez.model;

public class OrderItem {

    private int orderItemId;
    private int orderId;
    private Product product;
    private int quantity;
    private double priceAtPurchase;

    public OrderItem() {
    }

    public OrderItem(int orderItemId, int orderId, Product product, int quantity, double priceAtPurchase) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.product = product;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public double getSubtotal() {
        return priceAtPurchase * quantity;
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPriceAtPurchase() {
        return priceAtPurchase;
    }

    public void setPriceAtPurchase(double priceAtPurchase) {
        this.priceAtPurchase = priceAtPurchase;
    }
}
