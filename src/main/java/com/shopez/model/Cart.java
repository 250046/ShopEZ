package com.shopez.model;

import java.util.ArrayList;
import java.util.List;

public class Cart {

    private int cartId;
    private int customerId;
    private List<CartItem> items;

    public Cart() {
        this.items = new ArrayList<>();
    }

    public Cart(int cartId, int customerId) {
        this.cartId = cartId;
        this.customerId = customerId;
        this.items = new ArrayList<>();
    }

    public void addItem(CartItem item) {
        for (CartItem existing : items) {
            if (existing.getProduct().getProductId() == item.getProduct().getProductId()) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                return;
            }
        }
        items.add(item);
    }

    public void removeItem(int productId) {
        items.removeIf(item -> item.getProduct().getProductId() == productId);
    }

    public void updateQuantity(int productId, int newQty) {
        for (CartItem item : items) {
            if (item.getProduct().getProductId() == productId) {
                item.setQuantity(newQty);
                return;
            }
        }
    }

    public double getSubtotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    public int getTotalItemCount() {
        int count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    public void clear() {
        items.clear();
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public int getCartId() {
        return cartId;
    }

    public void setCartId(int cartId) {
        this.cartId = cartId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }
}
