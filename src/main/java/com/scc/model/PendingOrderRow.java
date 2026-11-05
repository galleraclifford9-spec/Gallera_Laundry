package com.scc.model;

public record PendingOrderRow(int order_id, String customer_name, String contact,
                              String status, String pickup_date,
                              double total_amount, double balance) {}