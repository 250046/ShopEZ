package com.shopez.controller;

import com.shopez.Main;
import com.shopez.dao.OrderDAO;
import com.shopez.dao.ProductDAO;
import com.shopez.model.Product;
import com.shopez.model.Seller;
import com.shopez.util.AlertUtil;
import com.shopez.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class SellerDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label totalProductsLabel;
    @FXML private Label pendingOrdersLabel;
    @FXML private Label totalRevenueLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    @FXML
    private void initialize() {
        loadStats();
    }

    private void loadStats() {
        Seller seller = (Seller) SessionManager.getCurrentUser();
        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() throws Exception {
                int products = productDAO.findBySeller(seller.getUserId()).size();
                int pendingOrders = orderDAO.getSellerPendingOrderCount(seller.getUserId());
                return new int[]{products, pendingOrders};
            }
        };
        task.setOnSucceeded(e -> {
            int[] stats = task.getValue();
            totalProductsLabel.setText(String.valueOf(stats[0]));
            pendingOrdersLabel.setText(String.valueOf(stats[1]));
        });
        new Thread(task).start();

        Task<Double> revenueTask = new Task<>() {
            @Override
            protected Double call() throws Exception {
                return orderDAO.getSellerRevenue(seller.getUserId());
            }
        };
        revenueTask.setOnSucceeded(e -> totalRevenueLabel.setText(String.format("$%.2f", revenueTask.getValue())));
        new Thread(revenueTask).start();
    }

    @FXML
    private void handleProducts() {
        loadContent("fxml/SellerProducts.fxml");
    }

    @FXML
    private void handleOrders() {
        loadContent("fxml/SellerOrders.fxml");
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

            Object controller = loader.getController();
            if (controller instanceof SellerProductsController) {
                ((SellerProductsController) controller).setDashboardController(this);
            } else if (controller instanceof AddEditProductController) {
                ((AddEditProductController) controller).setDashboardController(this);
            } else if (controller instanceof SellerOrdersController) {
                ((SellerOrdersController) controller).setDashboardController(this);
            }

            contentArea.getChildren().setAll(content);
        } catch (IOException e) {
            AlertUtil.showError("Navigation Error", "Failed to load screen: " + fxmlPath);
        }
    }

    public void loadAddEditProduct(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/shopez/fxml/AddEditProduct.fxml"));
            Parent content = loader.load();
            AddEditProductController controller = loader.getController();
            controller.setDashboardController(this);
            if (product != null) {
                controller.setProduct(product);
            }
            contentArea.getChildren().setAll(content);
        } catch (IOException e) {
            AlertUtil.showError("Navigation Error", "Failed to load product form.");
        }
    }

    public void showDashboard() {
        contentArea.getChildren().clear();
        loadStats();
        // Reload the default dashboard content
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/shopez/fxml/SellerDashboard.fxml"));
            // We just refresh stats and show the existing view
        } catch (Exception ignored) {
        }
    }
}
