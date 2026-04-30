package com.shopez.dao;

import com.shopez.db.DBConnection;
import com.shopez.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    public Review save(Review review) throws SQLException {
        String sql = "INSERT INTO reviews (customer_id, product_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, review.getCustomerId());
            ps.setInt(2, review.getProductId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    review.setReviewId(keys.getInt(1));
                }
            }
        }
        return review;
    }

    public List<Review> findByProduct(int productId) throws SQLException {
        String sql = "SELECT r.*, u.name AS customer_name FROM reviews r "
                + "JOIN users u ON r.customer_id = u.user_id "
                + "WHERE r.product_id = ? ORDER BY r.created_at DESC";
        List<Review> reviews = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapReview(rs));
                }
            }
        }
        return reviews;
    }

    public List<Review> findByCustomer(int customerId) throws SQLException {
        String sql = "SELECT r.*, u.name AS customer_name FROM reviews r "
                + "JOIN users u ON r.customer_id = u.user_id "
                + "WHERE r.customer_id = ? ORDER BY r.created_at DESC";
        List<Review> reviews = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapReview(rs));
                }
            }
        }
        return reviews;
    }

    public boolean hasCustomerReviewedProduct(int customerId, int productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reviews WHERE customer_id = ? AND product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean hasCustomerPurchasedProduct(int customerId, int productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders o JOIN order_items oi ON o.order_id = oi.order_id "
                + "WHERE o.customer_id = ? AND oi.product_id = ? AND o.status != 'CANCELLED'";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public double getAverageRating(int productId) throws SQLException {
        String sql = "SELECT COALESCE(AVG(rating), 0.0) AS avg_rating FROM reviews WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg_rating");
                }
            }
        }
        return 0.0;
    }

    private Review mapReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("review_id"));
        review.setCustomerId(rs.getInt("customer_id"));
        review.setProductId(rs.getInt("product_id"));
        review.setCustomerName(rs.getString("customer_name"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));
        Timestamp ts = rs.getTimestamp("created_at");
        review.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return review;
    }
}
