package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.PaymentDao;
import com.scc.dao.ReportDao;
import com.scc.model.PendingOrderRow;
import com.scc.model.Payment;
import com.scc.model.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ReportUI {

    private final ReportDao reportDao = new ReportDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final ActivityLogDao logDao = new ActivityLogDao();
    private final InputHelper in;

    public ReportUI(InputHelper in) {
        this.in = in;
    }

    // ADMIN: Reports
    public void menu(User admin) {
        while (true) {
            System.out.println("\n------------- REPORTS -------------");
            System.out.println("[1] Pending & Unclaimed Laundry Report");
            System.out.println("[2] Daily Sales Report");
            System.out.println("[3] Monthly Sales Report");
            System.out.println("[0] Back");
            System.out.println("-----------------------------------");
            String choice = in.line("Enter choice: ");

            switch (choice) {
                case "1": pendingUnclaimed(admin); break;
                case "2": dailySales(admin); break;
                case "3": monthlySales(admin); break;
                case "0": return;
                default: System.out.println("Invalid Choice, Try Again.");
            }
        }
    }

    // ---------------- PENDING & UNCLAIMED ----------------

    public void pendingUnclaimed(User actor) {
        System.out.println("\n=== PENDING & UNCLAIMED LAUNDRY REPORT ===");
        List<PendingOrderRow> rows = reportDao.getPendingUnclaimed();

        String border = InputHelper.border(5, 20, 24, 10, 10, 11, 11);
        String fmt = "| %-5s | %-20s | %-24s | %-10s | %-10s | %-11s | %-11s |%n";

        System.out.println(border);
        System.out.printf(fmt, "ORDER", "CUSTOMER", "CONTACT", "STATUS", "PICKUP", "TOTAL", "BALANCE");
        System.out.println(border);

        double totalBalance = 0;
        if (rows.isEmpty()) {
            InputHelper.emptyRow(border);
        } else {
            for (PendingOrderRow r : rows) {
                System.out.printf(fmt,
                        "#" + r.order_id(),
                        InputHelper.truncate(r.customer_name(), 20),
                        InputHelper.truncate(r.contact(), 24),
                        r.status(),
                        InputHelper.dateOnly(r.pickup_date()),
                        InputHelper.money(r.total_amount()),
                        InputHelper.money(r.balance()));
                totalBalance += r.balance();
            }
        }
        System.out.println(border);
        System.out.println("Total orders: " + rows.size() + " | Total unpaid balance: " + InputHelper.money(totalBalance));

        if (rows.isEmpty()) return;
        if (in.confirm("Export this report to CSV?")) {
            List<String[]> data = new ArrayList<>();
            for (PendingOrderRow r : rows) {
                data.add(new String[]{
                        String.valueOf(r.order_id()), r.customer_name(), r.contact(), r.status(),
                        r.pickup_date() == null ? "" : r.pickup_date(),
                        String.format("%.2f", r.total_amount()), String.format("%.2f", r.balance())});
            }
            export(actor, "pending_unclaimed_report",
                    new String[]{"Order", "Customer", "Contact", "Status", "Pickup Date", "Total", "Balance"}, data);
        }
    }

    // ---------------- SALES ----------------

    public void dailySales(User actor) {
        String s = in.line("Date (YYYY-MM-DD, blank = today): ");
        LocalDate date;
        if (s.isEmpty()) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println(" -> [ERROR] Use format YYYY-MM-DD.");
                return;
            }
        }
        showSales(actor, "DAILY SALES REPORT - " + date, date.toString(), "daily_sales_" + date);
    }

    public void monthlySales(User actor) {
        String s = in.line("Month (YYYY-MM, blank = this month): ");
        YearMonth ym;
        if (s.isEmpty()) {
            ym = YearMonth.now();
        } else {
            try {
                ym = YearMonth.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println(" -> [ERROR] Use format YYYY-MM.");
                return;
            }
        }
        showSales(actor, "MONTHLY SALES REPORT - " + ym, ym.toString(), "monthly_sales_" + ym);
    }

    private void showSales(User actor, String title, String prefix, String fileName) {
        System.out.println("\n=== " + title + " ===");
        List<Payment> payments = paymentDao.getPayments(null, null, prefix);
        PaymentUI.printPaymentTable(payments);

        double total = 0;
        for (Payment p : payments) total += p.amount_paid();
        System.out.println("Orders received  : " + reportDao.countOrders(prefix));
        System.out.println("Payments received: " + payments.size());
        System.out.println("TOTAL COLLECTED  : " + InputHelper.money(total));

        if (payments.isEmpty()) return;
        if (in.confirm("Export this report to CSV?")) {
            List<String[]> data = new ArrayList<>();
            for (Payment p : payments) {
                data.add(new String[]{
                        p.receipt_number(), String.valueOf(p.order_id()), p.customer_name(),
                        String.format("%.2f", p.amount_paid()), p.payment_method(),
                        p.payment_date(), p.received_by()});
            }
            export(actor, fileName,
                    new String[]{"Receipt No", "Order", "Customer", "Amount", "Method", "Date", "Received By"}, data);
        }
    }

    // ---------------- CSV EXPORT ----------------

    private void export(User actor, String baseName, String[] headers, List<String[]> rows) {
        String file = baseName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println(toCsvLine(headers));
            for (String[] r : rows) {
                pw.println(toCsvLine(r));
            }
        } catch (IOException e) {
            System.err.println(" -> Export failed: " + e.getMessage());
            return;
        }
        String path = new File(file).getAbsolutePath();
        System.out.println(" -> Exported to: " + path);
        logDao.log(actor.user_id(), "REPORT_EXPORTED", "User " + actor.user_id() + " exported " + file);
    }

    private String toCsvLine(String[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(",");
            String c = cols[i] == null ? "" : cols[i];
            sb.append("\"").append(c.replace("\"", "\"\"")).append("\"");
        }
        return sb.toString();
    }
}