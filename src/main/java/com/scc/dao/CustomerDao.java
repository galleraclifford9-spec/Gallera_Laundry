package com.scc.dao;

import com.scc.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    private static final String BASE_SELECT =
            "SELECT u.user_id, u.full_name, u.username, "
                    + "COALESCE(c.contact_number, '') AS contact_number, "
                    + "COALESCE(c.address, '') AS address, u.status "
                    + "FROM users u LEFT JOIN customers c ON c.user_id = u.user_id "
                    + "WHERE u.role = 'CUSTOMER' ";

    private Customer map(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("username"),
                rs.getString("contact_number"),
                rs.getString("address"),
                rs.getString("status"));
    }

    // keyword empty/null -> all customers
    public List<Customer> search(String keyword) {
        List<Customer> list = new ArrayList<>();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String sql = BASE_SELECT
                + (hasKeyword ? "AND (u.full_name LIKE ? OR u.username LIKE ? OR COALESCE(c.contact_number,'') LIKE ?) " : "")
                + "ORDER BY u.user_id";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (hasKeyword) {
                String like = "%" + keyword.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching customers: " + e.getMessage());
        }
        return list;
    }

    public Customer getById(int userId) {
        String sql = BASE_SELECT + "AND u.user_id = ?";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching customer: " + e.getMessage());
        }
        return null;
    }

    // Returns new user_id, or -1 if failed
    public int registerCustomer(int actorId, String fullName, String username, String password,
                                String contact, String address) {
        String userSql = "INSERT INTO users (full_name, username, password, role, status) VALUES (?,?,?, 'CUSTOMER', 'APPROVED')";
        String custSql = "INSERT INTO customers (user_id, contact_number, address) VALUES (?,?,?)";
        int newId = -1;

        try (Connection conn = DatabaseConnection.getconnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, fullName);
                    ps.setString(2, username);
                    ps.setString(3, password);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(custSql)) {
                    ps.setInt(1, newId);
                    ps.setString(2, contact);
                    ps.setString(3, address);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error registering customer: " + e.getMessage());
            return -1;
        }

        logDao.log(actorId, "CUSTOMER_REGISTERED",
                "User " + actorId + " registered Customer " + fullName + " (ID: " + newId + ")");
        return newId;
    }

    public boolean updateDetails(int actorId, Customer old, String fullName, String contact, String address) {
        String userSql = "UPDATE users SET full_name = ? WHERE user_id = ?";
        String ensureSql = "INSERT OR IGNORE INTO customers (user_id) VALUES (?)";
        String custSql = "UPDATE customers SET contact_number = ?, address = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getconnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                    ps.setString(1, fullName);
                    ps.setInt(2, old.user_id());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(ensureSql)) {
                    ps.setInt(1, old.user_id());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(custSql)) {
                    ps.setString(1, contact);
                    ps.setString(2, address);
                    ps.setInt(3, old.user_id());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error updating customer: " + e.getMessage());
            return false;
        }

        logDao.log(actorId, "CUSTOMER_UPDATED",
                "User " + actorId + " updated Customer " + old.user_id()
                        + " (old: " + old.full_name() + " | " + old.contact_number() + " | " + old.address()
                        + " -> new: " + fullName + " | " + contact + " | " + address + ")");
        return true;
    }
}