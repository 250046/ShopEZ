package com.shopez;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("ShopEZ");
        switchScene("fxml/Login.fxml");
        primaryStage.show();
    }

    public static void switchScene(String fxmlPath) {
        try {
            URL fxmlUrl = Main.class.getResource("/com/shopez/" + fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("FXML file not found: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, 900, 650);

            URL cssUrl = Main.class.getResource("/com/shopez/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath + " — " + e.getMessage(), e);
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
