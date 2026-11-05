package com.scc.ui;

import com.scc.dao.CustomerDao;
import com.scc.dao.UserDao;
import com.scc.model.Customer;
import com.scc.model.User;

public class ProfileUI {

    private final UserDao userDao;
    private final CustomerDao customerDao = new CustomerDao();
    private final InputHelper in;

    public ProfileUI(UserDao userDao, InputHelper in) {
        this.userDao = userDao;
        this.in = in;
    }

    public void menu(User user) {
        while (true) {
            System.out.println("\n------------- MY PROFILE -------------");
            System.out.println("[1] View My Profile");
            System.out.println("[2] Edit Profile");
            System.out.println("[3] Change Password");
            System.out.println("[0] Back");
            System.out.println("--------------------------------------");
            String choice = in.line("Enter choice: ");

            switch (choice) {
                case "1": viewProfile(user); break;
                case "2": editProfile(user); break;
                case "3": changePassword(user); break;
                case "0": return;
                default: System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    private void viewProfile(User user) {
        User u = userDao.getUserById(user.user_id());
        if (u == null) {
            System.out.println(" -> [ERROR] Account not found.");
            return;
        }
        System.out.println("\n=== MY PROFILE ===");
        System.out.println("User ID   : " + u.user_id());
        System.out.println("Full name : " + u.full_name());
        System.out.println("Username  : " + u.username());
        System.out.println("Role      : " + u.role());
        System.out.println("Status    : " + u.status());
        System.out.println("Created   : " + u.date_created());

        if ("CUSTOMER".equals(u.role())) {
            Customer c = customerDao.getById(u.user_id());
            if (c != null) {
                System.out.println("Contact   : " + (c.contact_number().isEmpty() ? "-" : c.contact_number()));
                System.out.println("Address   : " + (c.address().isEmpty() ? "-" : c.address()));
            }
        }
    }

    private void editProfile(User user) {
        System.out.println("\n=== EDIT PROFILE ===");
        System.out.println("(Press Enter to keep the current value)");

        if ("CUSTOMER".equals(user.role())) {
            Customer c = customerDao.getById(user.user_id());
            if (c == null) {
                System.out.println(" -> [ERROR] Customer record not found.");
                return;
            }
            String name = in.line("Full name [" + c.full_name() + "]: ");
            String contact = in.line("Contact number [" + c.contact_number() + "]: ");
            String address = in.line("Address [" + c.address() + "]: ");

            if (name.isEmpty()) name = c.full_name();
            if (contact.isEmpty()) contact = c.contact_number();
            if (address.isEmpty()) address = c.address();

            if (name.equals(c.full_name()) && contact.equals(c.contact_number()) && address.equals(c.address())) {
                System.out.println(" -> No changes made.");
                return;
            }
            if (customerDao.updateDetails(user.user_id(), c, name, contact, address)) {
                System.out.println(" -> Profile updated. (Name change shows on your next login.)");
            }
        } else {
            User u = userDao.getUserById(user.user_id());
            String name = in.line("Full name [" + u.full_name() + "]: ");
            if (name.isEmpty() || name.equals(u.full_name())) {
                System.out.println(" -> No changes made.");
                return;
            }
            if (userDao.updateFullName(user.user_id(), name)) {
                System.out.println(" -> Profile updated. (Name change shows on your next login.)");
            }
        }
    }

    private void changePassword(User user) {
        System.out.println("\n=== CHANGE PASSWORD ===");
        String oldPass = in.line("Current password (0 to cancel): ");
        if (oldPass.equals("0")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        String newPass;
        while (true) {
            newPass = in.line("New password (min 5 chars): ");
            if (newPass.length() < 5) {
                System.out.println(" -> [ERROR] Password must be at least 5 characters.");
                continue;
            }
            String confirm = in.line("Confirm new password: ");
            if (!confirm.equals(newPass)) {
                System.out.println(" -> [ERROR] Passwords do not match.");
                continue;
            }
            break;
        }

        if (userDao.changePassword(user.user_id(), oldPass, newPass)) {
            System.out.println(" -> Password changed successfully.");
        }
    }
}