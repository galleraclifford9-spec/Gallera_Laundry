package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class StaffMenu {

    private final Scanner sc;

    private final CustomerUI customerUI;
    private final OrderUI orderUI;

    public StaffMenu(UserDao userDao, Scanner sc) {
        this.sc = sc;
        InputHelper in = new InputHelper(sc);
        this.customerUI = new CustomerUI(userDao, in);
        this.orderUI = new OrderUI(in);
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
            System.out.println("[4] Search Customer / View Orders");
            System.out.println("[5] Cancel Order");
            System.out.println("[0] Logout");
            System.out.println("==========================================");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    customerUI.registerCustomer(staff);
                    break;
                case "2":
                    orderUI.createOrder(staff);
                    break;
                case "3":
                    orderUI.updateOrderStatus(staff);
                    break;
                case "4":
                    searchMenu(staff);
                    break;
                case "5":
                    orderUI.cancelOrder(staff);
                    break;
                case "0":
                    System.out.println("\nLogged out successfully.");
                    return;
                default:
                    System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    private void searchMenu(User staff) {
        while (true) {
            System.out.println("\n------------- SEARCH / VIEW -------------");
            System.out.println("[1] Search Customers (name / contact)");
            System.out.println("[2] View / Filter Orders");
            System.out.println("[3] Customer Order History");
            System.out.println("[4] Edit Customer Details");
            System.out.println("[0] Back");
            System.out.println("-----------------------------------------");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    customerUI.searchCustomers();
                    break;
                case "2":
                    orderUI.viewOrders();
                    break;
                case "3":
                    customerUI.history();
                    break;
                case "4":
                    customerUI.editCustomer(staff);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid Choice, Try Again.");
            }
        }
    }
}