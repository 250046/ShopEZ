package com.shopez.model;

import java.time.LocalDateTime;

public class CreditCardPayment extends Payment {

    private String maskedCardNumber;
    private String cardHolder;

    public CreditCardPayment() {
        this.paymentMethod = "CREDIT_CARD";
    }

    @Override
    public boolean processPayment() {
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
        return true;
    }

    @Override
    public boolean refund() {
        this.status = PaymentStatus.REFUNDED;
        return true;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
    }
}
