package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner sc;

    public AdminMenu(UserDao userDao, Scanner sc){
        this.userDao = userDao;
        this.sc = sc;
    }

    public void display(User admin){
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("          ADMINISTRATOR MENU");
            System.out.println("==========================================");
            System.out.println("Logged in as: " + admin.full_name() + " (Administrator)\n");
            System.out.println("[1] User Management");
            System.out.println("[2] Customer Management");
            System.out.println("[3] Service Management");
            System.out.println("[4] Order Management");
            System.out.println("[5] Payment Management");
            System.out.println("[6] Reports");
            System.out.println("[7] Audit & Activity Logs");
            System.out.println("[8] My Profile / Change Password");
            System.out.println("[0] Logout");
            System.out.println("==========================================");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                case "2":
                case "3":
                case "4":
                case "5":
                case "6":
                case "7":
                case "8":
                    System.out.println("\nComing soon...");
                    break;
                case "0":
                    System.out.println("\nLogged out successfully.");
                    return;
                default:
                    System.out.println("Invalid Choice, Try Again.");
            }
        }
    }
}
