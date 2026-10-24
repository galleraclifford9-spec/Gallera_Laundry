package com.scc.dao;

import java.sql.Connection;
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

        try (Connection conn = DatabaseConnection.getconnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createUsers);

            // Idugang diri ang ubang tables gikan sa imong ERD.
            // Parent tables una, dayon ang naay FOREIGN KEY. Pananglitan:
            // stmt.execute("CREATE TABLE IF NOT EXISTS services (...)");
            // stmt.execute("CREATE TABLE IF NOT EXISTS orders (..., FOREIGN KEY (customer_id) REFERENCES users(user_id))");

            System.out.println("-> Database initialized.");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }
}