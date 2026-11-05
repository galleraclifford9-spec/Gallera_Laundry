package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner sc;
    private final ActivityLogDao logDao = new ActivityLogDao();

    private final CustomerUI customerUI;
    private final ServiceUI serviceUI;
    private final OrderUI orderUI;
    private final PaymentUI paymentUI;
    private final ReportUI reportUI;
    private final ProfileUI profileUI;

    public AdminMenu(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
        InputHelper in = new InputHelper(sc);
        this.customerUI = new CustomerUI(userDao, in);
        this.serviceUI = new ServiceUI(in);
        this.orderUI = new OrderUI(in);
        this.paymentUI = new PaymentUI(in);
        this.reportUI = new ReportUI(in);
        this.profileUI = new ProfileUI(userDao, in);
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
                    userManagement(admin);
                    break;
                case "2":
                    customerUI.menu(admin);
                    break;
                case "3":
                    serviceUI.menu(admin);
                    break;
                case "4":
                    orderUI.adminMenu(admin);
                    break;
                case "5":
                    paymentUI.adminMenu(admin);
                    break;
                case "6":
                    reportUI.menu(admin);
                    break;
                case "7":
                    viewAuditLogs();
                    break;
                case "8":
                    profileUI.menu(admin);
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

    private void userManagement(User admin) {
        while (true) {
            System.out.println("\n------------- USER MANAGEMENT -------------");
            System.out.println("[1] View All System Users");
            System.out.println("[2] View Pending Registrations");
            System.out.println("[3] Approve User Account");
            System.out.println("[4] Update User Role");
            System.out.println("[5] Update User Status");
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
                    approveUserAccount(admin);
                    break;
                case "4":
                    handlingUpdateRole(admin);
                    break;
                case "5":
                    handlingUpdateStatus(admin);
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

    private void approveUserAccount(User admin) {
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

            if (findById(pending, targetId) == null) {
                System.out.println("Invalid! User ID " + targetId + " is not in the pending list.");
                continue;
            }

            if (userDao.approveUser(admin.user_id(), targetId)) {
                System.out.println("-> SUCCESS: User ID " + targetId + " has been APPROVED.");
            } else {
                System.out.println("-> Failed to approve User ID " + targetId + ".");
            }
            return;
        }
    }

    // ---------------- UPDATE ROLE ----------------

    private void handlingUpdateRole(User currentAdmin) {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n=== UPDATE USER ROLE ===");
        printUserTable(users);
        if (users.isEmpty()) return;

        int userId = readNum("Enter User ID to Update Role (0 to go back): ");
        if (userId == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        if (userId == currentAdmin.user_id()) {
            System.out.println(" -> [WARNING] Modifying the current active Admin is restricted.");
            return;
        }
        User target = findById(users, userId);
        if (target == null) {
            System.out.println(" -> [ERROR] User ID " + userId + " not found.");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        if (newRole.equalsIgnoreCase(target.role())) {
            System.out.println(" -> User ID " + userId + " is already " + newRole + ".");
            return;
        }

        userDao.updateRole(currentAdmin.user_id(), userId, target.role(), newRole);
    }

    private String readValidRole() {
        while (true) {
            String choice = readString("Choose new Role [1] ADMIN | [2] STAFF | [3] CUSTOMER | [0] Back: ");
            switch (choice) {
                case "1": return "ADMIN";
                case "2": return "STAFF";
                case "3": return "CUSTOMER";
                case "0": return null;
                default:  System.out.println(" -> [ERROR] Invalid, enter 0-3.");
            }
        }
    }

    // ---------------- UPDATE STATUS ----------------

    private void handlingUpdateStatus(User currentAdmin) {
        List<User> users = userDao.getAllUsers();
        System.out.println("\n=== UPDATE USER STATUS ===");
        printUserTable(users);
        if (users.isEmpty()) return;

        int userId = readNum("Enter User ID to Update Status (0 to go back): ");
        if (userId == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        if (userId == currentAdmin.user_id()) {
            System.out.println(" -> [WARNING] Modifying the current active Admin is restricted.");
            return;
        }
        User target = findById(users, userId);
        if (target == null) {
            System.out.println(" -> [ERROR] User ID " + userId + " not found.");
            return;
        }
        if ("PENDING".equalsIgnoreCase(target.status())) {
            System.out.println(" -> [ERROR] User is still pending. Use 'Approve User Account' instead.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        if (newStatus.equalsIgnoreCase(target.status())) {
            System.out.println(" -> User ID " + userId + " is already " + newStatus + ".");
            return;
        }

        userDao.updateStatus(currentAdmin.user_id(), userId, target.status(), newStatus);
    }

    private String readValidStatus() {
        while (true) {
            String choice = readString("Choose new Status [1] APPROVED | [2] INACTIVE | [0] Back: ");
            switch (choice) {
                case "1": return "APPROVED";
                case "2": return "INACTIVE";
                case "0": return null;
                default:  System.out.println(" -> [ERROR] Invalid, enter 0-2.");
            }
        }
    }

    // ---------------- AUDIT & ACTIVITY LOGS ----------------

    private void viewAuditLogs() {
        while (true) {
            List<String> types = logDao.getActionTypes();

            System.out.println("\n------------- AUDIT & ACTIVITY LOGS -------------");
            System.out.println("[1] All Logs");
            for (int i = 0; i < types.size(); i++) {
                System.out.println("[" + (i + 2) + "] " + types.get(i));
            }
            System.out.println("[0] Back");
            System.out.println("-------------------------------------------------");
            System.out.print("Enter choice: ");
            String raw = sc.nextLine().trim();

            int pick;
            try {
                pick = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Invalid, enter 0-" + (types.size() + 1) + ".");
                continue;
            }

            if (pick == 0) return;
            if (pick < 1 || pick > types.size() + 1) {
                System.out.println(" -> [ERROR] Invalid, enter 0-" + (types.size() + 1) + ".");
                continue;
            }

            String filter = (pick == 1) ? null : types.get(pick - 2);
            List<ActivityLog> logs = logDao.getLogs(filter);
            printLogTable(logs);
        }
    }

    private void printLogTable(List<ActivityLog> logs) {
        String border = InputHelper.border(5, 7, 19, 20, 70);
        String fmt = "| %-5s | %-7s | %-19s | %-20s | %-70s |%n";

        System.out.println(border);
        System.out.printf(fmt, "ID", "USER ID", "TIMESTAMP", "ACTION", "DESCRIPTION");
        System.out.println(border);

        if (logs.isEmpty()) {
            InputHelper.emptyRow(border);
        } else {
            for (ActivityLog l : logs) {
                System.out.printf(fmt,
                        l.id(),
                        l.user_id(),
                        l.timestamp(),
                        l.action_type(),
                        truncate(l.description(), 70));
            }
        }

        System.out.println(border);
        System.out.println("Total: " + logs.size());
    }

    // ---------------- TABLE HELPER ----------------

    private void printUserTable(List<User> users) {
        String border = "+-------+--------------------------------+------------+------------+";
        String format = "| %-5s | %-30s | %-10s | %-10s |%n";

        System.out.println(border);
        System.out.printf(format, "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);

        if (users.isEmpty()) {
            System.out.printf("| %-64s |%n", "No records found.");
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

    // ---------------- INPUT / LOOKUP HELPERS ----------------

    private User findById(List<User> users, int id) {
        for (User u : users) {
            if (u.user_id() == id) return u;
        }
        return null;
    }

    private int readNum(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Please enter a valid numeric ID.");
            }
        }
    }

    private String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("It must not be empty.");
        }
    }

}