package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class StaffMenu {

    private final UserDao userDao;
    private final Scanner sc;

    public StaffMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    public void display(User staff) {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("          LAUNDRY STAFF MENU");
            System.out.println("==========================================");
            System.out.println("Logged in as: " + staff.full_name() + " (Staff)\n");
            System.out.println("[1] Register Customer");
            System.out.println("[2] Create Order");
            System.out.println("[3] Update Order Status");
            System.out.println("[4] Record Payment");
            System.out.println("[5] Search Customer / View Orders");
            System.out.println("[6] Cancel Order");
            System.out.println("[7] Daily Report");
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