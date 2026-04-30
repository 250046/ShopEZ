package com.shopez.controller;

import com.shopez.dao.ProductDAO;
import com.shopez.model.Product;
import com.shopez.util.AlertUtil;
import com.shopez.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;

public class SellerProductsController {

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> nameCol;
    @FXML private TableColumn<Product, String> categoryCol;
    @FXML private TableColumn<Product, String> priceCol;
    @FXML private TableColumn<Product, String> stockCol;
    @FXML private TableColumn<Product, String> typeCol;
    @FXML private TableColumn<Product, String> activeCol;
    @FXML private TableColumn<Product, Void> actionCol;

    private final ProductDAO productDAO = new ProductDAO();
    private SellerDashboardController dashboardController;
    private final ObservableList<Product> products = FXCollections.observableArrayList();

    public void setDashboardController(SellerDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        categoryCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        priceCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getPrice())));
        stockCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getStockQty())));
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductType()));
        activeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Yes" : "No"));

        setupActionColumn();
        productTable.setItems(products);
        loadProducts();
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button toggleBtn = new Button("Toggle");
            private final HBox box = new HBox(5, editBtn, deleteBtn, toggleBtn);

            {
                editBtn.getStyleClass().add("primary-button");
                deleteBtn.getStyleClass().add("danger-button");
                toggleBtn.getStyleClass().add("secondary-button");

                editBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (dashboardController != null) {
                        dashboardController.loadAddEditProduct(p);
                    }
                });

                deleteBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (AlertUtil.showConfirm("Delete Product", "Are you sure you want to delete \"" + p.getName() + "\"?")) {
                        deleteProduct(p);
                    }
                });

                toggleBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    toggleActive(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadProducts() {
        int sellerId = SessionManager.getCurrentUser().getUserId();
        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                return productDAO.findBySeller(sellerId);
            }
        };
        task.setOnSucceeded(e -> products.setAll(task.getValue()));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load products."));
        new Thread(task).start();
    }

    private void deleteProduct(Product product) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                productDAO.delete(product.getProductId());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            products.remove(product);
            AlertUtil.showInfo("Deleted", "Product deleted successfully.");
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to delete product. It may have existing orders."));
        new Thread(task).start();
    }

    private void toggleActive(Product product) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                productDAO.setActive(product.getProductId(), !product.isActive());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            product.setActive(!product.isActive());
            loadProducts();
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to toggle product status."));
        new Thread(task).start();
    }

    @FXML
    private void handleAddProduct() {
        if (dashboardController != null) {
            dashboardController.loadAddEditProduct(null);
        }
    }
}
