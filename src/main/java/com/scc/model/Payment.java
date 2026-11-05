package com.scc.model;

public record Payment(int payment_id, int order_id, String customer_name, String received_by,
                      double amount_paid, String payment_method, String payment_date,
                      String receipt_number) {

}