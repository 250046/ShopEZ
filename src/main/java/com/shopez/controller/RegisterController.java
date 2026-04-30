package com.shopez.controller;

import com.shopez.Main;
import com.shopez.dao.UserDAO;
import com.shopez.model.Customer;
import com.shopez.model.Seller;
import com.shopez.model.User;
import com.shopez.util.AlertUtil;
import com.shopez.util.PasswordUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private javafx.scene.layout.VBox storeNameBox;
    @FXML private TextField storeNameField;
    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("Customer", "Seller"));
        roleCombo.setValue("Customer");

        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSeller = "Seller".equals(newVal);
            storeNameBox.setVisible(isSeller);
            storeNameBox.setManaged(isSeller);
        });
    }

    @FXML
    private void handleRegister() {
        clearErrors();

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleCombo.getValue();

        boolean valid = true;

        if (name.isEmpty()) {
            showFieldError(nameError, "Name is required");
            valid = false;
        }
        if (email.isEmpty()) {
            showFieldError(emailError, "Email is required");
            valid = false;
        } else if (!email.contains("@")) {
            showFieldError(emailError, "Invalid email format");
            valid = false;
        }
        if (password.isEmpty()) {
            showFieldError(passwordError, "Password is required");
            valid = false;
        } else if (password.length() < 6) {
            showFieldError(passwordError, "Password must be at least 6 characters");
            valid = false;
        }
        if (!password.equals(confirmPassword)) {
            showFieldError(confirmError, "Passwords do not match");
            valid = false;
        }
        if ("Seller".equals(role) && storeNameField.getText().trim().isEmpty()) {
            AlertUtil.showError("Validation Error", "Store name is required for seller accounts.");
            valid = false;
        }

        if (!valid) return;

        User newUser;
        if ("Seller".equals(role)) {
            Seller seller = new Seller();
            seller.setName(name);
            seller.setEmail(email);
            seller.setPasswordHash(PasswordUtil.hash(password));
            seller.setRole("SELLER");
            seller.setStoreName(storeNameField.getText().trim());
            seller.setApproved(false);
            newUser = seller;
        } else {
            Customer customer = new Customer();
            customer.setName(name);
            customer.setEmail(email);
            customer.setPasswordHash(PasswordUtil.hash(password));
            customer.setRole("CUSTOMER");
            customer.setWalletBalance(0);
            newUser = customer;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Check if email already taken
                if (userDAO.findByEmail(email).isPresent()) {
                    throw new RuntimeException("EMAIL_TAKEN");
                }
                userDAO.save(newUser);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            String msg = "Seller".equals(role)
                    ? "Registration successful! Your seller account is pending admin approval."
                    : "Registration successful! You can now log in.";
            AlertUtil.showInfo("Success", msg);
            Main.switchScene("fxml/Login.fxml");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null && "EMAIL_TAKEN".equals(ex.getMessage())) {
                showFieldError(emailError, "This email is already registered");
            } else {
                AlertUtil.showError("Registration Error", "Failed to create account. Please try again.");
            }
        });

        new Thread(task).start();
    }

    @FXML
    private void handleLoginLink() {
        Main.switchScene("fxml/Login.fxml");
    }

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearErrors() {
        for (Label l : new Label[]{nameError, emailError, passwordError, confirmError}) {
            l.setVisible(false);
            l.setManaged(false);
        }
    }
}
