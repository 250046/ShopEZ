package com.shopez.dao;

import com.shopez.db.DBConnection;
import com.shopez.model.DigitalProduct;
import com.shopez.model.PhysicalProduct;
import com.shopez.model.Product;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
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
        p.setImagePath(rs.getString("image_path"));
        Timestamp ts = rs.getTimestamp("created_at");
        p.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);

        try {
            p.setSellerStoreName(rs.getString("store_name"));
        } catch (SQLException ignored) {
            // store_name not in result set for some queries
        }

        return p;
    }

    public List<Product> findAll() throws SQLException {
        String sql = "SELECT p.*, u.store_name FROM products p JOIN users u ON p.seller_id = u.user_id WHERE p.is_active = 1 ORDER BY p.created_at DESC";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(mapProduct(rs));
            }
        }
        return products;
    }

    public List<Product> findByCategory(String category) throws SQLException {
        String sql = "SELECT p.*, u.store_name FROM products p JOIN users u ON p.seller_id = u.user_id WHERE p.is_active = 1 AND p.category = ? ORDER BY p.created_at DESC";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        }
        return products;
    }

    public List<Product> findBySeller(int sellerId) throws SQLException {
        String sql = "SELECT p.*, u.store_name FROM products p JOIN users u ON p.seller_id = u.user_id WHERE p.seller_id = ? ORDER BY p.created_at DESC";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        }
        return products;
    }

    public Optional<Product> findById(int productId) throws SQLException {
        String sql = "SELECT p.*, u.store_name FROM products p JOIN users u ON p.seller_id = u.user_id WHERE p.product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapProduct(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Product save(Product product) throws SQLException {
        String sql = "INSERT INTO products (seller_id, name, description, price, stock_qty, category, product_type, "
                + "weight_kg, dimensions, download_url, license_key, image_path, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, product.getSellerId());
            ps.setString(2, product.getName());
            ps.setString(3, product.getDescription());
            ps.setDouble(4, product.getPrice());
            ps.setInt(5, product.getStockQty());
            ps.setString(6, product.getCategory());
            ps.setString(7, product.getProductType());

            if (product instanceof PhysicalProduct) {
                PhysicalProduct pp = (PhysicalProduct) product;
                ps.setDouble(8, pp.getWeightKg());
                ps.setString(9, pp.getDimensions());
                ps.setNull(10, Types.VARCHAR);
                ps.setNull(11, Types.VARCHAR);
            } else if (product instanceof DigitalProduct) {
                DigitalProduct dp = (DigitalProduct) product;
                ps.setNull(8, Types.DECIMAL);
                ps.setNull(9, Types.VARCHAR);
                ps.setString(10, dp.getDownloadUrl());
                ps.setString(11, dp.getLicenseKey());
            } else {
                ps.setNull(8, Types.DECIMAL);
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
                ps.setNull(11, Types.VARCHAR);
            }

            if (product.getImagePath() != null) {
                ps.setString(12, product.getImagePath());
            } else {
                ps.setNull(12, Types.VARCHAR);
            }
            ps.setBoolean(13, product.isActive());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    product.setProductId(keys.getInt(1));
                }
            }
        }
        return product;
    }

    public void update(Product product) throws SQLException {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, stock_qty = ?, category = ?, product_type = ?, "
                + "weight_kg = ?, dimensions = ?, download_url = ?, license_key = ?, image_path = ?, is_active = ? WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getStockQty());
            ps.setString(5, product.getCategory());
            ps.setString(6, product.getProductType());

            if (product instanceof PhysicalProduct) {
                PhysicalProduct pp = (PhysicalProduct) product;
                ps.setDouble(7, pp.getWeightKg());
                ps.setString(8, pp.getDimensions());
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
            } else if (product instanceof DigitalProduct) {
                DigitalProduct dp = (DigitalProduct) product;
                ps.setNull(7, Types.DECIMAL);
                ps.setNull(8, Types.VARCHAR);
                ps.setString(9, dp.getDownloadUrl());
                ps.setString(10, dp.getLicenseKey());
            } else {
                ps.setNull(7, Types.DECIMAL);
                ps.setNull(8, Types.VARCHAR);
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
            }

            if (product.getImagePath() != null) {
                ps.setString(11, product.getImagePath());
            } else {
                ps.setNull(11, Types.VARCHAR);
            }
            ps.setBoolean(12, product.isActive());
            ps.setInt(13, product.getProductId());
            ps.executeUpdate();
        }
    }

    public void delete(int productId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    public void updateStock(int productId, int newQty) throws SQLException {
        // Get old stock to check if restocking from 0
        int oldStock = 0;
        String checkSql = "SELECT stock_qty FROM products WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(checkSql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    oldStock = rs.getInt("stock_qty");
                }
            }
        }

        String sql = "UPDATE products SET stock_qty = ? WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, newQty);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }

        // If restocking from 0, notify watchers
        if (oldStock == 0 && newQty > 0) {
            notifyWatchers(productId);
        }
    }

    private void notifyWatchers(int productId) throws SQLException {
        // Get product name
        String productName = "";
        String nameSql = "SELECT name FROM products WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(nameSql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    productName = rs.getString("name");
                }
            }
        }

        // Get all watchers
        String watcherSql = "SELECT customer_id FROM stock_watchers WHERE product_id = ?";
        List<Integer> watcherIds = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(watcherSql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    watcherIds.add(rs.getInt("customer_id"));
                }
            }
        }

        // Insert notification for each watcher
        String notifSql = "INSERT INTO notifications (user_id, message) VALUES (?, ?)";
        String message = "Good news! \"" + productName + "\" is back in stock. Grab it before it sells out.";
        try (PreparedStatement ps = getConnection().prepareStatement(notifSql)) {
            for (int customerId : watcherIds) {
                ps.setInt(1, customerId);
                ps.setString(2, message);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Remove watch entries
        String deleteSql = "DELETE FROM stock_watchers WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(deleteSql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    public List<Product> search(String keyword) throws SQLException {
        String sql = "SELECT p.*, u.store_name FROM products p JOIN users u ON p.seller_id = u.user_id "
                + "WHERE p.is_active = 1 AND (p.name LIKE ? OR p.description LIKE ?) ORDER BY p.created_at DESC";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        }
        return products;
    }

    public List<String> getAllCategories() throws SQLException {
        String sql = "SELECT DISTINCT category FROM products WHERE is_active = 1 AND category IS NOT NULL ORDER BY category";
        List<String> categories = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        }
        return categories;
    }

    public void addStockWatcher(int customerId, int productId) throws SQLException {
        String sql = "INSERT IGNORE INTO stock_watchers (customer_id, product_id) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public int getUnreadNotificationCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void markNotificationsRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public List<Product> findAllIncludingInactive() throws SQLException {
        String sql = "SELECT p.*, u.store_name FROM products p JOIN users u ON p.seller_id = u.user_id ORDER BY p.created_at DESC";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(mapProduct(rs));
            }
        }
        return products;
    }

    public void setActive(int productId, boolean active) throws SQLException {
        String sql = "UPDATE products SET is_active = ? WHERE product_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }
}
