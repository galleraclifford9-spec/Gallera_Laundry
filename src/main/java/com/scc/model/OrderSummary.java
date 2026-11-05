package com.scc.model;

public record OrderSummary(int order_id, int customer_id, String customer_name, String staff_name,
                           double weight_kg, double total_amount, double paid, String status,
                           String order_date, String pickup_date, String remarks) {
    public double balance() {
        return Math.max(0, total_amount - paid);
    }
}