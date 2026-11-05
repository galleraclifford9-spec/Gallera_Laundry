package com.scc.dao;

import com.scc.model.OrderLine;
import com.scc.model.OrderSummary;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    public static final String[] FLOW = {"RECEIVED", "WASHING", "DRYING", "READY", "CLAIMED"};
    public static final String[] ALL_STATUS = {"RECEIVED", "WASHING", "DRYING", "READY", "CLAIMED", "CANCELLED"};

    private static final String BASE_SELECT =
            "SELECT o.order_id, o.customer_id, c.full_name AS customer_name, s.full_name AS staff_name, "
                    + "o.weight_kg, o.total_amount, "
                    + "COALESCE((SELECT SUM(p.amount_paid) FROM payments p WHERE p.order_id = o.order_id), 0) AS paid, "
                    + "o.status, o.order_date, o.pickup_date, o.remarks "
                    + "FROM orders o "
                    + "JOIN users c ON c.user_id = o.customer_id "
                    + "JOIN users s ON s.user_id = o.staff_id ";

    private OrderSummary map(ResultSet rs) throws SQLException {
        return new OrderSummary(
                rs.getInt("order_id"),
                rs.getInt("customer_id"),
                rs.getString("customer_name"),
                rs.getString("staff_name"),
                rs.getDouble("weight_kg"),
                rs.getDouble("total_amount"),
                rs.getDouble("paid"),
                rs.getString("status"),
                rs.getString("order_date"),
                rs.getString("pickup_date"),
                rs.getString("remarks"));
    }

    // Any filter can be null. date format: YYYY-MM-DD
    public List<OrderSummary> getOrders(String status, Integer customerId, String date) {
        List<OrderSummary> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();

        if (status != null) {
            where.add("o.status = ?");
            params.add(status);
        }
        if (customerId != null) {
            where.add("o.customer_id = ?");
            params.add(customerId);
        }
        if (date != null) {
            where.add("date(o.order_date) = ?");
            params.add(date);
        }
        if (!where.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        }
        sql.append("ORDER BY o.order_id DESC");

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
            System.err.println("Error fetching orders: " + e.getMessage());
        }
        return list;
    }

    public OrderSummary getOrderById(int orderId) {
        String sql = BASE_SELECT + "WHERE o.order_id = ?";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching order: " + e.getMessage());
        }
        return null;
    }

    public List<OrderLine> getItems(int orderId) {
        List<OrderLine> items = new ArrayList<>();
        String sql = "SELECT oi.service_id, s.service_name, s.pricing_type, oi.quantity, oi.unit_price "
                + "FROM order_items oi JOIN services s ON s.service_id = oi.service_id "
                + "WHERE oi.order_id = ? ORDER BY oi.order_item_id";
        try (Connection conn = DatabaseConnection.getconnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderLine(
                            rs.getInt("service_id"),
                            rs.getString("service_name"),
                            rs.getString("pricing_type"),
                            rs.getDouble("quantity"),
                            rs.getDouble("unit_price")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching order items: " + e.getMessage());
        }
        return items;
    }

    // CORE BUSINESS PROCESS: returns order id, or -1 if failed
    public int createOrder(int staffId, int customerId, List<OrderLine> lines, String pickupDate, String remarks) {
        String nameSql = "SELECT full_name FROM users WHERE user_id = ? AND role = 'CUSTOMER' AND status = 'APPROVED'";
        String insOrder = "INSERT INTO orders (customer_id, staff_id, weight_kg, total_amount, status, pickup_date, remarks) "
                + "VALUES (?,?,?,?, 'RECEIVED', ?, ?)";
        String insItem = "INSERT INTO order_items (order_id, service_id, quantity, unit_price, subtotal) VALUES (?,?,?,?,?)";

        double total = 0;
        double kg = 0;
        for (OrderLine l : lines) {
            total += l.subtotal();
            if ("PER_KG".equals(l.pricing_type())) kg += l.quantity();
        }

        int orderId = -1;
        String customerName = null;

        try (Connection conn = DatabaseConnection.getconnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(nameSql)) {
                    ps.setInt(1, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println(" -> Customer ID not found / not approved.");
                            conn.rollback();
                            return -1;
                        }
                        customerName = rs.getString(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(insOrder, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, customerId);
                    ps.setInt(2, staffId);
                    ps.setDouble(3, kg);
                    ps.setDouble(4, total);
                    ps.setString(5, pickupDate);
                    ps.setString(6, remarks);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(insItem)) {
                    for (OrderLine l : lines) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, l.service_id());
                        ps.setDouble(3, l.quantity());
                        ps.setDouble(4, l.unit_price());
                        ps.setDouble(5, l.subtotal());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error creating order: " + e.getMessage());
            return -1;
        }

        logDao.log(staffId, "ORDER_CREATED",
                String.format("Created Order #%d for Customer %s - Total: P%.2f", orderId, customerName, total));
        return orderId;
    }

    // LIFECYCLE / STATE CHANGE
    public boolean updateOrderStatus(int staffId, int orderId, String newStatus) {
        String selSql = "SELECT status FROM orders WHERE order_id = ?";
        String updSql = "UPDATE orders SET status = ? WHERE order_id = ?";

        try (Connection conn = DatabaseConnection.getconnection()) {
            String oldStatus;
            try (PreparedStatement ps = conn.prepareStatement(selSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println(" -> Order not found.");
                        return false;
                    }
                    oldStatus = rs.getString(1);
                }
            }

            if ("CANCELLED".equals(oldStatus)) {
                System.out.println(" -> Order #" + orderId + " is CANCELLED and can no longer be updated.");
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(updSql)) {
                ps.setString(1, newStatus);
                ps.setInt(2, orderId);
                ps.executeUpdate();
            }

            logDao.log(staffId, "ORDER_STATUS",
                    "Updated Order #" + orderId + " status from " + oldStatus + " to " + newStatus);
            return true;
        } catch (SQLException e) {
            System.err.println("Error updating order: " + e.getMessage());
            return false;
        }
    }

    // SOFT DELETE: only pending (RECEIVED) orders can be cancelled
    public boolean cancelOrder(int staffId, int orderId) {
        String selSql = "SELECT status FROM orders WHERE order_id = ?";
        String updSql = "UPDATE orders SET status = 'CANCELLED' WHERE order_id = ?";

        try (Connection conn = DatabaseConnection.getconnection()) {
            String oldStatus;
            try (PreparedStatement ps = conn.prepareStatement(selSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println(" -> Order not found.");
                        return false;
                    }
                    oldStatus = rs.getString(1);
                }
            }

            if (!"RECEIVED".equals(oldStatus)) {
                System.out.println(" -> Only RECEIVED (pending) orders can be cancelled. Current status: " + oldStatus);
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(updSql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            logDao.log(staffId, "ORDER_CANCELLED",
                    "Cancelled Order #" + orderId + " (status changed from " + oldStatus + " to CANCELLED)");
            return true;
        } catch (SQLException e) {
            System.err.println("Error cancelling order: " + e.getMessage());
            return false;
        }
    }
}