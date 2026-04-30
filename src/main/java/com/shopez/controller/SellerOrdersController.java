package com.shopez.controller;

import com.shopez.dao.OrderDAO;
import com.shopez.model.Order;
import com.shopez.util.AlertUtil;
import com.shopez.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class SellerOrdersController {

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> orderIdCol;
    @FXML private TableColumn<Order, String> dateCol;
    @FXML private TableColumn<Order, String> customerCol;
    @FXML private TableColumn<Order, String> totalCol;
    @FXML private TableColumn<Order, String> statusCol;
    @FXML private TableColumn<Order, Void> actionCol;

    private final OrderDAO orderDAO = new OrderDAO();
    private SellerDashboardController dashboardController;
    private final ObservableList<Order> orders = FXCollections.observableArrayList();

    public void setDashboardController(SellerDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        orderIdCol.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getOrderId()));
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt() != null
                        ? data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "N/A"));
        customerCol.setCellValueFactory(data -> new SimpleStringProperty("Customer #" + data.getValue().getCustomerId()));
        totalCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getTotalAmount())));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        setupActionColumn();
        orderTable.setItems(orders);
        loadOrders();
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> statusCombo = new ComboBox<>(
                    FXCollections.observableArrayList("PROCESSING", "SHIPPED", "DELIVERED")
            );
            private final Button updateBtn = new Button("Update");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, statusCombo, updateBtn);

            {
                updateBtn.getStyleClass().add("primary-button");
                updateBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    String newStatus = statusCombo.getValue();
                    if (newStatus != null) {
                        updateOrderStatus(order, Order.OrderStatus.valueOf(newStatus));
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Order order = getTableView().getItems().get(getIndex());
                    if (order.getStatus() == Order.OrderStatus.CANCELLED || order.getStatus() == Order.OrderStatus.DELIVERED) {
                        setGraphic(null);
                    } else {
                        statusCombo.setValue(order.getStatus().name());
                        setGraphic(box);
                    }
                }
            }
        });
    }

    private void loadOrders() {
        int sellerId = SessionManager.getCurrentUser().getUserId();
        Task<List<Order>> task = new Task<>() {
            @Override
            protected List<Order> call() throws Exception {
                return orderDAO.findBySeller(sellerId);
            }
        };
        task.setOnSucceeded(e -> orders.setAll(task.getValue()));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load orders."));
        new Thread(task).start();
    }

    private void updateOrderStatus(Order order, Order.OrderStatus newStatus) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                orderDAO.updateStatus(order.getOrderId(), newStatus);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            order.setStatus(newStatus);
            loadOrders();
            AlertUtil.showInfo("Updated", "Order #" + order.getOrderId() + " status updated to " + newStatus.name());
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to update order status."));
        new Thread(task).start();
    }
}
