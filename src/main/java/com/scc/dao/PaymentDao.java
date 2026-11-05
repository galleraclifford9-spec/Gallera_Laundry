package com.scc.dao;

import com.scc.model.Payment;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PaymentDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    private static final String BASE_SELECT =
            "SELECT p.payment_id, p.order_id, c.full_name AS customer_name, r.full_name AS received_by, "
                    + "p.amount_paid, p.payment_method, p.payment_date, p.receipt_number "
                    + "FROM payments p "
                    + "JOIN orders o ON o.order_id = p.order_id "
                    + "JOIN users c ON c.user_id = o.customer_id "
                    + "JOIN users r ON r.user_id = p.received_by ";

    private Payment map(ResultSet rs) throws SQLException {
        return new Payment(
                rs.getInt("payment_id"),
                rs.getInt("order_id"),
                rs.getString("customer_name"),
                rs.getString("received_by"),
                rs.getDouble("amount_paid"),
                rs.getString("payment_method"),
                rs.getString("payment_date"),
                rs.getString("receipt_number"));
    }

    // Any filter can be null. datePrefix: "2026-11-05" (daily) or "2026-11" (monthly)
    public List<Payment> getPayments(Integer customerId, Integer orderId, String datePrefix) {
        List<Payment> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();

        if (customerId != null) {
            where.add("o.customer_id = ?");
            params.add(customerId);
        }
        if (orderId != null) {
            where.add("p.order_id = ?");
            params.add(orderId);
        }
        if (datePrefix != null) {
            where.add("p.payment_date LIKE ?");
            params.add(datePrefix + "%");
        }
        if (!where.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        }
        sql.append("ORDER BY p.payment_id DESC");

        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching payments: " + e.getMessage());
        }
        return list;
    }

    public Payment findByReceipt(String receipt) {
        String sql = BASE_SELECT + "WHERE p.receipt_number = ?";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receipt);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching payment: " + e.getMessage());
        }
        return null;
    }

    // Returns the saved Payment (with receipt number), or null if failed
    public Payment recordPayment(int staffId, int orderId, double amount, String method) {
        String ordSql = "SELECT o.total_amount, o.status, "
                + "COALESCE((SELECT SUM(p.amount_paid) FROM payments p WHERE p.order_id = o.order_id), 0) AS paid "
                + "FROM orders o WHERE o.order_id = ?";
        String idSql = "SELECT COALESCE(MAX(payment_id), 0) + 1 FROM payments";
        String insSql = "INSERT INTO payments (order_id, received_by, amount_paid, payment_method, receipt_number) VALUES (?,?,?,?,?)";

        String receipt = null;
        double balanceAfter = 0;

        try (Connection conn = DatabaseConnection.getconnection()) {
            conn.setAutoCommit(false);
            try {
                double total = 0;
                double paid = 0;
                String status = null;

                try (PreparedStatement ps = conn.prepareStatement(ordSql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println(" -> Order not found.");
                            conn.rollback();
                            return null;
                        }
                        total = rs.getDouble("total_amount");
                        status = rs.getString("status");
                        paid = rs.getDouble("paid");
                    }
                }

                if ("CANCELLED".equals(status)) {
                    System.out.println(" -> Order #" + orderId + " is CANCELLED. Cannot accept payment.");
                    conn.rollback();
                    return null;
                }

                double balance = total - paid;
                if (balance <= 0.009) {
                    System.out.println(" -> Order #" + orderId + " is already fully paid.");
                    conn.rollback();
                    return null;
                }
                if (amount > balance + 0.009) {
                    System.out.printf(" -> Amount exceeds the balance (P%.2f).%n", balance);
                    conn.rollback();
                    return null;
                }

                int nextId;
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(idSql)) {
                    rs.next();
                    nextId = rs.getInt(1);
                }
                receipt = "RCPT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                        + "-" + String.format("%05d", nextId);

                try (PreparedStatement ps = conn.prepareStatement(insSql)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, staffId);
                    ps.setDouble(3, amount);
                    ps.setString(4, method);
                    ps.setString(5, receipt);
                    ps.executeUpdate();
                }

                conn.commit();
                balanceAfter = balance - amount;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error recording payment: " + e.getMessage());
            return null;
        }

        logDao.log(staffId, "PAYMENT_RECORDED",
                String.format("Recorded payment P%.2f for Order #%d (Receipt %s) - Balance: P%.2f",
                        amount, orderId, receipt, Math.max(0, balanceAfter)));
        return findByReceipt(receipt);
    }
}