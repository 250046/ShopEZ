package com.shopez.model;

import com.shopez.util.PasswordUtil;

import java.time.LocalDateTime;

public abstract class User {

    protected int userId;
    protected String name;
    protected String email;
    protected String passwordHash;
    protected String role;
    protected LocalDateTime createdAt;

    public User() {
    }

    public User(int userId, String name, String email, String passwordHash, String role, LocalDateTime createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public abstract String getDashboardTitle();

    public boolean verifyPassword(String raw) {
        return PasswordUtil.verify(raw, this.passwordHash);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return role + "{userId=" + userId + ", name='" + name + "', email='" + email + "'}";
    }
}
