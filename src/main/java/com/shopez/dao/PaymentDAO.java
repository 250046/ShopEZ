package com.shopez.dao;

import com.shopez.db.DBConnection;
import com.shopez.model.CreditCardPayment;
import com.shopez.model.Payment;
import com.shopez.model.WalletPayment;

import java.sql.*;
import java.util.Optional;

public class PaymentDAO {

    private Connection getConnection() {
        return DBConnection.getInstance().getConnection();
    }

    public Payment save(Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (order_id, payment_method, amount, status, masked_card, card_holder, wallet_snapshot, paid_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getOrderId());
            ps.setString(2, payment.getPaymentMethod());
            ps.setDouble(3, payment.getAmount());
            ps.setString(4, payment.getStatus().name());

            if (payment instanceof CreditCardPayment) {
                CreditCardPayment cc = (CreditCardPayment) payment;
                ps.setString(5, cc.getMaskedCardNumber());
                ps.setString(6, cc.getCardHolder());
                ps.setNull(7, Types.DECIMAL);
            } else if (payment instanceof WalletPayment) {
                WalletPayment wp = (WalletPayment) payment;
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.VARCHAR);
                ps.setDouble(7, wp.getWalletBalance());
            } else {
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.DECIMAL);
            }

            if (payment.getPaidAt() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(payment.getPaidAt()));
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    payment.setPaymentId(keys.getInt(1));
                }
            }
        }
        return payment;
    }

    public Optional<Payment> findByOrder(int orderId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE order_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPayment(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void updateStatus(int paymentId, Payment.PaymentStatus status) throws SQLException {
        String sql = "UPDATE payments SET status = ? WHERE payment_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, paymentId);
            ps.executeUpdate();
        }
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        String method = rs.getString("payment_method");
        Payment payment;

        if ("CREDIT_CARD".equals(method)) {
            CreditCardPayment cc = new CreditCardPayment();
            cc.setMaskedCardNumber(rs.getString("masked_card"));
            cc.setCardHolder(rs.getString("card_holder"));
            payment = cc;
        } else {
            WalletPayment wp = new WalletPayment();
            wp.setWalletBalance(rs.getDouble("wallet_snapshot"));
            payment = wp;
        }

        payment.setPaymentId(rs.getInt("payment_id"));
        payment.setOrderId(rs.getInt("order_id"));
        payment.setAmount(rs.getDouble("amount"));
        payment.setStatus(Payment.PaymentStatus.valueOf(rs.getString("status")));
        payment.setPaymentMethod(method);
        Timestamp paidAt = rs.getTimestamp("paid_at");
        payment.setPaidAt(paidAt != null ? paidAt.toLocalDateTime() : null);

        return payment;
    }
}
