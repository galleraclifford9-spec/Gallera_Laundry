package com.scc;

import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.AdminMenu;
import com.scc.ui.CustomerMenu;
import com.scc.ui.StaffMenu;


import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final UserDao userDao = new UserDao();

    public static void main() {

        while (true) {

            System.out.println("\n========================================");
            System.out.println("           LAUNDRY SERVICE SYSTEM");
            System.out.println("\n========================================");
            System.out.println("[1] Login");
            System.out.println("[2] Register");
            System.out.println("[3] Exit");
            System.out.print("Enter Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    handleLogin();
                    break;
                case "2":
                    registerUser();
                    break;
                case "3":
                    return;
                default:
                    System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    public static void handleLogin(){
        System.out.println("\n    ===== LOG IN =====");

        String username;
        while (true) {
            System.out.print("Enter Username: ");
            username = sc.nextLine().trim();

            if(username.isEmpty()){
                System.out.println("Invalid! Fullname cannot be empty.");
            }else {
                break;
            }
        }

        String password;
        while (true) {
            System.out.print("Enter Password: ");
            password = sc.nextLine().trim();

            if(password.isEmpty()){
                System.out.println("Invalid! Fullname cannot be empty.");
            }else{
                break;
            }
        }

        User user = userDao.login(username, password);

        if(user != null){
            System.out.println("\n -> Welcome "+user.full_name()+ " | ROLE: "+user.role()+"");
            switch (user.role()){
                case "ADMIN" -> new AdminMenu(userDao, sc).display(user);
                case "STAFF" -> new StaffMenu(userDao, sc).display(user);
                case "CUSTOMER" -> new CustomerMenu(userDao, sc).display(user);
            }
        }
    }

    public static void registerUser(){

        System.out.println("\n    ===== REGISTER =====");

        String full_name;
        while (true) {
            System.out.print("Enter your Fullname: ");
            full_name = sc.nextLine().trim();

            if (full_name.isEmpty()) {
                System.out.println("Invalid! Fullname cannot be empty.");
            } else {
                break;
            }
        }

        String user;
        while (true) {
            System.out.print("Enter username(with @gmail.com): ");
            user = sc.nextLine().trim().toLowerCase();

            if (!user.endsWith("@gmail.com") || user.length() <= "@gmail.com".length() || user.contains(" ")) {
                System.out.println("Invalid! Enter a valid Gmail without spaces (example: lito@gmail.com).");
            } else if (userDao.usernameExists(user)) {
                System.out.println("Invalid! Username already taken.");
            } else {
                break;
            }
        }

        String pass;
        while (true) {
            System.out.print("Enter Password: ");
            pass = sc.nextLine().trim();

            if (pass.length() < 5) {
                System.out.println("Invalid! Password must be at least 5 characters.");
                continue;
            }

            boolean confirmed = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                System.out.print("Confirm Password: ");
                String confirm = sc.nextLine().trim();

                if (confirm.equals(pass)) {
                    confirmed = true;
                    break;
                }
                System.out.println("\nPasswords do not match! Attempt " + attempt + " of 3.");
            }

            if (confirmed) {
                break;
            }
            System.out.println("Too many wrong attempts. Register Again.");
            return;
        }

        String role;
        while (true) {
            System.out.println("\n-- Select Role --");
            System.out.println("[1] ADMIN");
            System.out.println("[2] STAFF");
            System.out.println("[3] CUSTOMER");
            System.out.print("Enter Choice: ");
            String roleChoice = sc.nextLine().trim();

            if (roleChoice.equals("1")) {
                role = "ADMIN";
                break;
            } else if (roleChoice.equals("2")) {
                role = "STAFF";
                break;
            } else if (roleChoice.equals("3")) {
                role = "CUSTOMER";
                break;
            } else {
                System.out.println("Invalid! Please select 1, 2, or 3 only.");
            }
        }

        userDao.registerUser(full_name, user, pass, role);
    }

}