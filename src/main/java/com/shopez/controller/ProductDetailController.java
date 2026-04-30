package com.shopez.controller;

import com.shopez.dao.CartDAO;
import com.shopez.dao.ProductDAO;
import com.shopez.dao.ReviewDAO;
import com.shopez.model.*;
import com.shopez.util.AlertUtil;
import com.shopez.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.List;

public class ProductDetailController {

    @FXML private StackPane productImageBox;
    @FXML private ImageView productImageView;
    @FXML private Label productImageIcon;
    @FXML private Label productName;
    @FXML private Label productType;
    @FXML private Label productCategory;
    @FXML private Label productDescription;
    @FXML private Label productPrice;
    @FXML private Label productShipping;
    @FXML private Label productStock;
    @FXML private Label productSeller;
    @FXML private Label productExtra;
    @FXML private Label avgRating;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Button addToCartBtn;
    @FXML private Button watchStockBtn;
    @FXML private ListView<String> reviewList;
    @FXML private javafx.scene.layout.VBox reviewFormBox;
    @FXML private ComboBox<Integer> ratingCombo;
    @FXML private TextArea reviewComment;

    private final ProductDAO productDAO = new ProductDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private CustomerDashboardController dashboardController;
    private Product currentProduct;

    public void setDashboardController(CustomerDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        ratingCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        ratingCombo.setValue(5);
    }

