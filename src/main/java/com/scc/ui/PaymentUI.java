package com.scc.ui;

import com.scc.dao.OrderDao;
import com.scc.dao.PaymentDao;
import com.scc.model.OrderSummary;
import com.scc.model.Payment;
import com.scc.model.User;

import java.util.List;

public class PaymentUI {

    private final PaymentDao paymentDao = new PaymentDao();
    private final OrderDao orderDao = new OrderDao();
    private final InputHelper in;

    public PaymentUI(InputHelper in) {
        this.in = in;
    }

    // ADMIN: Payment Management
    public void adminMenu(User admin) {
        while (true) {
            System.out.println("\n------------- PAYMENT MANAGEMENT -------------");
            System.out.println("[1] Record Payment");
            System.out.println("[2] View All Payments");
            System.out.println("[3] View Payments of an Order");
            System.out.println("[0] Back");
            System.out.println("----------------------------------------------");
            String choice = in.line("Enter choice: ");

            switch (choice) {
                case "1": recordPayment(admin); break;
                case "2": viewAllPayments(); break;
                case "3": viewByOrder(); break;
                case "0": return;
                default: System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    public void recordPayment(User actor) {
        System.out.println("\n=== RECORD PAYMENT ===");

        List<OrderSummary> unpaid = orderDao.getOrders(null, null, null);
        unpaid.removeIf(o -> "CANCELLED".equals(o.status()) || o.balance() <= 0.009);
        if (unpaid.isEmpty()) {
            System.out.println(" -> No orders with unpaid balance.");
            return;
        }

        OrderUI.printOrderTable(unpaid);
        OrderSummary order = null;
        while (order == null) {
            int id = in.readInt("Order ID to pay (0 to cancel): ");
            if (id == 0) {
                System.out.println(" -> Operation Cancelled.");
                return;
            }
            for (OrderSummary o : unpaid) {
                if (o.order_id() == id) order = o;
            }
            if (order == null) System.out.println(" -> [ERROR] Order #" + id + " is not in the unpaid list.");
        }

        System.out.println("Customer: " + order.customer_name());
        System.out.println("Total   : " + InputHelper.money(order.total_amount()));
        System.out.println("Paid    : " + InputHelper.money(order.paid()));
        System.out.println("Balance : " + InputHelper.money(order.balance()));

        double amount;
        while (true) {
            amount = in.readPositiveDouble("Amount received (cash): ");
            if (amount <= order.balance() + 0.009) break;
            System.out.println(" -> [ERROR] Amount exceeds the balance (" + InputHelper.money(order.balance()) + ").");
        }

        if (!in.confirm("Save payment of " + InputHelper.money(amount) + " for Order #" + order.order_id() + "?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        Payment p = paymentDao.recordPayment(actor.user_id(), order.order_id(), amount, "CASH");
        if (p != null) {
            double balanceAfter = Math.max(0, order.balance() - amount);
            printReceipt(p, balanceAfter);
        }
    }

    private void printReceipt(Payment p, double balanceAfter) {
        System.out.println("\n==========================================");
        System.out.println("              OFFICIAL RECEIPT");
        System.out.println("==========================================");
        System.out.println("Receipt No : " + p.receipt_number());
        System.out.println("Date       : " + p.payment_date());
        System.out.println("Order No   : #" + p.order_id());
        System.out.println("Customer   : " + p.customer_name());
        System.out.println("Received by: " + p.received_by());
        System.out.println("Method     : " + p.payment_method());
        System.out.println("Amount paid: " + InputHelper.money(p.amount_paid()));
        System.out.println("Balance    : " + InputHelper.money(balanceAfter)
                + (balanceAfter <= 0.009 ? "  (FULLY PAID)" : "  (PARTIAL PAYMENT)"));
        System.out.println("==========================================");
    }

    public void viewAllPayments() {
        System.out.println("\n=== ALL PAYMENTS ===");
        printPaymentTable(paymentDao.getPayments(null, null, null));
    }

    private void viewByOrder() {
        int id = in.readInt("Order ID (0 to cancel): ");
        if (id == 0) return;
        System.out.println("\n=== PAYMENTS FOR ORDER #" + id + " ===");
        printPaymentTable(paymentDao.getPayments(null, id, null));
    }

    // CUSTOMER
    public void paymentHistory(User customer) {
        System.out.println("\n=== MY PAYMENT HISTORY ===");
        printPaymentTable(paymentDao.getPayments(customer.user_id(), null, null));
    }

    public static void printPaymentTable(List<Payment> list) {
        String border = InputHelper.border(5, 20, 6, 18, 12, 6, 19, 16);
        String fmt = "| %-5s | %-20s | %-6s | %-18s | %-12s | %-6s | %-19s | %-16s |%n";

        System.out.println(border);
        System.out.printf(fmt, "ID", "RECEIPT NO", "ORDER", "CUSTOMER", "AMOUNT", "METHOD", "DATE", "RECEIVED BY");
        System.out.println(border);

        if (list.isEmpty()) {
            InputHelper.emptyRow(border);
        } else {
            for (Payment p : list) {
                System.out.printf(fmt,
                        p.payment_id(),
                        p.receipt_number(),
                        "#" + p.order_id(),
                        InputHelper.truncate(p.customer_name(), 18),
                        InputHelper.money(p.amount_paid()),
                        p.payment_method(),
                        p.payment_date(),
                        InputHelper.truncate(p.received_by(), 16));
            }
        }
        System.out.println(border);
        System.out.println("Total: " + list.size());
    }
}