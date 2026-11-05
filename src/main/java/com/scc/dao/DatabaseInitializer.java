package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        String createUsers = """
            CREATE TABLE IF NOT EXISTS users (
                user_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name    TEXT NOT NULL,
                username     TEXT NOT NULL UNIQUE,
                password     TEXT NOT NULL,
                role         TEXT NOT NULL CHECK (role IN ('ADMIN','STAFF','CUSTOMER')),
                status       TEXT NOT NULL DEFAULT 'PENDING',
                date_created TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createActivityLogs = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id     INTEGER NOT NULL,
                action_type TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp   TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            )
            """;

        String createCustomers = """
            CREATE TABLE IF NOT EXISTS customers (
                customer_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id         INTEGER NOT NULL UNIQUE,
                contact_number  TEXT,
                address         TEXT,
                date_registered TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            )
            """;

        String createServices = """
            CREATE TABLE IF NOT EXISTS services (
                service_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                service_name TEXT NOT NULL UNIQUE,
                description  TEXT,
                price        REAL NOT NULL,
                pricing_type TEXT NOT NULL CHECK (pricing_type IN ('PER_KG','PER_PIECE')),
                is_available INTEGER NOT NULL DEFAULT 1
            )
            """;

        String createOrders = """
            CREATE TABLE IF NOT EXISTS orders (
                order_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id  INTEGER NOT NULL,
                staff_id     INTEGER NOT NULL,
                weight_kg    REAL NOT NULL,
                total_amount REAL NOT NULL,
                status       TEXT NOT NULL DEFAULT 'RECEIVED',
                order_date   TEXT DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (customer_id) REFERENCES users(user_id),
                FOREIGN KEY (staff_id)    REFERENCES users(user_id)
            )
            """;

        String createOrderItems = """
            CREATE TABLE IF NOT EXISTS order_items (
                order_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id      INTEGER NOT NULL,
                service_id    INTEGER NOT NULL,
                quantity      REAL NOT NULL,
                unit_price    REAL NOT NULL,
                subtotal      REAL NOT NULL,
                FOREIGN KEY (order_id)   REFERENCES orders(order_id) ON DELETE CASCADE,
                FOREIGN KEY (service_id) REFERENCES services(service_id)
            )
            """;

        String createPayments = """
            CREATE TABLE IF NOT EXISTS payments (
                payment_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id       INTEGER NOT NULL,
                received_by    INTEGER NOT NULL,
                amount_paid    REAL NOT NULL,
                payment_method TEXT NOT NULL DEFAULT 'CASH',
                payment_date   TEXT DEFAULT (datetime('now','localtime')),
                receipt_number TEXT NOT NULL UNIQUE,
                FOREIGN KEY (order_id)    REFERENCES orders(order_id),
                FOREIGN KEY (received_by) REFERENCES users(user_id)
            )
            """;

        try (Connection conn = DatabaseConnection.getconnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createUsers);
            stmt.execute(createActivityLogs);
            stmt.execute(createCustomers);
            stmt.execute(createServices);
            stmt.execute(createOrders);
            stmt.execute(createOrderItems);
            stmt.execute(createPayments);

            // Migration: daan nga orders table walay pickup_date/remarks
            addColumnIfMissing(conn, "orders", "pickup_date", "TEXT");
            addColumnIfMissing(conn, "orders", "remarks", "TEXT");

            seedServices(conn);

            System.out.println("-> Database initialized.");
            System.out.println("-> DB file: " + DatabaseConnection.getDbPath());
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String type)
            throws SQLException {
        boolean found = false;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            }
        }
    }

    private static void seedServices(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM services")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        String sql = "INSERT INTO services (service_name, description, price, pricing_type) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Object[][] data = {
                    {"Wash & Fold", "Wash, dry and fold", 50.0, "PER_KG"},
                    {"Ironing", "Press and iron", 30.0, "PER_KG"},
                    {"Dry Cleaning", "Professional dry cleaning", 150.0, "PER_PIECE"}
            };
            for (Object[] d : data) {
                ps.setString(1, (String) d[0]);
                ps.setString(2, (String) d[1]);
                ps.setDouble(3, (Double) d[2]);
                ps.setString(4, (String) d[3]);
                ps.addBatch();
            }
            ps.executeBatch();
            System.out.println("-> Default services created");
        }
    }
}