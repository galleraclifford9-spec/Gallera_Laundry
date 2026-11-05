package com.scc.ui;

import com.scc.dao.ServiceDao;
import com.scc.model.Service;
import com.scc.model.User;

import java.util.List;

public class ServiceUI {

    private final ServiceDao serviceDao = new ServiceDao();
    private final InputHelper in;

    public ServiceUI(InputHelper in) {
        this.in = in;
    }

    // ADMIN: Service Management
    public void menu(User admin) {
        while (true) {
            System.out.println("\n------------- SERVICE MANAGEMENT -------------");
            System.out.println("[1] View All Services");
            System.out.println("[2] Add New Service");
            System.out.println("[3] Update Service Price");
            System.out.println("[4] Activate / Deactivate Service");
            System.out.println("[0] Back");
            System.out.println("----------------------------------------------");
            String choice = in.line("Enter choice: ");

            switch (choice) {
                case "1": viewServices(false); break;
                case "2": addService(admin); break;
                case "3": updatePrice(admin); break;
                case "4": toggleAvailability(admin); break;
                case "0": return;
                default: System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    public void viewServices(boolean onlyAvailable) {
        List<Service> list = serviceDao.getAll(onlyAvailable);
        System.out.println(onlyAvailable ? "\n=== SERVICE PRICE LIST ===" : "\n=== ALL SERVICES ===");
        printTable(list);
    }

    private void addService(User admin) {
        System.out.println("\n=== ADD NEW SERVICE ===");
        String name = in.line("Service name (0 to cancel): ");
        if (name.equals("0") || name.isEmpty()) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        String desc = in.line("Description (optional): ");
        double price = in.readPositiveDouble("Price: ");
        String type = readPricingType();
        if (type == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }

        if (serviceDao.addService(admin.user_id(), name, desc, price, type)) {
            System.out.println(" -> Service \"" + name + "\" added.");
        }
    }

    private String readPricingType() {
        while (true) {
            String c = in.line("Pricing type [1] PER KG | [2] PER PIECE | [0] Back: ");
            switch (c) {
                case "1": return "PER_KG";
                case "2": return "PER_PIECE";
                case "0": return null;
                default: System.out.println(" -> [ERROR] Invalid, enter 0-2.");
            }
        }
    }

    private Service pickService(List<Service> list, String prompt) {
        while (true) {
            int id = in.readInt(prompt);
            if (id == 0) return null;
            for (Service s : list) {
                if (s.service_id() == id) return s;
            }
            System.out.println(" -> [ERROR] Service ID " + id + " not found.");
        }
    }

    private void updatePrice(User admin) {
        List<Service> list = serviceDao.getAll(false);
        System.out.println("\n=== UPDATE SERVICE PRICE ===");
        printTable(list);
        if (list.isEmpty()) return;

        Service s = pickService(list, "Service ID to update (0 to cancel): ");
        if (s == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        double newPrice = in.readPositiveDouble("New price for " + s.service_name()
                + " (current " + InputHelper.money(s.price()) + "): ");
        if (serviceDao.updatePrice(admin.user_id(), s, newPrice)) {
            System.out.println(" -> Price updated.");
        }
    }

    private void toggleAvailability(User admin) {
        List<Service> list = serviceDao.getAll(false);
        System.out.println("\n=== ACTIVATE / DEACTIVATE SERVICE ===");
        printTable(list);
        if (list.isEmpty()) return;

        Service s = pickService(list, "Service ID (0 to cancel): ");
        if (s == null) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        boolean makeAvailable = !s.is_available();
        String action = makeAvailable ? "ACTIVATE" : "DEACTIVATE";
        if (!in.confirm(action + " " + s.service_name() + "?")) {
            System.out.println(" -> Operation Cancelled.");
            return;
        }
        if (serviceDao.setAvailability(admin.user_id(), s, makeAvailable)) {
            System.out.println(" -> " + s.service_name() + " is now " + (makeAvailable ? "AVAILABLE" : "INACTIVE") + ".");
        }
    }

    public static void printTable(List<Service> list) {
        String border = InputHelper.border(4, 22, 12, 9, 9);
        String fmt = "| %-4s | %-22s | %-12s | %-9s | %-9s |%n";

        System.out.println(border);
        System.out.printf(fmt, "ID", "SERVICE", "PRICE", "TYPE", "STATUS");
        System.out.println(border);

        if (list.isEmpty()) {
            InputHelper.emptyRow(border);
        } else {
            for (Service s : list) {
                System.out.printf(fmt,
                        s.service_id(),
                        InputHelper.truncate(s.service_name(), 22),
                        InputHelper.money(s.price()) + "/" + s.unit(),
                        "PER_KG".equals(s.pricing_type()) ? "per kg" : "per piece",
                        s.is_available() ? "Available" : "Inactive");
            }
        }
        System.out.println(border);
        System.out.println("Total: " + list.size());
    }
}