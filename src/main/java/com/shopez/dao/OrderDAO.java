package com.shopez.dao;

import com.shopez.db.DBConnection;
import com.shopez.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDAO {

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    public Order save(Order order) throws SQLException {
        String sql = "INSERT INTO orders (customer_id, total_amount, status, shipping_address) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getCustomerId());
            ps.setDouble(2, order.getTotalAmount());
            ps.setString(3, order.getStatus().name());
            ps.setString(4, order.getShippingAddress());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setOrderId(keys.getInt(1));
                }
            }
        }

        // Insert order items
        String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, price_at_purchase) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(itemSql)) {
            for (OrderItem item : order.getItems()) {
                ps.setInt(1, order.getOrderId());
                ps.setInt(2, item.getProduct().getProductId());
                ps.setInt(3, item.getQuantity());
                ps.setDouble(4, item.getPriceAtPurchase());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        return order;
    }

    public Optional<Order> findById(int orderId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE order_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(findOrderItems(orderId));
                    return Optional.of(order);
                }
            }
        }
        return Optional.empty();
    }

    public List<Order> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(findOrderItems(order.getOrderId()));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    public List<Order> findBySeller(int sellerId) throws SQLException {
        String sql = "SELECT DISTINCT o.* FROM orders o "
                + "JOIN order_items oi ON o.order_id = oi.order_id "
                + "JOIN products p ON oi.product_id = p.product_id "
                + "WHERE p.seller_id = ? ORDER BY o.created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(findOrderItems(order.getOrderId()));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    public List<Order> findAll() throws SQLException {
        String sql = "SELECT * FROM orders ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Order order = mapOrder(rs);
                order.setItems(findOrderItems(order.getOrderId()));
                orders.add(order);
            }
        }
        return orders;
    }

    public void updateStatus(int orderId, Order.OrderStatus status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setCustomerId(rs.getInt("customer_id"));
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setStatus(Order.OrderStatus.valueOf(rs.getString("status")));
        order.setShippingAddress(rs.getString("shipping_address"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        order.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        order.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);
        return order;
    }

    private List<OrderItem> findOrderItems(int orderId) throws SQLException {
        String sql = "SELECT oi.*, p.name, p.description, p.price, p.stock_qty, p.category, p.product_type, "
                + "p.weight_kg, p.dimensions, p.download_url, p.license_key, p.image_path, p.is_active, p.seller_id, p.created_at AS p_created_at, "
                + "u.store_name FROM order_items oi "
                + "JOIN products p ON oi.product_id = p.product_id "
                + "JOIN users u ON p.seller_id = u.user_id "
                + "WHERE oi.order_id = ?";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setOrderItemId(rs.getInt("order_item_id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPriceAtPurchase(rs.getDouble("price_at_purchase"));

                    // Map product
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
                    p.setSellerStoreName(rs.getString("store_name"));

                    item.setProduct(p);
                    items.add(item);
                }
            }
        }
        return items;
    }

    public double getTotalRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status != 'CANCELLED'";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0;
    }

    public double getSellerRevenue(int sellerId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(oi.quantity * oi.price_at_purchase), 0) FROM order_items oi "
                + "JOIN products p ON oi.product_id = p.product_id "
                + "JOIN orders o ON oi.order_id = o.order_id "
                + "WHERE p.seller_id = ? AND o.status != 'CANCELLED'";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0;
    }

    public int getSellerOrderCount(int sellerId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT o.order_id) FROM orders o "
                + "JOIN order_items oi ON o.order_id = oi.order_id "
                + "JOIN products p ON oi.product_id = p.product_id "
                + "WHERE p.seller_id = ? AND o.status != 'CANCELLED'";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public int getSellerPendingOrderCount(int sellerId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT o.order_id) FROM orders o "
                + "JOIN order_items oi ON o.order_id = oi.order_id "
                + "JOIN products p ON oi.product_id = p.product_id "
                + "WHERE p.seller_id = ? AND o.status = 'PENDING'";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
