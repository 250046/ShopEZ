module com.shopez {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.shopez to javafx.fxml;
    opens com.shopez.controller to javafx.fxml;
    opens com.shopez.model to javafx.base;

    exports com.shopez;
    exports com.shopez.controller;
    exports com.shopez.model;
    exports com.shopez.dao;
    exports com.shopez.db;
    exports com.shopez.util;
    exports com.shopez.observer;
}
