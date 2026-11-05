package com.scc.model;

public record OrderLine(int service_id, String service_name, String pricing_type,
                        double quantity, double unit_price) {
    public double subtotal() {
        return quantity * unit_price;
    }
}