    public void loadProduct(int productId) {
        Task<Product> task = new Task<>() {
            @Override
            protected Product call() throws Exception {
                return productDAO.findById(productId).orElse(null);
            }
        };
        task.setOnSucceeded(e -> {
            currentProduct = task.getValue();
            if (currentProduct == null) {
                AlertUtil.showError("Error", "Product not found.");
                return;
            }
            displayProduct();
            loadReviews();
            checkReviewEligibility();
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to load product."));
        new Thread(task).start();
    }

    private void displayProduct() {
        Image uploaded = ProductListController.loadProductImage(currentProduct);
        if (uploaded != null) {
            productImageView.setImage(uploaded);
            productImageView.setVisible(true);
            productImageIcon.setVisible(false);
        } else {
            productImageView.setImage(null);
            productImageView.setVisible(false);
            productImageIcon.setText(ProductListController.getEmojiForProduct(currentProduct));
            productImageIcon.setVisible(true);
        }
        productName.setText(currentProduct.getName());
        productType.setText(currentProduct.getProductType());
        productCategory.setText("Category: " + currentProduct.getCategory());
        productDescription.setText(currentProduct.getDescription());
        productPrice.setText(String.format("$%.2f", currentProduct.getPrice()));
        productSeller.setText("Sold by: " + (currentProduct.getSellerStoreName() != null ? currentProduct.getSellerStoreName() : "Unknown"));

        if (currentProduct instanceof PhysicalProduct) {
            PhysicalProduct pp = (PhysicalProduct) currentProduct;
            productShipping.setText(String.format("Shipping: $%.2f (%.2f kg)", pp.calculateShipping(), pp.getWeightKg()));
            productExtra.setText("Dimensions: " + pp.getDimensions());
        } else if (currentProduct instanceof DigitalProduct) {
            productShipping.setText("Digital product - no shipping");
            productExtra.setText("Instant download after purchase");
        }

        if (currentProduct.isInStock()) {
            productStock.setText("In Stock (" + currentProduct.getStockQty() + " available)");
            productStock.setStyle("-fx-text-fill: #10B981;");
            quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, currentProduct.getStockQty(), 1));
            addToCartBtn.setDisable(false);
            watchStockBtn.setVisible(false);
            watchStockBtn.setManaged(false);
        } else {
            productStock.setText("Out of Stock");
            productStock.setStyle("-fx-text-fill: #EF4444;");
            quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, 0));
            addToCartBtn.setDisable(true);
            watchStockBtn.setVisible(true);
            watchStockBtn.setManaged(true);
        }

        // Load average rating
        Task<Double> ratingTask = new Task<>() {
            @Override
            protected Double call() throws Exception {
                return reviewDAO.getAverageRating(currentProduct.getProductId());
            }
        };
        ratingTask.setOnSucceeded(e -> {
            double avg = ratingTask.getValue();
            avgRating.setText(avg > 0 ? String.format("%.1f / 5", avg) : "No ratings yet");
        });
        new Thread(ratingTask).start();
    }

    private void loadReviews() {
        Task<List<Review>> task = new Task<>() {
            @Override
            protected List<Review> call() throws Exception {
                return reviewDAO.findByProduct(currentProduct.getProductId());
            }
        };
        task.setOnSucceeded(e -> {
            List<Review> reviews = task.getValue();
            reviewList.getItems().clear();
            if (reviews.isEmpty()) {
                reviewList.getItems().add("No reviews yet.");
            } else {
                for (Review r : reviews) {
                    reviewList.getItems().add(r.getRating() + "/5 - " + r.getCustomerName() + ": " + r.getComment());
                }
            }
        });
        new Thread(task).start();
    }

    private void checkReviewEligibility() {
        if (!SessionManager.isCustomer()) return;
        int customerId = SessionManager.getCurrentUser().getUserId();

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                boolean purchased = reviewDAO.hasCustomerPurchasedProduct(customerId, currentProduct.getProductId());
                boolean reviewed = reviewDAO.hasCustomerReviewedProduct(customerId, currentProduct.getProductId());
                return purchased && !reviewed;
            }
        };
        task.setOnSucceeded(e -> {
            boolean canReview = task.getValue();
            reviewFormBox.setVisible(canReview);
            reviewFormBox.setManaged(canReview);
        });
        new Thread(task).start();
    }

    @FXML
    private void handleAddToCart() {
        if (currentProduct == null) return;
        int qty = quantitySpinner.getValue();

        Customer customer = (Customer) SessionManager.getCurrentUser();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Cart cart = cartDAO.findByCustomer(customer.getUserId());
                CartItem item = new CartItem();
                item.setProduct(currentProduct);
                item.setQuantity(qty);
                item.setUnitPrice(currentProduct.getPrice());
                cartDAO.addItem(cart.getCartId(), item);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            AlertUtil.showInfo("Added to Cart", currentProduct.getName() + " x" + qty + " added to your cart.");
            if (dashboardController != null) {
                dashboardController.refreshCartBadge();
            }
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to add item to cart."));
        new Thread(task).start();
    }

    @FXML
    private void handleWatchStock() {
        if (currentProduct == null) return;
        Customer customer = (Customer) SessionManager.getCurrentUser();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                productDAO.addStockWatcher(customer.getUserId(), currentProduct.getProductId());
                return null;
            }
        };
        task.setOnSucceeded(e -> AlertUtil.showInfo("Watching", "You'll be notified when \"" + currentProduct.getName() + "\" is back in stock."));
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to set up stock notification."));
        new Thread(task).start();
    }

    @FXML
    private void handleSubmitReview() {
        Integer rating = ratingCombo.getValue();
        String comment = reviewComment.getText().trim();

        if (rating == null) {
            AlertUtil.showError("Validation", "Please select a rating.");
            return;
        }
        if (comment.isEmpty()) {
            AlertUtil.showError("Validation", "Please write a review comment.");
            return;
        }

        Review review = new Review();
        review.setCustomerId(SessionManager.getCurrentUser().getUserId());
        review.setProductId(currentProduct.getProductId());
        review.setRating(rating);
        review.setComment(comment);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                reviewDAO.save(review);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            AlertUtil.showInfo("Review Submitted", "Thank you for your review!");
            reviewFormBox.setVisible(false);
            reviewFormBox.setManaged(false);
            loadReviews();
            displayProduct(); // refresh rating
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to submit review."));
        new Thread(task).start();
    }

    @FXML
    private void handleBack() {
        if (dashboardController != null) {
            dashboardController.loadContent("fxml/ProductList.fxml");
        }
    }
}
