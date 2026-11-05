package com.scc.model;

public record Service(int service_id, String service_name, String description,
                      double price, String pricing_type, boolean is_available) {
    public String unit() {
        return "PER_KG".equals(pricing_type) ? "kg" : "pc";
    }
}