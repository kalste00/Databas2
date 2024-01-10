module se.kth.databas2.databas2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;
    requires org.mongodb.driver.sync.client;
    requires java.management;

    opens se.kth.databas2 to javafx.fxml;
    opens se.kth.databas2.model to javafx.base;
    exports se.kth.databas2;
}