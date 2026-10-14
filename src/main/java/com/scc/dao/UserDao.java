package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    public boolean registerUser(String full_name, String username, String password, String role){

        String sql = "INSERT INTO users (full_name, username, password, role ,status) VALUES(?,?,?,?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getconnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, full_name);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role.toUpperCase());
            pstmt.executeUpdate();
            System.out.println("\n -> Successful Registration, Waiting for Approval!");
            return true;
        } catch (SQLException e ){
            System.err.println("Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean usernameExists(String username){
        String sql = "SELECT 2 FROM users WHERE username = ?";

        try(Connection conn = DatabaseConnection.getconnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, username);
            try(ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        } catch (SQLException e){
            System.out.println("\nDatabase error: " + e.getMessage());
            return true;
        }
    }

    public User login(String username, String password){
        String sql = "SELECT user_id, full_name, username, password, role, status, date_created FROM users WHERE username = ? AND password = ?";

        try(Connection conn = DatabaseConnection.getconnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try(ResultSet rs = pstmt.executeQuery()){
                if(rs.next()){
                    String status = rs.getString("status");
                    if("PENDING".equalsIgnoreCase(status)){
                        System.out.println("-> Account is pending, Waiting for Approval");
                        return null;
                    }
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("full_name"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            status,
                            rs.getString("date_created")
                    );
                }else {
                    System.out.println(" -> Invalid username or password");
                }
            }
        }catch (SQLException e){
            System.err.println("Error during login " + e.getMessage());
        }
        return null;
    }
}
