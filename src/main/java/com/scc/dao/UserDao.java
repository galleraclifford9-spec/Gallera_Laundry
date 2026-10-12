package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static sun.security.util.KnownOIDs.Data;

public class UserDao {

    public boolean registerUser(String username, String password, String role){

        String sql = "INSERT INTO users (username, password, fullname, role ,status) VALUES (?,?,?,? 'PENDING')";

        try (Connection conn = DatabaseConnection.getconnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, username);
            pstmt.setString(2,password);
            pstmt.setString(3,fullname);
            pstmt.setString(4,role.toUpperCase());
            pstmt.executeUpdate();
        } catch (SQLException e ){
            System.err.println("Failed: " + e.getMessage());
        }
    }
}
