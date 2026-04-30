package com.shopez.observer;

import com.shopez.model.Product;

public interface StockNotifiable {
    void onStockRestored(Product product);
}
