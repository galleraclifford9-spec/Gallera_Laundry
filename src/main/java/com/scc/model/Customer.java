package com.scc.model;

public record Customer(int user_id, String full_name, String username,
                       String contact_number, String address, String status) {}