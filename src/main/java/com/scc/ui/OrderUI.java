package com.scc.ui;

import com.scc.dao.CustomerDao;
import com.scc.dao.OrderDao;
import com.scc.dao.ServiceDao;
import com.scc.model.Customer;
import com.scc.model.OrderLine;
import com.scc.model.OrderSummary;
import com.scc.model.Service;
import com.scc.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderUI {

    private final OrderDao orderDao = new OrderDao();
    private final ServiceDao serviceDao = new ServiceDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final InputHelper in;

    public OrderUI(InputHelper in) {
        this.in = in;
    }

    public void adminMenu(User admin) {
        while (true) {
            System.out.println("\n------------- ORDER MANAGEMENT -------------");
            System.out.println("[1] View / Filter Orders");
            System.out.println("[2] Create Order");
            System.out.println("[3] Update Order Status");
            System.out.println("[4] Cancel Order");
            System.out.println("[0] Back");
            System.out.println("--------------------------------------------");
            String choice = in.line("Enter choice: ");

            switch (choice) {
                case "1": viewOrders(); break;
                case "2": createOrder(admin); break;
                case "3": updateOrderStatus(admin); break;
                case "4": cancelOrder(admin); break;
                case "0": return;
                default: System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    public void createOrder(User actor) {
        System.out.println("\n=== CREATE ORDER ===");

        List<Customer> customers = customerDao.search("");
        customers.removeIf(c -> !"APPROVED".equalsIgnoreCase(c.status()));
        if (customers.isEmpty()) {
            System.out.println(" -> No approved customers. Register a customer first.");
            return;
        }
        List<Service> services = serviceDao.getAll(true);
        if (services.isEmpty()) {
            System.out.println(" -> No available services. Ask the admin to add one.");
            return;
        }

        CustomerUI.printCustomerTable(customers);
        Customer customer = null;
        while (customer == null) {
            int id = in.readInt("Customer User ID (0 to cancel): ");
            if (id == 0) {
                System.out.println(" -> Operation Cancelled.");
                return;
            }
            for (Customer c : customers) {
                if (c.user_id() == id) customer = c;
            }
            if (customer == null) System.out.println(" -> [ERROR] Customer ID " + id + " not found.");
        }

        List<OrderLine> lines = new ArrayList<>();
        while (true) {
            System.out.println();
            ServiceUI.printTable(services);
            int sid = in.readInt("Service ID to add (0 to " + (lines.isEmpty() ? "cancel" : "finish") + "): ");
            if (sid == 0) {
                if (lines.isEmpty()) {
                    System.out.println(" -> Operation Cancelled.");
                    return;
                }
                break;
            }
            Service s = null;
            for (Service x : services) {
                if (x.service_id() == sid) s = x;
            }
            if (s == null) {
                System.out.println(" -> [ERROR] Service ID " + sid + " not found.");
                continue;
            }

            boolean perKg = "PER_KG".equals(s.pricing_type());
            double qty = in.readPositiveDouble(perKg ? "Weight (kg): " : "Quantity (pieces): ");
            if (!perKg && qty != Math.floor(qty)) {
                System.out.println(" -> [ERROR] Pieces must be a whole number.");
                continue;
            }

            lines.add(new OrderLine(s.service_id(), s.service_name(), s.pricing_type(), qty, s.price()));
            double running = 0;
            for (OrderLine l : lines) running += l.subtotal();
            System.out.println(" -> Added. Running total: " + InputHelper.money(running));

            if (!in.confirm("Add another service?")) break;
        }

        LocalDate pickup = in.readDate("Pickup date (YYYY-MM-DD): ", true);
        String remarks = in.line("Remarks (optional): ");

        double total = 0;
        System.out.println("\n--- ORDER SUMMARY ---");
        System.out.println("Customer: " + customer.full_name());
        for (OrderLine l : lines) {
            System.out.printf("  %-22s %8.2f x %-10s = %s%n",
                    InputHelper.truncate(l.service_name(), 22), l.quantity(),
                    InputHelper.money(l.unit_price()), InputHelper.money(l.subtotal()));
            total += l.subtotal();
        }
        System.out.println("Pickup date: " + pickup);
        System.out.println("TOTAL: " + InputHelper.money(total));

        if (!in.confirm("Create this order?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        int orderId = orderDao.createOrder(actor.user_id(), customer.user_id(), lines, pickup.toString(), remarks);
        if (orderId > 0) {
            System.out.printf(" -> Order #%d created. Total: %s%n", orderId, InputHelper.money(total));
        }
    }

    public void updateOrderStatus(User actor) {
        System.out.println("\n=== UPDATE ORDER STATUS ===");
        int orderId = in.readInt("Order ID (0 to cancel): ");
        if (orderId == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        OrderSummary o = orderDao.getOrderById(orderId);
        if (o == null) {
            System.out.println(" -> Order not found.");
            return;
        }
        System.out.println("Order #" + o.order_id() + " | " + o.customer_name() + " | Current status: " + o.status());
        if ("CANCELLED".equals(o.status())) {
            System.out.println(" -> Order is CANCELLED and can no longer be updated.");
            return;
        }

        String[] flow = OrderDao.FLOW;
        for (int i = 0; i < flow.length; i++) {
            System.out.println("[" + (i + 1) + "] " + flow[i]);
        }

        int pick = in.readInt("Choose new status (0 to cancel): ");
        if (pick == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        if (pick < 1 || pick > flow.length) {
            System.out.println(" -> [ERROR] Invalid, enter 1-" + flow.length + ".");
            return;
        }

        String newStatus = flow[pick - 1];
        if (newStatus.equals(o.status())) {
            System.out.println(" -> Order is already " + newStatus + ".");
            return;
        }

        if (orderDao.updateOrderStatus(actor.user_id(), orderId, newStatus)) {
            System.out.println(" -> Order #" + orderId + " updated to " + newStatus);
        }
    }

    public void cancelOrder(User actor) {
        System.out.println("\n=== CANCEL ORDER ===");
        int orderId = in.readInt("Order ID to cancel (0 to go back): ");
        if (orderId == 0) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        if (!in.confirm("Are you sure you want to cancel Order #" + orderId + "?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        if (orderDao.cancelOrder(actor.user_id(), orderId)) {
            System.out.println(" -> Order #" + orderId + " has been CANCELLED.");
        }
    }

    public void viewOrders() {
        while (true) {
            System.out.println("\n------------- VIEW / FILTER ORDERS -------------");
            System.out.println("[1] All Orders");
            System.out.println("[2] Filter by Status");
            System.out.println("[3] Filter by Date");
            System.out.println("[4] Filter by Customer");
            System.out.println("[5] View Order Details");
            System.out.println("[0] Back");
            System.out.println("------------------------------------------------");
            String choice = in.line("Enter choice: ");

            switch (choice) {
                case "1":
                    printOrderTable(orderDao.getOrders(null, null, null));
                    break;
                case "2":
                    String status = pickStatus();
                    if (status != null) printOrderTable(orderDao.getOrders(status, null, null));
                    break;
                case "3":
                    LocalDate d = in.readDate("Order date (YYYY-MM-DD): ", false);
                    printOrderTable(orderDao.getOrders(null, null, d.toString()));
                    break;
                case "4":
                    int cid = in.readInt("Customer User ID (0 to cancel): ");
                    if (cid != 0) printOrderTable(orderDao.getOrders(null, cid, null));
                    break;
                case "5":
                    int oid = in.readInt("Order ID (0 to cancel): ");
                    if (oid != 0) showDetails(oid, null);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    private String pickStatus() {
        String[] all = OrderDao.ALL_STATUS;
        for (int i = 0; i < all.length; i++) {
            System.out.println("[" + (i + 1) + "] " + all[i]);
        }
        while (true) {
            int pick = in.readInt("Choose status (0 to cancel): ");
            if (pick == 0) return null;
            if (pick >= 1 && pick <= all.length) return all[pick - 1];
            System.out.println(" -> [ERROR] Invalid, enter 0-" + all.length + ".");
        }
    }

    public void viewMyOrders(User customer) {
        System.out.println("\n=== MY ORDERS ===");
        List<OrderSummary> orders = orderDao.getOrders(null, customer.user_id(), null);
        printOrderTable(orders);
        if (orders.isEmpty()) return;

        int id = in.readInt("Enter Order ID for details (0 to go back): ");
        if (id != 0) showDetails(id, customer.user_id());
    }

    public void trackOrder(User customer) {
        System.out.println("\n=== TRACK ORDER STATUS ===");
        int id = in.readInt("Enter Order ID (0 to go back): ");
        if (id != 0) showDetails(id, customer.user_id());
    }

    public void showDetails(int orderId, Integer onlyCustomerId) {
        OrderSummary o = orderDao.getOrderById(orderId);
        if (o == null || (onlyCustomerId != null && o.customer_id() != onlyCustomerId)) {
            System.out.println(" -> Order not found.");
            return;
        }

        System.out.println("\n================ ORDER #" + o.order_id() + " ================");
        System.out.println("Customer    : " + o.customer_name());
        System.out.println("Received by : " + o.staff_name());
        System.out.println("Order date  : " + o.order_date());
        System.out.println("Pickup date : " + (o.pickup_date() == null ? "-" : o.pickup_date()));
        System.out.println("Remarks     : " + (o.remarks() == null || o.remarks().isBlank() ? "-" : o.remarks()));
        System.out.println("Progress    : " + progressLine(o.status()));

        List<OrderLine> items = orderDao.getItems(orderId);
        String border = InputHelper.border(22, 10, 12, 12);
        String fmt = "| %-22s | %-10s | %-12s | %-12s |%n";
        System.out.println(border);
        System.out.printf(fmt, "SERVICE", "QTY", "UNIT PRICE", "SUBTOTAL");
        System.out.println(border);
        if (items.isEmpty()) {
            InputHelper.emptyRow(border);
        } else {
            for (OrderLine l : items) {
                String unit = "PER_KG".equals(l.pricing_type()) ? " kg" : " pc";
                System.out.printf(fmt,
                        InputHelper.truncate(l.service_name(), 22),
                        String.format("%.2f", l.quantity()) + unit,
                        InputHelper.money(l.unit_price()),
                        InputHelper.money(l.subtotal()));
            }
        }
        System.out.println(border);

        System.out.println("Total   : " + InputHelper.money(o.total_amount()));
    }

    private String progressLine(String status) {
        if ("CANCELLED".equals(status)) return "[CANCELLED]";
        StringBuilder sb = new StringBuilder();
        for (String s : OrderDao.FLOW) {
            if (sb.length() > 0) sb.append(" > ");
            sb.append(s.equals(status) ? "[" + s + "]" : s);
        }
        return sb.toString();
    }

    public static void printOrderTable(List<OrderSummary> list) {
        String border = InputHelper.border(5, 20, 11, 10, 10, 10);
        String fmt = "| %-5s | %-20s | %-11s | %-10s | %-10s | %-10s |%n";

        System.out.println(border);
        System.out.printf(fmt, "ID", "CUSTOMER", "TOTAL", "STATUS", "ORDERED", "PICKUP");
        System.out.println(border);

        if (list.isEmpty()) {
            InputHelper.emptyRow(border);
        } else {
            for (OrderSummary o : list) {
                System.out.printf(fmt,
                        o.order_id(),
                        InputHelper.truncate(o.customer_name(), 20),
                        InputHelper.money(o.total_amount()),
                        o.status(),
                        InputHelper.dateOnly(o.order_date()),
                        InputHelper.dateOnly(o.pickup_date()));
            }
        }
        System.out.println(border);
        System.out.println("Total: " + list.size());
    }
}