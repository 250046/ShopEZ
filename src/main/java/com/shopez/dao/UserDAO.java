package com.shopez.dao;

import com.shopez.db.DBConnection;
import com.shopez.model.Admin;
import com.shopez.model.Customer;
import com.shopez.model.Seller;
import com.shopez.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    private User mapUser(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        int userId = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;

        switch (role) {
            case "CUSTOMER": {
                Customer c = new Customer();
                c.setUserId(userId);
                c.setName(name);
                c.setEmail(email);
                c.setPasswordHash(passwordHash);
                c.setRole(role);
                c.setCreatedAt(createdAt);
                c.setShippingAddress(rs.getString("shipping_address"));
                c.setWalletBalance(rs.getDouble("wallet_balance"));
                return c;
            }
            case "SELLER": {
                Seller s = new Seller();
                s.setUserId(userId);
                s.setName(name);
                s.setEmail(email);
                s.setPasswordHash(passwordHash);
                s.setRole(role);
                s.setCreatedAt(createdAt);
                s.setStoreName(rs.getString("store_name"));
                s.setApproved(rs.getBoolean("is_approved"));
                return s;
            }
            case "ADMIN": {
                Admin a = new Admin();
                a.setUserId(userId);
                a.setName(name);
                a.setEmail(email);
                a.setPasswordHash(passwordHash);
                a.setRole(role);
                a.setCreatedAt(createdAt);
                return a;
            }
            default:
                throw new SQLException("Unknown role: " + role);
        }
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public User save(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, role, shipping_address, wallet_balance, store_name, is_approved) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());

            if (user instanceof Customer) {
                Customer c = (Customer) user;
                ps.setString(5, c.getShippingAddress());
                ps.setDouble(6, c.getWalletBalance());
                ps.setNull(7, Types.VARCHAR);
                ps.setInt(8, 1);
            } else if (user instanceof Seller) {
                Seller s = (Seller) user;
                ps.setNull(5, Types.VARCHAR);
                ps.setDouble(6, 0);
                ps.setString(7, s.getStoreName());
                ps.setInt(8, s.isApproved() ? 1 : 0);
            } else {
                ps.setNull(5, Types.VARCHAR);
                ps.setDouble(6, 0);
                ps.setNull(7, Types.VARCHAR);
                ps.setInt(8, 1);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserId(keys.getInt(1));
                }
            }
        }
        return user;
    }

    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, email = ?, password_hash = ?, shipping_address = ?, "
                + "wallet_balance = ?, store_name = ?, is_approved = ? WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());

            if (user instanceof Customer) {
                Customer c = (Customer) user;
                ps.setString(4, c.getShippingAddress());
                ps.setDouble(5, c.getWalletBalance());
                ps.setNull(6, Types.VARCHAR);
                ps.setInt(7, 1);
            } else if (user instanceof Seller) {
                Seller s = (Seller) user;
                ps.setNull(4, Types.VARCHAR);
                ps.setDouble(5, 0);
                ps.setString(6, s.getStoreName());
                ps.setInt(7, s.isApproved() ? 1 : 0);
            } else {
                ps.setNull(4, Types.VARCHAR);
                ps.setDouble(5, 0);
                ps.setNull(6, Types.VARCHAR);
                ps.setInt(7, 1);
            }

            ps.setInt(8, user.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public List<User> findAllSellers() throws SQLException {
        String sql = "SELECT * FROM users WHERE role = 'SELLER' ORDER BY created_at DESC";
        List<User> sellers = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sellers.add(mapUser(rs));
            }
        }
        return sellers;
    }

    public List<User> findAllCustomers() throws SQLException {
        String sql = "SELECT * FROM users WHERE role = 'CUSTOMER' ORDER BY created_at DESC";
        List<User> customers = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                customers.add(mapUser(rs));
            }
        }
        return customers;
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    public void updateWalletBalance(int customerId, double newBalance) throws SQLException {
        String sql = "UPDATE users SET wallet_balance = ? WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        }
    }

    public void approveSeller(int sellerId) throws SQLException {
        String sql = "UPDATE users SET is_approved = 1 WHERE user_id = ? AND role = 'SELLER'";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            ps.executeUpdate();
        }
    }

    public void banUser(int userId) throws SQLException {
        String sql = "UPDATE users SET is_banned = 1 WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean isBanned(int userId) throws SQLException {
        String sql = "SELECT is_banned FROM users WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_banned");
                }
            }
        }
        return false;
    }
}
