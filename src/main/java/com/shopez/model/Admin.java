package com.shopez.model;

import java.time.LocalDateTime;

public class Admin extends User {

    public Admin() {
        this.role = "ADMIN";
    }

    public Admin(int userId, String name, String email, String passwordHash, LocalDateTime createdAt) {
        super(userId, name, email, passwordHash, "ADMIN", createdAt);
    }

    @Override
    public String getDashboardTitle() {
        return "Admin Dashboard";
    }
}
