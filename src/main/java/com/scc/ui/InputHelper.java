package com.scc.ui;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class InputHelper {

    private final Scanner sc;

    public InputHelper(Scanner sc) {
        this.sc = sc;
    }

    public String line(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    public String readString(String prompt) {
        while (true) {
            String s = line(prompt);
            if (!s.isEmpty()) return s;
            System.out.println(" -> [ERROR] It must not be empty.");
        }
    }

    public int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(line(prompt));
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Enter a valid number.");
            }
        }
    }

    public double readPositiveDouble(String prompt) {
        while (true) {
            try {
                double v = Double.parseDouble(line(prompt));
                if (v > 0) return v;
                System.out.println(" -> [ERROR] Must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println(" -> [ERROR] Enter a valid number.");
            }
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String s = line(prompt + " (Y/N): ");
            if (s.equalsIgnoreCase("Y")) return true;
            if (s.equalsIgnoreCase("N")) return false;
            System.out.println(" -> [ERROR] Enter Y or N.");
        }
    }

    public LocalDate readDate(String prompt, boolean notBeforeToday) {
        while (true) {
            String s = line(prompt);
            try {
                LocalDate d = LocalDate.parse(s);
                if (notBeforeToday && d.isBefore(LocalDate.now())) {
                    System.out.println(" -> [ERROR] Date cannot be in the past.");
                    continue;
                }
                return d;
            } catch (DateTimeParseException e) {
                System.out.println(" -> [ERROR] Use format YYYY-MM-DD (example: 2026-11-10).");
            }
        }
    }

    // ---------------- STATIC HELPERS ----------------

    public static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    public static String money(double v) {
        return String.format("P%,.2f", v);
    }

    public static String border(int... widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        return sb.toString();
    }

    public static void emptyRow(String border) {
        System.out.printf("| %-" + (border.length() - 4) + "s |%n", "No records found.");
    }

    public static String dateOnly(String dt) {
        if (dt == null || dt.isBlank()) return "-";
        return dt.length() >= 10 ? dt.substring(0, 10) : dt;
    }
}