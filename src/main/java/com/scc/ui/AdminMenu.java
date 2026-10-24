package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner sc;

    public AdminMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    public void display(User admin) {
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
                    userManagement();
                    break;
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

    // ---------------- USER MANAGEMENT ----------------

    private void userManagement() {
        while (true) {
            System.out.println("\n------------- USER MANAGEMENT -------------");
            System.out.println("[1] View All System Users");
            System.out.println("[2] View Pending Registrations");
            System.out.println("[3] Approve User Account");
            System.out.println("[0] Back");
            System.out.println("-------------------------------------------");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    viewAllUsers();
                    break;
                case "2":
                    viewPendingUsers();
                    break;
                case "3":
                    approveUserAccount();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n=== ALL SYSTEM USERS ===");
        printUserTable(users);
    }

    private void viewPendingUsers() {
        List<User> pending = userDao.getPendingUsers();
        System.out.println("\n=== PENDING REGISTRATIONS ===");
        printUserTable(pending);
    }

    private void approveUserAccount() {
        List<User> pending = userDao.getPendingUsers();
        System.out.println("\n=== APPROVE USER ACCOUNT ===");

        if (pending.isEmpty()) {
            System.out.println("-> No pending accounts to approve.");
            return;
        }

        printUserTable(pending);

        while (true) {
            System.out.print("Enter Target User ID to approve (0 to cancel): ");
            String input = sc.nextLine().trim();

            int targetId;
            try {
                targetId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid! Please enter a valid number.");
                continue;
            }

            if (targetId == 0) {
                System.out.println("-> Approval cancelled.");
                return;
            }

            boolean inPendingList = false;
            for (User u : pending) {
                if (u.user_id() == targetId) {
                    inPendingList = true;
                    break;
                }
            }

            if (!inPendingList) {
                System.out.println("Invalid! User ID " + targetId + " is not in the pending list.");
                continue;
            }

            if (userDao.approveUser(targetId)) {
                System.out.println("-> SUCCESS: User ID " + targetId + " has been APPROVED.");
            } else {
                System.out.println("-> Failed to approve User ID " + targetId + ".");
            }
            return;
        }
    }

    // ---------------- TABLE HELPER ----------------

    private void printUserTable(List<User> users) {
        String border = "+-------+--------------------------------+------------+------------+";
        String format = "| %-5s | %-30s | %-10s | %-10s |%n";

        System.out.println(border);
        System.out.printf(format, "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);

        if (users.isEmpty()) {
            System.out.printf("| %-68s |%n", "No records found.");
        } else {
            for (User u : users) {
                System.out.printf(format,
                        u.user_id(),
                        truncate(u.username(), 30),
                        u.role(),
                        u.status());
            }
        }

        System.out.println(border);
        System.out.println("Total: " + users.size());
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}