package com.shopez.model;

import java.time.LocalDateTime;

public class Customer extends User {

    private String shippingAddress;
    private double walletBalance;

    public Customer() {
        this.role = "CUSTOMER";
    }

    public Customer(int userId, String name, String email, String passwordHash,
                    LocalDateTime createdAt, String shippingAddress, double walletBalance) {
        super(userId, name, email, passwordHash, "CUSTOMER", createdAt);
        this.shippingAddress = shippingAddress;
        this.walletBalance = walletBalance;
    }

    @Override
    public String getDashboardTitle() {
        return "Customer Dashboard";
    }

    public void topUpWallet(double amount) {
        if (amount > 0) {
            this.walletBalance += amount;
        }
    }

    public void deductWallet(double amount) throws InsufficientFundsException {
        if (amount > walletBalance) {
            throw new InsufficientFundsException(
                    "Insufficient wallet balance. Available: $" + String.format("%.2f", walletBalance)
                            + ", Required: $" + String.format("%.2f", amount));
        }
        this.walletBalance -= amount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }
}
