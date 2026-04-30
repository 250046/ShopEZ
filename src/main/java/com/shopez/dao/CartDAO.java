package com.shopez.dao;

import com.shopez.db.DBConnection;
import com.shopez.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDAO {

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    public Cart findByCustomer(int customerId) throws SQLException {
        // Check if cart exists, create if not
        String findSql = "SELECT cart_id FROM carts WHERE customer_id = ?";
        int cartId = -1;
        try (PreparedStatement ps = getConnection().prepareStatement(findSql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cartId = rs.getInt("cart_id");
                }
            }
        }

        if (cartId == -1) {
            String createSql = "INSERT INTO carts (customer_id) VALUES (?)";
            try (PreparedStatement ps = getConnection().prepareStatement(createSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        cartId = keys.getInt(1);
                    }
                }
            }
        }

        Cart cart = new Cart(cartId, customerId);

        // Load cart items with product details
        String itemsSql = "SELECT ci.cart_item_id, ci.quantity, ci.unit_price, "
                + "p.product_id, p.name, p.description, p.price, p.stock_qty, p.category, "
                + "p.product_type, p.weight_kg, p.dimensions, p.download_url, p.license_key, "
                + "p.image_path, p.is_active, p.seller_id, p.created_at, u.store_name "
                + "FROM cart_items ci "
                + "JOIN products p ON ci.product_id = p.product_id "
                + "JOIN users u ON p.seller_id = u.user_id "
                + "WHERE ci.cart_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(itemsSql)) {
            ps.setInt(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product product = mapProduct(rs);
                    CartItem item = new CartItem();
                    item.setCartItemId(rs.getInt("cart_item_id"));
                    item.setProduct(product);
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    cart.getItems().add(item);
                }
            }
        }

        return cart;
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        String type = rs.getString("product_type");
        Product p;

        if ("PHYSICAL".equals(type)) {
            PhysicalProduct pp = new PhysicalProduct();
            pp.setWeightKg(rs.getDouble("weight_kg"));
            pp.setDimensions(rs.getString("dimensions"));
            p = pp;
        } else {
            DigitalProduct dp = new DigitalProduct();
            dp.setDownloadUrl(rs.getString("download_url"));
            dp.setLicenseKey(rs.getString("license_key"));
            p = dp;
        }

        p.setProductId(rs.getInt("product_id"));
        p.setSellerId(rs.getInt("seller_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getDouble("price"));
        p.setStockQty(rs.getInt("stock_qty"));
        p.setCategory(rs.getString("category"));
        p.setActive(rs.getBoolean("is_active"));
        try {
            p.setImagePath(rs.getString("image_path"));
        } catch (SQLException ignored) {
        }
        Timestamp ts = rs.getTimestamp("created_at");
        p.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        try {
            p.setSellerStoreName(rs.getString("store_name"));
        } catch (SQLException ignored) {
        }

        return p;
    }

    public void addItem(int cartId, CartItem item) throws SQLException {
        // Check if product already in cart — merge quantities
        String checkSql = "SELECT cart_item_id, quantity FROM cart_items WHERE cart_id = ? AND product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(checkSql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, item.getProduct().getProductId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int newQty = rs.getInt("quantity") + item.getQuantity();
                    String updateSql = "UPDATE cart_items SET quantity = ? WHERE cart_id = ? AND product_id = ?";
                    try (PreparedStatement ups = getConnection().prepareStatement(updateSql)) {
                        ups.setInt(1, newQty);
                        ups.setInt(2, cartId);
                        ups.setInt(3, item.getProduct().getProductId());
                        ups.executeUpdate();
                    }
                    return;
                }
            }
        }

        String insertSql = "INSERT INTO cart_items (cart_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(insertSql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, item.getProduct().getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setDouble(4, item.getUnitPrice());
            ps.executeUpdate();
        }
    }

    public void updateItemQty(int cartId, int productId, int qty) throws SQLException {
        String sql = "UPDATE cart_items SET quantity = ? WHERE cart_id = ? AND product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, cartId);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    public void removeItem(int cartId, int productId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public void clearCart(int cartId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.executeUpdate();
        }
    }
}
