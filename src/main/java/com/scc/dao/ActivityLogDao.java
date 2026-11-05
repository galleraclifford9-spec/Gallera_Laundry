package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    public void log(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error writing log: " + e.getMessage());
        }
    }

    // actionType == null -> all logs
    public List<ActivityLog> getLogs(String actionType) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT l.id, l.user_id, u.username, l.action_type, l.description, l.timestamp "
                + "FROM activity_logs l JOIN users u ON u.user_id = l.user_id "
                + (actionType != null ? "WHERE l.action_type = ? " : "")
                + "ORDER BY l.id DESC";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (actionType != null) ps.setString(1, actionType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("action_type"),
                            rs.getString("description"),
                            rs.getString("timestamp")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching logs: " + e.getMessage());
        }
        return logs;
    }

    // Para sa dynamic filter menu sa Audit Logs
    public List<String> getActionTypes() {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT action_type FROM activity_logs ORDER BY action_type";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                types.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching action types: " + e.getMessage());
        }
        return types;
    }
}