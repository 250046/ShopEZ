package com.shopez.controller;

import com.shopez.dao.CartDAO;
import com.shopez.model.Cart;
import com.shopez.model.CartItem;
import com.shopez.model.Customer;
import com.shopez.util.AlertUtil;
import com.shopez.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class CartController {

    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> productCol;
    @FXML private TableColumn<CartItem, String> priceCol;
    @FXML private TableColumn<CartItem, String> qtyCol;
    @FXML private TableColumn<CartItem, String> subtotalCol;
    @FXML private TableColumn<CartItem, Void> removeCol;
    @FXML private Label totalLabel;
    @FXML private Button checkoutBtn;

    private final CartDAO cartDAO = new CartDAO();
    private CustomerDashboardController dashboardController;
    private Cart currentCart;
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    public void setDashboardController(CustomerDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        productCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getName()));
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getUnitPrice())));
        qtyCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        subtotalCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getSubtotal())));

        // Editable quantity via cell click
        qtyCol.setCellFactory(col -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>(1, 999, 1);

            {
                spinner.setEditable(true);
                spinner.setPrefWidth(80);
                spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && oldVal != null && !newVal.equals(oldVal)) {
                        CartItem item = getTableView().getItems().get(getIndex());
                        updateQuantity(item, newVal);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CartItem cartItem = getTableView().getItems().get(getIndex());
                    spinner.getValueFactory().setValue(cartItem.getQuantity());
                    setGraphic(spinner);
                }
            }
        });

        setupRemoveColumn();
        cartTable.setItems(cartItems);

        loadCart();
    }

    private void setupRemoveColumn() {
        removeCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");

            {
                removeBtn.getStyleClass().add("danger-button");
                removeBtn.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    removeItem(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });
    }

    private void loadCart() {
        Customer customer = (Customer) SessionManager.getCurrentUser();
        Task<Cart> task = new Task<>() {
            @Override
            protected Cart call() throws Exception {
                return cartDAO.findByCustomer(customer.getUserId());
            }
        };
        task.setOnSucceeded(e -> {
            currentCart = task.getValue();
            cartItems.setAll(currentCart.getItems());
            updateTotal();
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load cart."));
        new Thread(task).start();
    }

    private void updateQuantity(CartItem item, int newQty) {
        if (currentCart == null) return;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                cartDAO.updateItemQty(currentCart.getCartId(), item.getProduct().getProductId(), newQty);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            item.setQuantity(newQty);
            updateTotal();
            if (dashboardController != null) {
                dashboardController.refreshCartBadge();
            }
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to update quantity."));
        new Thread(task).start();
    }

    private void removeItem(CartItem item) {
        if (currentCart == null) return;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                cartDAO.removeItem(currentCart.getCartId(), item.getProduct().getProductId());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            cartItems.remove(item);
            updateTotal();
            if (dashboardController != null) {
                dashboardController.refreshCartBadge();
            }
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to remove item."));
        new Thread(task).start();
    }

    private void updateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getSubtotal();
        }
        totalLabel.setText(String.format("Total: $%.2f", total));
        checkoutBtn.setDisable(cartItems.isEmpty());
    }

    @FXML
    private void handleCheckout() {
        if (cartItems.isEmpty()) {
            AlertUtil.showError("Empty Cart", "Your cart is empty.");
            return;
        }
        if (dashboardController != null) {
            dashboardController.loadContent("fxml/Checkout.fxml");
        }
    }
}
