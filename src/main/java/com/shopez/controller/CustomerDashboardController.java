package com.shopez.controller;

import com.shopez.Main;
import com.shopez.dao.CartDAO;
import com.shopez.dao.ProductDAO;
import com.shopez.dao.UserDAO;
import com.shopez.model.Cart;
import com.shopez.model.Customer;
import com.shopez.util.AlertUtil;
import com.shopez.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Optional;

public class CustomerDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button cartBtn;
    @FXML private Label walletLabel;
    @FXML private Label notifLabel;
    @FXML private Button browseBtn;
    @FXML private Button ordersBtn;
    @FXML private Button topUpBtn;

    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void initialize() {
        refreshWallet();
        refreshCartBadge();
        refreshNotifications();
        handleBrowse();
    }

    @FXML
    private void handleBrowse() {
        loadContent("fxml/ProductList.fxml");
    }

    @FXML
    private void handleCart() {
        loadContent("fxml/Cart.fxml");
    }

    @FXML
    private void handleOrders() {
        loadContent("fxml/OrderHistory.fxml");
    }

    @FXML
    private void handleTopUp() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Top Up Wallet");
        dialog.setHeaderText("Enter amount to add to your wallet");
        dialog.setContentText("Amount ($):");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    AlertUtil.showError("Invalid Amount", "Please enter a positive amount.");
                    return;
                }
                Customer customer = (Customer) SessionManager.getCurrentUser();
                customer.topUpWallet(amount);

                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        userDAO.updateWalletBalance(customer.getUserId(), customer.getWalletBalance());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> {
                    refreshWallet();
                    AlertUtil.showInfo("Success", "Wallet topped up by $" + String.format("%.2f", amount));
                });
                task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to top up wallet."));
                new Thread(task).start();
            } catch (NumberFormatException e) {
                AlertUtil.showError("Invalid Input", "Please enter a valid number.");
            }
        });
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        Main.switchScene("fxml/Login.fxml");
    }

    public void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/shopez/" + fxmlPath));
            Parent content = loader.load();

            // Pass reference to dashboard controller
            Object controller = loader.getController();
            if (controller instanceof ProductListController) {
                ((ProductListController) controller).setDashboardController(this);
            } else if (controller instanceof ProductDetailController) {
                ((ProductDetailController) controller).setDashboardController(this);
            } else if (controller instanceof CartController) {
                ((CartController) controller).setDashboardController(this);
            } else if (controller instanceof CheckoutController) {
                ((CheckoutController) controller).setDashboardController(this);
            } else if (controller instanceof OrderHistoryController) {
                ((OrderHistoryController) controller).setDashboardController(this);
            }

            contentArea.getChildren().setAll(content);
        } catch (IOException e) {
            AlertUtil.showError("Navigation Error", "Failed to load screen: " + fxmlPath);
        }
    }

    public void loadProductDetail(int productId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/shopez/fxml/ProductDetail.fxml"));
            Parent content = loader.load();
            ProductDetailController controller = loader.getController();
            controller.setDashboardController(this);
            controller.loadProduct(productId);
            contentArea.getChildren().setAll(content);
        } catch (IOException e) {
            AlertUtil.showError("Navigation Error", "Failed to load product detail.");
        }
    }

    public void refreshCartBadge() {
        Customer customer = (Customer) SessionManager.getCurrentUser();
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                Cart cart = cartDAO.findByCustomer(customer.getUserId());
                return cart.getTotalItemCount();
            }
        };
        task.setOnSucceeded(e -> cartBtn.setText("Cart (" + task.getValue() + ")"));
        new Thread(task).start();
    }

    public void refreshWallet() {
        Customer customer = (Customer) SessionManager.getCurrentUser();
        walletLabel.setText("Wallet: $" + String.format("%.2f", customer.getWalletBalance()));
    }

    public void refreshNotifications() {
        Customer customer = (Customer) SessionManager.getCurrentUser();
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return productDAO.getUnreadNotificationCount(customer.getUserId());
            }
        };
        task.setOnSucceeded(e -> {
            int count = task.getValue();
            if (count > 0) {
                notifLabel.setText("Notifications: " + count);
                notifLabel.setVisible(true);
            } else {
                notifLabel.setText("");
                notifLabel.setVisible(false);
            }
        });
        new Thread(task).start();
    }
}
