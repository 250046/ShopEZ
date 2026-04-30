package com.shopez.util;

import com.shopez.model.Admin;
import com.shopez.model.Customer;
import com.shopez.model.Seller;
import com.shopez.model.User;

public class SessionManager {

    private static User currentUser;

    private SessionManager() {
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isCustomer() {
        return currentUser instanceof Customer;
    }

    public static boolean isSeller() {
        return currentUser instanceof Seller;
    }

    public static boolean isAdmin() {
        return currentUser instanceof Admin;
    }
}
