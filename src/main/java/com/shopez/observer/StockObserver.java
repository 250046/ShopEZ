package com.shopez.observer;

import com.shopez.db.DBConnection;
import com.shopez.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StockObserver implements StockNotifiable {

    private final int customerId;
    private final int productId;

    public StockObserver(int customerId, int productId) {
        this.customerId = customerId;
        this.productId = productId;
    }

    @Override
    public void onStockRestored(Product product) {
        String message = "Good news! \"" + product.getName() + "\" is back in stock. Grab it before it sells out.";
        String sql = "INSERT INTO notifications (user_id, message) VALUES (?, ?)";
        try {
            Connection conn = DBConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, customerId);
                ps.setString(2, message);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert stock notification: " + e.getMessage(), e);
        }
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getProductId() {
        return productId;
    }
}
