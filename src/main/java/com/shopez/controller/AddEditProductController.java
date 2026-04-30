package com.shopez.controller;

import com.shopez.dao.ProductDAO;
import com.shopez.model.DigitalProduct;
import com.shopez.model.PhysicalProduct;
import com.shopez.model.Product;
import com.shopez.util.AlertUtil;
import com.shopez.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

public class AddEditProductController {

    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private RadioButton physicalRadio;
    @FXML private RadioButton digitalRadio;
    @FXML private ToggleGroup typeGroup;
    @FXML private VBox physicalBox;
    @FXML private VBox digitalBox;
    @FXML private TextField weightField;
    @FXML private TextField dimensionsField;
    @FXML private TextField downloadUrlField;

    // Image fields
    @FXML private StackPane imagePreviewBox;
    @FXML private ImageView imagePreview;
    @FXML private Label imagePlaceholder;
    @FXML private Label imageFileLabel;
    @FXML private Button removeImageBtn;

    private final ProductDAO productDAO = new ProductDAO();
    private SellerDashboardController dashboardController;
    private Product editingProduct;
    private String currentImagePath;

    private static final Path IMAGES_DIR = Paths.get(System.getProperty("user.home"), ".shopez", "images");

    public void setDashboardController(SellerDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    private void initialize() {
        typeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isPhysical = newVal == physicalRadio;
            physicalBox.setVisible(isPhysical);
            physicalBox.setManaged(isPhysical);
            digitalBox.setVisible(!isPhysical);
            digitalBox.setManaged(!isPhysical);
        });

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                return productDAO.getAllCategories();
            }
        };
        task.setOnSucceeded(e -> categoryCombo.setItems(FXCollections.observableArrayList(task.getValue())));
        new Thread(task).start();

        try {
            Files.createDirectories(IMAGES_DIR);
        } catch (IOException e) {
            AlertUtil.showError("Error", "Could not create images directory: " + e.getMessage());
        }
    }

    public void setProduct(Product product) {
        this.editingProduct = product;
        formTitle.setText("Edit Product");
        nameField.setText(product.getName());
        descField.setText(product.getDescription());
        priceField.setText(String.valueOf(product.getPrice()));
        stockField.setText(String.valueOf(product.getStockQty()));
        categoryCombo.setValue(product.getCategory());

        if (product instanceof PhysicalProduct) {
            physicalRadio.setSelected(true);
            PhysicalProduct pp = (PhysicalProduct) product;
            weightField.setText(String.valueOf(pp.getWeightKg()));
            dimensionsField.setText(pp.getDimensions());
        } else if (product instanceof DigitalProduct) {
            digitalRadio.setSelected(true);
            DigitalProduct dp = (DigitalProduct) product;
            downloadUrlField.setText(dp.getDownloadUrl());
        }

        // Load existing image
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            currentImagePath = product.getImagePath();
            loadImagePreview(currentImagePath);
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Product Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selected = fc.showOpenDialog(imagePreviewBox.getScene().getWindow());
        if (selected == null) return;

        // Validate size (5 MB max)
        if (selected.length() > 5 * 1024 * 1024) {
            AlertUtil.showError("File Too Large", "Please choose an image smaller than 5 MB.");
            return;
        }

        try {
            // Generate unique filename, preserving extension
            String originalName = selected.getName();
            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot > 0) ext = originalName.substring(dot);
            String newFileName = UUID.randomUUID().toString() + ext;

            Path destination = IMAGES_DIR.resolve(newFileName);
            Files.createDirectories(IMAGES_DIR);
            Files.copy(selected.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            currentImagePath = destination.toString();
            loadImagePreview(currentImagePath);
            imageFileLabel.setText(originalName);
        } catch (IOException e) {
            AlertUtil.showError("Upload Failed", "Could not save image: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveImage() {
        currentImagePath = null;
        imagePreview.setImage(null);
        imagePlaceholder.setVisible(true);
        removeImageBtn.setVisible(false);
        removeImageBtn.setManaged(false);
        imageFileLabel.setText("JPG, PNG up to 5MB");
    }

    private void loadImagePreview(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                Image img = new Image(file.toURI().toString(), 110, 110, true, true);
                imagePreview.setImage(img);
                imagePlaceholder.setVisible(false);
                removeImageBtn.setVisible(true);
                removeImageBtn.setManaged(true);
            }
        } catch (Exception e) {
            AlertUtil.showError("Preview Error", "Failed to load image preview.");
        }
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String desc = descField.getText().trim();
        String priceStr = priceField.getText().trim();
        String stockStr = stockField.getText().trim();
        String category = categoryCombo.getValue();

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            AlertUtil.showError("Validation", "Please fill in name, price, and stock fields.");
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            AlertUtil.showError("Validation", "Price and stock must be valid numbers.");
            return;
        }

        if (price <= 0) {
            AlertUtil.showError("Validation", "Price must be greater than zero.");
            return;
        }

        boolean isPhysical = physicalRadio.isSelected();

        if (isPhysical) {
            String weightStr = weightField.getText().trim();
            if (weightStr.isEmpty() || dimensionsField.getText().trim().isEmpty()) {
                AlertUtil.showError("Validation", "Physical products require weight and dimensions.");
                return;
            }
        }

        Product product;
        if (isPhysical) {
            PhysicalProduct pp;
            if (editingProduct instanceof PhysicalProduct) {
                pp = (PhysicalProduct) editingProduct;
            } else {
                pp = new PhysicalProduct();
            }
            pp.setWeightKg(Double.parseDouble(weightField.getText().trim()));
            pp.setDimensions(dimensionsField.getText().trim());
            product = pp;
        } else {
            DigitalProduct dp;
            if (editingProduct instanceof DigitalProduct) {
                dp = (DigitalProduct) editingProduct;
            } else {
                dp = new DigitalProduct();
                dp.setLicenseKey(dp.generateLicense());
            }
            dp.setDownloadUrl(downloadUrlField.getText().trim());
            product = dp;
        }

        product.setName(name);
        product.setDescription(desc);
        product.setPrice(price);
        product.setStockQty(stock);
        product.setCategory(category != null ? category : "");
        product.setSellerId(SessionManager.getCurrentUser().getUserId());
        product.setActive(true);
        product.setImagePath(currentImagePath);

        if (editingProduct != null) {
            product.setProductId(editingProduct.getProductId());
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (editingProduct != null) {
                    productDAO.update(product);
                } else {
                    productDAO.save(product);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            AlertUtil.showInfo("Success", editingProduct != null ? "Product updated." : "Product added.");
            if (dashboardController != null) {
                dashboardController.loadContent("fxml/SellerProducts.fxml");
            }
        });
        task.setOnFailed(e -> AlertUtil.showError("Error", "Failed to save product."));
        new Thread(task).start();
    }

    @FXML
    private void handleCancel() {
        if (dashboardController != null) {
            dashboardController.loadContent("fxml/SellerProducts.fxml");
        }
    }
}
