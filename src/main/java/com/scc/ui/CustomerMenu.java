package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class CustomerMenu {

    private final Scanner sc;

    private final OrderUI orderUI;
    private final PaymentUI paymentUI;
    private final ServiceUI serviceUI;
    private final ProfileUI profileUI;

    public CustomerMenu(UserDao userDao, Scanner sc) {
        this.sc = sc;
        InputHelper in = new InputHelper(sc);
        this.orderUI = new OrderUI(in);
        this.paymentUI = new PaymentUI(in);
        this.serviceUI = new ServiceUI(in);
        this.profileUI = new ProfileUI(userDao, in);
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
                    orderUI.viewMyOrders(customer);
                    break;
                case "2":
                    orderUI.trackOrder(customer);
                    break;
                case "3":
                    paymentUI.paymentHistory(customer);
                    break;
                case "4":
                    serviceUI.viewServices(true);
                    break;
                case "5":
                    profileUI.menu(customer);
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