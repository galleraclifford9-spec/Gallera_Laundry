package com.scc.dao;

import com.scc.model.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    private Service map(ResultSet rs) throws SQLException {
        return new Service(
                rs.getInt("service_id"),
                rs.getString("service_name"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getString("pricing_type"),
                rs.getInt("is_available") == 1);
    }

    public List<Service> getAll(boolean onlyAvailable) {
        List<Service> list = new ArrayList<>();
        String sql = "SELECT service_id, service_name, description, price, pricing_type, is_available FROM services "
                + (onlyAvailable ? "WHERE is_available = 1 " : "")
                + "ORDER BY service_id";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching services: " + e.getMessage());
        }
        return list;
    }

    public boolean addService(int userId, String name, String description, double price, String pricingType) {
        String sql = "INSERT INTO services (service_name, description, price, pricing_type) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, price);
            ps.setString(4, pricingType);
            ps.executeUpdate();
            logDao.log(userId, "SERVICE_ADDED",
                    String.format("Added service %s at P%.2f (%s)", name, price, pricingType));
            return true;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                System.out.println(" -> Service name already exists.");
            } else {
                System.err.println("Error adding service: " + e.getMessage());
            }
            return false;
        }
    }

    public boolean updatePrice(int userId, Service service, double newPrice) {
        String sql = "UPDATE services SET price = ? WHERE service_id = ?";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, service.service_id());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                logDao.log(userId, "SERVICE_PRICE",
                        String.format("Updated service %s price from P%.2f to P%.2f",
                                service.service_name(), service.price(), newPrice));
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Error updating price: " + e.getMessage());
            return false;
        }
    }

    public boolean setAvailability(int userId, Service service, boolean available) {
        String sql = "UPDATE services SET is_available = ? WHERE service_id = ?";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setInt(2, service.service_id());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                logDao.log(userId, "SERVICE_AVAILABILITY",
                        "Service " + service.service_name() + " set to " + (available ? "AVAILABLE" : "INACTIVE"));
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Error updating service: " + e.getMessage());
            return false;
        }
    }
}