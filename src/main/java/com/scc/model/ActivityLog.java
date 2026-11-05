package com.scc.model;

public record ActivityLog(int id, int user_id, String username,
                          String action_type, String description, String timestamp) {}