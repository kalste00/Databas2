module se.kth.databas2.databas2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mongo.java.driver;

    opens se.kth.databas2 to javafx.fxml;
    opens se.kth.databas2.model to javafx.base;

    exports se.kth.databas2;
}