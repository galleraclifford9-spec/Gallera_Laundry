package com.scc.dao;

import com.scc.model.PendingOrderRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDao {

    // Orders still being processed, or READY but unclaimed beyond the pickup date
    public List<PendingOrderRow> getPendingUnclaimed() {
        List<PendingOrderRow> list = new ArrayList<>();
        String sql = "SELECT o.order_id, c.full_name AS customer_name, "
                + "COALESCE(NULLIF(cu.contact_number, ''), c.username) AS contact, "
                + "o.status, o.pickup_date, o.total_amount, "
                + "o.total_amount - COALESCE((SELECT SUM(p.amount_paid) FROM payments p WHERE p.order_id = o.order_id), 0) AS balance "
                + "FROM orders o "
                + "JOIN users c ON c.user_id = o.customer_id "
                + "LEFT JOIN customers cu ON cu.user_id = c.user_id "
                + "WHERE o.status IN ('RECEIVED','WASHING','DRYING') "
                + "OR (o.status = 'READY' AND (o.pickup_date IS NULL OR o.pickup_date < date('now','localtime'))) "
                + "ORDER BY o.pickup_date, o.order_id";

        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PendingOrderRow(
                        rs.getInt("order_id"),
                        rs.getString("customer_name"),
                        rs.getString("contact"),
                        rs.getString("status"),
                        rs.getString("pickup_date"),
                        rs.getDouble("total_amount"),
                        Math.max(0, rs.getDouble("balance"))));
            }
        } catch (SQLException e) {
            System.err.println("Error generating report: " + e.getMessage());
        }
        return list;
    }

    // datePrefix: "2026-11-05" or "2026-11"
    public int countOrders(String datePrefix) {
        String sql = "SELECT COUNT(*) FROM orders WHERE order_date LIKE ? AND status <> 'CANCELLED'";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, datePrefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting orders: " + e.getMessage());
        }
        return 0;
    }
}