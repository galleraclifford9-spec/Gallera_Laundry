package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLDataException;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:Laundry Service.db";

    public static Connection getconnection() throws SQLException{
        return DriverManager.getConnection(DB_URL);
    }
}
