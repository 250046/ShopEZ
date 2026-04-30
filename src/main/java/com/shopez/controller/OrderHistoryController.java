package com.shopez.controller;

import com.shopez.dao.OrderDAO;
import com.shopez.dao.PaymentDAO;
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
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class OrderHistoryController {

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> orderIdCol;
    @FXML private TableColumn<Order, String> dateCol;
    @FXML private TableColumn<Order, String> totalCol;
    @FXML private TableColumn<Order, String> statusCol;
    @FXML private TableColumn<Order, Void> actionCol;

    @FXML private VBox detailPanel;
    @FXML private Label detailTitle;
    @FXML private TableView<OrderItem> itemsTable;
    @FXML private TableColumn<OrderItem, String> itemNameCol;
    @FXML private TableColumn<OrderItem, String> itemQtyCol;
    @FXML private TableColumn<OrderItem, String> itemPriceCol;
    @FXML private TableColumn<OrderItem, String> itemSubCol;
    @FXML private Label paymentInfo;

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private CustomerDashboardController dashboardController;
    private final ObservableList<Order> orders = FXCollections.observableArrayList();

    public void setDashboardController(CustomerDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        orderIdCol.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getOrderId()));
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt() != null
                        ? data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "N/A"));
        totalCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getTotalAmount())));
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        setupActionColumn();

        itemNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getName()));
        itemQtyCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        itemPriceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getPriceAtPurchase())));
        itemSubCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getSubtotal())));

        orderTable.setItems(orders);

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showOrderDetail(newVal);
            }
        });

        loadOrders();
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = new Button("Cancel");

            {
                cancelBtn.getStyleClass().add("danger-button");
                cancelBtn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    cancelOrder(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Order order = getTableView().getItems().get(getIndex());
                    if (order.getStatus() == Order.OrderStatus.PENDING) {
                        setGraphic(cancelBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void loadOrders() {
        int customerId = SessionManager.getCurrentUser().getUserId();
        Task<List<Order>> task = new Task<>() {
            @Override
            protected List<Order> call() throws Exception {
                return orderDAO.findByCustomer(customerId);
            }
        };
        task.setOnSucceeded(e -> orders.setAll(task.getValue()));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load orders."));
        new Thread(task).start();
    }

    private void showOrderDetail(Order order) {
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        detailTitle.setText("Order #" + order.getOrderId() + " Details");
        itemsTable.setItems(FXCollections.observableArrayList(order.getItems()));

        // Load payment info
        Task<Optional<Payment>> task = new Task<>() {
            @Override
            protected Optional<Payment> call() throws Exception {
                return paymentDAO.findByOrder(order.getOrderId());
            }
        };
        task.setOnSucceeded(e -> {
            Optional<Payment> p = task.getValue();
            if (p.isPresent()) {
                Payment payment = p.get();
                paymentInfo.setText("Payment: " + payment.getPaymentMethod() + " | Status: " + payment.getStatus().name());
            } else {
                paymentInfo.setText("Payment information not available.");
            }
        });
        new Thread(task).start();
    }

    private void cancelOrder(Order order) {
        if (!AlertUtil.showConfirm("Cancel Order", "Are you sure you want to cancel order #" + order.getOrderId() + "?")) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                orderDAO.updateStatus(order.getOrderId(), Order.OrderStatus.CANCELLED);

                // Refund if wallet payment
                Optional<Payment> paymentOpt = paymentDAO.findByOrder(order.getOrderId());
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    if ("WALLET".equals(payment.getPaymentMethod()) && payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                        Customer customer = (Customer) SessionManager.getCurrentUser();
                        customer.topUpWallet(payment.getAmount());
                        userDAO.updateWalletBalance(customer.getUserId(), customer.getWalletBalance());
                        paymentDAO.updateStatus(payment.getPaymentId(), Payment.PaymentStatus.REFUNDED);
                    }
                }

                // Restore stock
                for (OrderItem item : order.getItems()) {
                    Product p = item.getProduct();
                    productDAO.updateStock(p.getProductId(), p.getStockQty() + item.getQuantity());
                }

                return null;
            }
        };

        task.setOnSucceeded(e -> {
            AlertUtil.showInfo("Cancelled", "Order #" + order.getOrderId() + " has been cancelled.");
            loadOrders();
            if (dashboardController != null) {
                dashboardController.refreshWallet();
            }
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to cancel order."));
        new Thread(task).start();
    }
}
