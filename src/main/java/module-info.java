module se.kth.databas.databas {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;
    requires org.mongodb.driver.sync.client;

    opens se.kth.databas2 to javafx.fxml;
    opens se.kth.databas2.model to javafx.base;
    exports se.kth.databas2;
}