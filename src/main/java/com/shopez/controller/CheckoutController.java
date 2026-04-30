package com.shopez.controller;

import com.shopez.dao.*;
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

public class CheckoutController {

    @FXML private TableView<CartItem> summaryTable;
    @FXML private TableColumn<CartItem, String> itemCol;
    @FXML private TableColumn<CartItem, String> itemQtyCol;
    @FXML private TableColumn<CartItem, String> itemPriceCol;
    @FXML private TableColumn<CartItem, String> itemSubtotalCol;
    @FXML private Label orderTotalLabel;
    @FXML private TextField addressField;
    @FXML private RadioButton creditCardRadio;
    @FXML private RadioButton walletRadio;
    @FXML private ToggleGroup paymentGroup;
    @FXML private VBox creditCardBox;
    @FXML private VBox walletBox;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardHolderField;
    @FXML private Label walletBalanceLabel;
    @FXML private Label walletWarning;

    private final CartDAO cartDAO = new CartDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final UserDAO userDAO = new UserDAO();
    private CustomerDashboardController dashboardController;
    private Cart currentCart;
    private final ObservableList<CartItem> items = FXCollections.observableArrayList();

    public void setDashboardController(CustomerDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        itemCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduct().getName()));
        itemQtyCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        itemPriceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getUnitPrice())));
        itemSubtotalCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getSubtotal())));
        summaryTable.setItems(items);

        // Payment method toggle
        paymentGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isWallet = newVal == walletRadio;
            creditCardBox.setVisible(!isWallet);
            creditCardBox.setManaged(!isWallet);
            walletBox.setVisible(isWallet);
            walletBox.setManaged(isWallet);
        });

        Customer customer = (Customer) SessionManager.getCurrentUser();
        addressField.setText(customer.getShippingAddress() != null ? customer.getShippingAddress() : "");
        walletBalanceLabel.setText(String.format("Available balance: $%.2f", customer.getWalletBalance()));

        loadCart();
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
            items.setAll(currentCart.getItems());
            double total = currentCart.getSubtotal();
            orderTotalLabel.setText(String.format("Total: $%.2f", total));

            // Check wallet sufficiency
            if (total > customer.getWalletBalance()) {
                walletWarning.setText("Insufficient wallet balance for this order.");
                walletWarning.setVisible(true);
                walletWarning.setManaged(true);
            }
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load cart."));
        new Thread(task).start();
    }

    @FXML
    private void handlePlaceOrder() {
        if (currentCart == null || currentCart.getItems().isEmpty()) {
            AlertUtil.showError("Empty Cart", "Your cart is empty.");
            return;
        }

        String address = addressField.getText().trim();
        if (address.isEmpty()) {
            AlertUtil.showError("Validation", "Please enter a shipping address.");
            return;
        }

        boolean isWallet = walletRadio.isSelected();
        if (!isWallet) {
            if (cardNumberField.getText().trim().isEmpty() || cardHolderField.getText().trim().isEmpty()) {
                AlertUtil.showError("Validation", "Please fill in all credit card fields.");
                return;
            }
        }

        Customer customer = (Customer) SessionManager.getCurrentUser();
        double total = currentCart.getSubtotal();

        if (isWallet && customer.getWalletBalance() < total) {
            AlertUtil.showError("Insufficient Funds", "Your wallet balance is insufficient for this order.");
            return;
        }

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                // Stock check (race condition guard)
                for (CartItem item : currentCart.getItems()) {
                    Product freshProduct = productDAO.findById(item.getProduct().getProductId())
                            .orElseThrow(() -> new OutOfStockException("Product not found: " + item.getProduct().getName()));
                    if (freshProduct.getStockQty() < item.getQuantity()) {
                        throw new OutOfStockException("Not enough stock for \"" + freshProduct.getName()
                                + "\". Available: " + freshProduct.getStockQty());
                    }
                }

                // Create order
                Order order = new Order();
                order.setCustomerId(customer.getUserId());
                order.setShippingAddress(address);
                order.setTotalAmount(total);
                order.setStatus(Order.OrderStatus.PENDING);

                for (CartItem ci : currentCart.getItems()) {
                    OrderItem oi = new OrderItem();
                    oi.setProduct(ci.getProduct());
                    oi.setQuantity(ci.getQuantity());
                    oi.setPriceAtPurchase(ci.getUnitPrice());
                    order.getItems().add(oi);
                }

                orderDAO.save(order);

                // Deduct stock
                for (CartItem ci : currentCart.getItems()) {
                    Product p = ci.getProduct();
                    int newStock = p.getStockQty() - ci.getQuantity();
                    productDAO.updateStock(p.getProductId(), Math.max(0, newStock));
                }

                // Process payment
                Payment payment;
                if (isWallet) {
                    WalletPayment wp = new WalletPayment();
                    wp.setOrderId(order.getOrderId());
                    wp.setAmount(total);
                    wp.setWalletBalance(customer.getWalletBalance());
                    wp.processPayment();
                    payment = wp;

                    customer.deductWallet(total);
                    userDAO.updateWalletBalance(customer.getUserId(), customer.getWalletBalance());
                } else {
                    CreditCardPayment cc = new CreditCardPayment();
                    cc.setOrderId(order.getOrderId());
                    cc.setAmount(total);
                    String cardNum = cardNumberField.getText().trim();
                    cc.setMaskedCardNumber("****-****-****-" + cardNum.substring(Math.max(0, cardNum.length() - 4)));
                    cc.setCardHolder(cardHolderField.getText().trim());
                    cc.processPayment();
                    payment = cc;
                }
                paymentDAO.save(payment);

                // Clear cart
                cartDAO.clearCart(currentCart.getCartId());

                return order.getOrderId();
            }
        };

        task.setOnSucceeded(e -> {
            int orderId = task.getValue();
            AlertUtil.showInfo("Order Placed", "Order #" + orderId + " has been placed successfully!");
            if (dashboardController != null) {
                dashboardController.refreshCartBadge();
                dashboardController.refreshWallet();
                dashboardController.loadContent("fxml/OrderHistory.fxml");
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof OutOfStockException) {
                AlertUtil.showError("Out of Stock", ex.getMessage());
            } else if (ex instanceof InsufficientFundsException) {
                AlertUtil.showError("Insufficient Funds", ex.getMessage());
            } else {
                AlertUtil.showError("Checkout Error", "Failed to place order: " + (ex != null ? ex.getMessage() : "Unknown error"));
            }
        });

        new Thread(task).start();
    }
}
