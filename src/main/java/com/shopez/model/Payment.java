package com.shopez.model;

import java.time.LocalDateTime;

public abstract class Payment {

    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }

    protected int paymentId;
    protected int orderId;
    protected double amount;
    protected PaymentStatus status;
    protected String paymentMethod;
    protected LocalDateTime paidAt;

    public Payment() {
        this.status = PaymentStatus.PENDING;
    }

    public abstract boolean processPayment();

    public abstract boolean refund();

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
