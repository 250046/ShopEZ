package com.shopez.controller;

import com.shopez.Main;
import com.shopez.dao.UserDAO;
import com.shopez.model.Customer;
import com.shopez.model.Seller;
import com.shopez.model.User;
import com.shopez.util.AlertUtil;
import com.shopez.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label emailError;
    @FXML private Label passwordError;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        clearErrors();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        boolean valid = true;
        if (email.isEmpty()) {
            showFieldError(emailError, "Email is required");
            valid = false;
        }
        if (password.isEmpty()) {
            showFieldError(passwordError, "Password is required");
            valid = false;
        }
        if (!valid) return;

        Task<Optional<User>> task = new Task<>() {
            @Override
            protected Optional<User> call() throws Exception {
                return userDAO.findByEmail(email);
            }
        };

        task.setOnSucceeded(e -> {
            Optional<User> result = task.getValue();
            if (result.isEmpty()) {
                AlertUtil.showError("Login Failed", "No account found with that email.");
                return;
            }

            User user = result.get();

            // Check banned status
            try {
                if (userDAO.isBanned(user.getUserId())) {
                    AlertUtil.showError("Account Suspended", "Your account has been suspended.");
                    return;
                }
            } catch (Exception ex) {
                AlertUtil.showError("Error", "Failed to check account status.");
                return;
            }

            // Check password
            if (!user.verifyPassword(password)) {
                AlertUtil.showError("Login Failed", "Incorrect password.");
                return;
            }

            // Check seller approval
            if (user instanceof Seller && !((Seller) user).isApproved()) {
                AlertUtil.showError("Pending Approval", "Your seller account is pending approval.");
                return;
            }

            SessionManager.login(user);

            // Route to correct dashboard
            if (user instanceof Customer) {
                Main.switchScene("fxml/CustomerDashboard.fxml");
            } else if (user instanceof Seller) {
                Main.switchScene("fxml/SellerDashboard.fxml");
            } else {
                Main.switchScene("fxml/AdminDashboard.fxml");
            }
        });

        task.setOnFailed(e -> {
            AlertUtil.showError("Connection Error", "Failed to connect to database. Please check that MySQL is running.");
        });

        new Thread(task).start();
    }

    @FXML
    private void handleRegisterLink() {
        Main.switchScene("fxml/Register.fxml");
    }

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearErrors() {
        emailError.setVisible(false);
        emailError.setManaged(false);
        passwordError.setVisible(false);
        passwordError.setManaged(false);
    }
}
