package com.shopez.model;

public class OutOfStockException extends Exception {

    public OutOfStockException(String message) {
        super(message);
    }
}
