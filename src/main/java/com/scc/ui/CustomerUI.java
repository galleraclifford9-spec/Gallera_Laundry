package com.scc.ui;

import com.scc.dao.CustomerDao;
import com.scc.dao.OrderDao;
import com.scc.dao.PaymentDao;
import com.scc.dao.UserDao;
import com.scc.model.Customer;
import com.scc.model.OrderSummary;
import com.scc.model.Payment;
import com.scc.model.User;

import java.util.List;

public class CustomerUI {

    private final UserDao userDao;
    private final CustomerDao customerDao = new CustomerDao();
    private final OrderDao orderDao = new OrderDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final InputHelper in;

    public CustomerUI(UserDao userDao, InputHelper in) {
        this.userDao = userDao;
        this.in = in;
    }

    // ADMIN: Customer Management
    public void menu(User admin) {
        while (true) {
            System.out.println("\n------------- CUSTOMER MANAGEMENT -------------");
            System.out.println("[1] View / Search Customers");
            System.out.println("[2] Register New Customer");
            System.out.println("[3] Edit Customer Details");
            System.out.println("[4] Customer Order & Payment History");
            System.out.println("[5] Activate / Deactivate Customer");
            System.out.println("[0] Back");
            System.out.println("-----------------------------------------------");
            String choice = in.line("Enter choice: ");

            switch (choice) {
                case "1": searchCustomers(); break;
                case "2": registerCustomer(admin); break;
                case "3": editCustomer(admin); break;
                case "4": history(); break;
                case "5": changeStatus(admin); break;
                case "0": return;
                default: System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    public void searchCustomers() {
        System.out.println("\n=== SEARCH CUSTOMERS ===");
        String keyword = in.line("Search by name / email / contact number (blank = all): ");
        List<Customer> list = customerDao.search(keyword);
        printCustomerTable(list);
    }

    public void registerCustomer(User actor) {
        System.out.println("\n=== REGISTER NEW CUSTOMER ===");

        String fullName = in.line("Full name (0 to cancel): ");
        if (fullName.equals("0")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        while (fullName.isEmpty()) {
            System.out.println(" -> [ERROR] Full name cannot be empty.");
            fullName = in.line("Full name (0 to cancel): ");
            if (fullName.equals("0")) {
                System.out.println(" -> Operation Cancelled.");
                return;
            }
        }

        String username;
        while (true) {
            username = in.line("Customer username (with @gmail.com): ").toLowerCase();
            if (!username.endsWith("@gmail.com") || username.length() <= "@gmail.com".length() || username.contains(" ")) {
                System.out.println(" -> [ERROR] Enter a valid Gmail without spaces (example: lito@gmail.com).");
            } else if (userDao.usernameExists(username)) {
                System.out.println(" -> [ERROR] Username already taken.");
            } else {
                break;
            }
        }

        String password;
        while (true) {
            password = in.line("Temporary password (min 5 chars): ");
            if (password.length() >= 5) break;
            System.out.println(" -> [ERROR] Password must be at least 5 characters.");
        }

        String contact = in.readString("Contact number: ");
        String address = in.line("Address (optional): ");

        int id = customerDao.registerCustomer(actor.user_id(), fullName, username, password, contact, address);
        if (id > 0) {
            System.out.println(" -> Customer registered. User ID: " + id + " (status: APPROVED)");
        }
    }

    public void editCustomer(User actor) {
        System.out.println("\n=== EDIT CUSTOMER DETAILS ===");
        List<Customer> list = customerDao.search("");
        printCustomerTable(list);
        if (list.isEmpty()) return;

        Customer c = pickCustomer(list, "Customer User ID to edit (0 to cancel): ");
        if (c == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        System.out.println("(Press Enter to keep the current value)");
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
        if (customerDao.updateDetails(actor.user_id(), c, name, contact, address)) {
            System.out.println(" -> Customer details updated.");
        }
    }

    public void history() {
        System.out.println("\n=== CUSTOMER ORDER & PAYMENT HISTORY ===");
        List<Customer> list = customerDao.search("");
        printCustomerTable(list);
        if (list.isEmpty()) return;

        Customer c = pickCustomer(list, "Customer User ID (0 to cancel): ");
        if (c == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        System.out.println("\n--- ORDERS of " + c.full_name() + " ---");
        List<OrderSummary> orders = orderDao.getOrders(null, c.user_id(), null);
        OrderUI.printOrderTable(orders);

        System.out.println("\n--- PAYMENTS of " + c.full_name() + " ---");
        List<Payment> payments = paymentDao.getPayments(c.user_id(), null, null);
        PaymentUI.printPaymentTable(payments);
    }

    private void changeStatus(User admin) {
        System.out.println("\n=== ACTIVATE / DEACTIVATE CUSTOMER ===");
        List<Customer> list = customerDao.search("");
        printCustomerTable(list);
        if (list.isEmpty()) return;

        Customer c = pickCustomer(list, "Customer User ID (0 to cancel): ");
        if (c == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        if ("PENDING".equalsIgnoreCase(c.status())) {
            System.out.println(" -> [ERROR] Customer is still pending. Use User Management > Approve User Account.");
            return;
        }

        String newStatus = "APPROVED".equalsIgnoreCase(c.status()) ? "INACTIVE" : "APPROVED";
        String action = newStatus.equals("INACTIVE") ? "DEACTIVATE" : "ACTIVATE";
        if (!in.confirm(action + " " + c.full_name() + "? (order history is kept)")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        userDao.updateStatus(admin.user_id(), c.user_id(), c.status(), newStatus);
    }

    private Customer pickCustomer(List<Customer> list, String prompt) {
        while (true) {
            int id = in.readInt(prompt);
            if (id == 0) return null;
            for (Customer c : list) {
                if (c.user_id() == id) return c;
            }
            System.out.println(" -> [ERROR] Customer ID " + id + " not found.");
        }
    }

    public static void printCustomerTable(List<Customer> list) {
        String border = InputHelper.border(5, 22, 26, 13, 22, 9);
        String fmt = "| %-5s | %-22s | %-26s | %-13s | %-22s | %-9s |%n";

        System.out.println(border);
        System.out.printf(fmt, "ID", "NAME", "USERNAME", "CONTACT", "ADDRESS", "STATUS");
        System.out.println(border);

        if (list.isEmpty()) {
            InputHelper.emptyRow(border);
        } else {
            for (Customer c : list) {
                System.out.printf(fmt,
                        c.user_id(),
                        InputHelper.truncate(c.full_name(), 22),
                        InputHelper.truncate(c.username(), 26),
                        InputHelper.truncate(c.contact_number(), 13),
                        InputHelper.truncate(c.address(), 22),
                        c.status());
            }
        }
        System.out.println(border);
        System.out.println("Total: " + list.size());
    }
}