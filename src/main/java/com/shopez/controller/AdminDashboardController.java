package com.shopez.controller;

import com.shopez.Main;
import com.shopez.dao.OrderDAO;
import com.shopez.dao.ProductDAO;
import com.shopez.dao.UserDAO;
import com.shopez.model.*;
import com.shopez.util.AlertUtil;
import com.shopez.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminDashboardController {

    // Stats
    @FXML private Label totalUsersLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalRevenueLabel;

    // Users tab
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userIdCol;
    @FXML private TableColumn<User, String> userNameCol;
    @FXML private TableColumn<User, String> userEmailCol;
    @FXML private TableColumn<User, String> userRoleCol;
    @FXML private TableColumn<User, String> userStatusCol;
    @FXML private TableColumn<User, Void> userActionCol;

    // Products tab
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> prodIdCol;
    @FXML private TableColumn<Product, String> prodNameCol;
    @FXML private TableColumn<Product, String> prodCategoryCol;
    @FXML private TableColumn<Product, String> prodPriceCol;
    @FXML private TableColumn<Product, String> prodStockCol;
    @FXML private TableColumn<Product, String> prodSellerCol;
    @FXML private TableColumn<Product, String> prodActiveCol;
    @FXML private TableColumn<Product, Void> prodActionCol;

    // Orders tab
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> orderIdCol;
    @FXML private TableColumn<Order, String> orderDateCol;
    @FXML private TableColumn<Order, String> orderCustomerCol;
    @FXML private TableColumn<Order, String> orderTotalCol;
    @FXML private TableColumn<Order, String> orderStatusCol;

    @FXML private TabPane tabPane;

    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final ObservableList<Order> orders = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupUsersTable();
        setupProductsTable();
        setupOrdersTable();

        loadStats();
        loadUsers();
        loadProducts();
        loadOrders();
    }

    private void setupUsersTable() {
        userIdCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getUserId())));
        userNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        userEmailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        userRoleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        userStatusCol.setCellValueFactory(data -> {
            User u = data.getValue();
            if (u instanceof Seller && !((Seller) u).isApproved()) {
                return new SimpleStringProperty("Pending Approval");
            }
            try {
                if (userDAO.isBanned(u.getUserId())) {
                    return new SimpleStringProperty("Banned");
                }
            } catch (Exception ignored) {
            }
            return new SimpleStringProperty("Active");
        });

        userActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button banBtn = new Button("Ban");
            private final Button approveBtn = new Button("Approve");
            private final HBox box = new HBox(5, approveBtn, banBtn);

            {
                banBtn.getStyleClass().add("danger-button");
                approveBtn.getStyleClass().add("primary-button");

                banBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    banUser(user);
                });

                approveBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    approveSeller(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    if (user instanceof Admin) {
                        setGraphic(null);
                    } else {
                        approveBtn.setVisible(user instanceof Seller && !((Seller) user).isApproved());
                        approveBtn.setManaged(user instanceof Seller && !((Seller) user).isApproved());
                        setGraphic(box);
                    }
                }
            }
        });

        usersTable.setItems(users);
    }

    private void setupProductsTable() {
        prodIdCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getProductId())));
        prodNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        prodCategoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        prodPriceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getPrice())));
        prodStockCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getStockQty())));
        prodSellerCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getSellerStoreName() != null ? data.getValue().getSellerStoreName() : ""));
        prodActiveCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Yes" : "No"));

        prodActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button deactivateBtn = new Button("Deactivate");

            {
                deactivateBtn.getStyleClass().add("danger-button");
                deactivateBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    deactivateProduct(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());
                    deactivateBtn.setText(p.isActive() ? "Deactivate" : "Activate");
                    setGraphic(deactivateBtn);
                }
            }
        });

        productsTable.setItems(products);
    }

    private void setupOrdersTable() {
        orderIdCol.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getOrderId()));
        orderDateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt() != null
                        ? data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "N/A"));
        orderCustomerCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getCustomerId())));
        orderTotalCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getTotalAmount())));
        orderStatusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        ordersTable.setItems(orders);
    }

    private void loadStats() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<User> allUsers = userDAO.findAll();
                List<Order> allOrders = orderDAO.findAll();
                double revenue = orderDAO.getTotalRevenue();

                javafx.application.Platform.runLater(() -> {
                    totalUsersLabel.setText("Users: " + allUsers.size());
                    totalOrdersLabel.setText("Orders: " + allOrders.size());
                    totalRevenueLabel.setText(String.format("Revenue: $%.2f", revenue));
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    private void loadUsers() {
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                return userDAO.findAll();
            }
        };
        task.setOnSucceeded(e -> users.setAll(task.getValue()));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load users."));
        new Thread(task).start();
    }

    private void loadProducts() {
        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                return productDAO.findAllIncludingInactive();
            }
        };
        task.setOnSucceeded(e -> products.setAll(task.getValue()));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load products."));
        new Thread(task).start();
    }

    private void loadOrders() {
        Task<List<Order>> task = new Task<>() {
            @Override
            protected List<Order> call() throws Exception {
                return orderDAO.findAll();
            }
        };
        task.setOnSucceeded(e -> orders.setAll(task.getValue()));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load orders."));
        new Thread(task).start();
    }

    private void banUser(User user) {
        if (!AlertUtil.showConfirm("Ban User", "Are you sure you want to ban " + user.getName() + "?")) return;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userDAO.banUser(user.getUserId());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            AlertUtil.showInfo("Banned", user.getName() + " has been banned.");
            loadUsers();
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to ban user."));
        new Thread(task).start();
    }

    private void approveSeller(User user) {
        if (!(user instanceof Seller)) return;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userDAO.approveSeller(user.getUserId());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            AlertUtil.showInfo("Approved", user.getName() + "'s seller account has been approved.");
            loadUsers();
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to approve seller."));
        new Thread(task).start();
    }

    private void deactivateProduct(Product product) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                productDAO.setActive(product.getProductId(), !product.isActive());
                return null;
            }
        };
        task.setOnSucceeded(e -> loadProducts());
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to update product."));
        new Thread(task).start();
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        Main.switchScene("fxml/Login.fxml");
    }
}
