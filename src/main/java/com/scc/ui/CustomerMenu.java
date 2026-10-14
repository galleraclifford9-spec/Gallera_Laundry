package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class CustomerMenu {

    private final UserDao userDao;
    private final Scanner sc;

    public CustomerMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    public void display(User customer) {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("            CUSTOMER MENU");
            System.out.println("==========================================");
            System.out.println("Logged in as: " + customer.full_name() + " (Customer)\n");
            System.out.println("[1] My Orders");
            System.out.println("[2] Track Order Status");
            System.out.println("[3] Payment History");
            System.out.println("[4] Service Price List");
            System.out.println("[5] Edit Profile / Change Password");
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