package com.shopez.model;

import java.time.LocalDateTime;

public class WalletPayment extends Payment {

    private double walletBalance;

    public WalletPayment() {
        this.paymentMethod = "WALLET";
    }

    @Override
    public boolean processPayment() {
        if (walletBalance >= amount) {
            this.status = PaymentStatus.COMPLETED;
            this.paidAt = LocalDateTime.now();
            return true;
        } else {
            this.status = PaymentStatus.FAILED;
            return false;
        }
    }

    @Override
    public boolean refund() {
        this.status = PaymentStatus.REFUNDED;
        return true;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }
}
