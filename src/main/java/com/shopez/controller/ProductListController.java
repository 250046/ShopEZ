package com.shopez.controller;

import com.shopez.dao.ProductDAO;
import com.shopez.dao.ReviewDAO;
import com.shopez.model.Product;
import com.shopez.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.util.List;

public class ProductListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private FlowPane productsPane;
    @FXML private Label resultCountLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private CustomerDashboardController dashboardController;

    public void setDashboardController(CustomerDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        loadProducts();
        loadCategories();
    }

    private void loadProducts() {
        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                return productDAO.findAll();
            }
        };
        task.setOnSucceeded(e -> renderProducts(task.getValue()));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load products."));
        new Thread(task).start();
    }

    private void loadCategories() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                return productDAO.getAllCategories();
            }
        };
        task.setOnSucceeded(e -> {
            List<String> categories = task.getValue();
            categories.add(0, "All Categories");
            categoryCombo.setItems(FXCollections.observableArrayList(categories));
        });
        new Thread(task).start();
    }

    private void renderProducts(List<Product> products) {
        productsPane.getChildren().clear();
        resultCountLabel.setText(products.size() + " product" + (products.size() == 1 ? "" : "s"));

        if (products.isEmpty()) {
            Label empty = new Label("No products found.");
            empty.getStyleClass().add("text-secondary");
            productsPane.getChildren().add(empty);
            return;
        }

        for (Product p : products) {
            productsPane.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(200);
        card.setMaxWidth(200);

        // Image area: uploaded photo OR emoji fallback
        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("product-image");
        imageBox.setPrefHeight(150);
        imageBox.setMinHeight(150);

        Image uploaded = loadProductImage(product);
        if (uploaded != null) {
            ImageView iv = new ImageView(uploaded);
            iv.setFitWidth(180);
            iv.setFitHeight(140);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            imageBox.getChildren().add(iv);
        } else {
            Label icon = new Label(getEmojiForProduct(product));
            icon.getStyleClass().add("product-icon");
            imageBox.getChildren().add(icon);
        }

        // Category badge (overlay top-left)
        Label categoryBadge = new Label(product.getCategory() != null ? product.getCategory() : "");
        categoryBadge.getStyleClass().add("category-badge");
        StackPane.setAlignment(categoryBadge, Pos.TOP_LEFT);
        StackPane.setMargin(categoryBadge, new javafx.geometry.Insets(8, 0, 0, 8));
        imageBox.getChildren().add(categoryBadge);

        // Stock indicator
        if (!product.isInStock()) {
            Label stockBadge = new Label("Out of stock");
            stockBadge.getStyleClass().add("stock-badge-out");
            StackPane.setAlignment(stockBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(stockBadge, new javafx.geometry.Insets(8, 8, 0, 0));
            imageBox.getChildren().add(stockBadge);
        }

        // Product name
        Label name = new Label(product.getName());
        name.getStyleClass().add("product-card-name");
        name.setWrapText(true);
        name.setMaxWidth(180);

        // Price
        Label price = new Label(String.format("$%.2f", product.getPrice()));
        price.getStyleClass().add("product-card-price");

        // Seller
        Label seller = new Label("By " + (product.getSellerStoreName() != null ? product.getSellerStoreName() : "Unknown"));
        seller.getStyleClass().add("text-secondary");

        // Rating
        Label rating = new Label("Loading rating...");
        rating.getStyleClass().add("text-secondary");
        Task<Double> ratingTask = new Task<>() {
            @Override
            protected Double call() throws Exception {
                return reviewDAO.getAverageRating(product.getProductId());
            }
        };
        ratingTask.setOnSucceeded(ev -> {
            double avg = ratingTask.getValue();
            rating.setText(avg > 0 ? "★ " + String.format("%.1f/5", avg) : "No ratings yet");
        });
        new Thread(ratingTask).start();

        // View button
        Button viewBtn = new Button("View Details");
        viewBtn.getStyleClass().add("primary-button");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> {
            if (dashboardController != null) {
                dashboardController.loadProductDetail(product.getProductId());
            }
        });

        card.getChildren().addAll(imageBox, name, price, seller, rating, viewBtn);

        // Click on card also opens detail
        card.setOnMouseClicked(e -> {
            if (e.getTarget() != viewBtn && dashboardController != null) {
                dashboardController.loadProductDetail(product.getProductId());
            }
        });

        return card;
    }

    public static Image loadProductImage(Product product) {
        if (product.getImagePath() == null || product.getImagePath().isEmpty()) return null;
        try {
            File f = new File(product.getImagePath());
            if (f.exists()) {
                return new Image(f.toURI().toString(), 300, 300, true, true);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String getEmojiForProduct(Product product) {
        String name = product.getName() != null ? product.getName().toLowerCase() : "";
        String category = product.getCategory() != null ? product.getCategory().toLowerCase() : "";

        if (name.contains("keyboard")) return "⌨";
        if (name.contains("mouse")) return "🖱";
        if (name.contains("hub") || name.contains("usb")) return "🔌";
        if (name.contains("headphone") || name.contains("headset")) return "🎧";
        if (name.contains("phone") || name.contains("mobile")) return "📱";
        if (name.contains("laptop") || name.contains("computer")) return "💻";
        if (name.contains("camera")) return "📷";
        if (name.contains("watch")) return "⌚";
        if (name.contains("speaker")) return "🔊";
        if (name.contains("book") || name.contains("ebook")) return "📖";
        if (name.contains("course") || name.contains("class") || name.contains("masterclass")) return "🎓";
        if (name.contains("java") || name.contains("code")) return "☕";
        if (category.contains("electron")) return "🔌";
        if (category.contains("educat")) return "📚";
        if (category.contains("cloth") || category.contains("fashion")) return "👕";
        if (category.contains("beauty")) return "💄";
        if (category.contains("food") || category.contains("grocer")) return "🍎";
        if (category.contains("toy")) return "🧸";
        if (category.contains("sport")) return "⚽";
        return "📦";
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadProducts();
            return;
        }
        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                return productDAO.search(keyword);
            }
        };
        task.setOnSucceeded(e -> renderProducts(task.getValue()));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Search failed."));
        new Thread(task).start();
    }

    @FXML
    private void handleCategoryFilter() {
        String category = categoryCombo.getValue();
        if (category == null || "All Categories".equals(category)) {
            loadProducts();
            return;
        }
        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                return productDAO.findByCategory(category);
            }
        };
        task.setOnSucceeded(e -> renderProducts(task.getValue()));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Filter failed."));
        new Thread(task).start();
    }

    @FXML
    private void handleClearFilter() {
        searchField.clear();
        categoryCombo.setValue(null);
        loadProducts();
    }
}
