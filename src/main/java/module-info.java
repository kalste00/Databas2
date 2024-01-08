module se.kth.databas2.databas2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens se.kth.databas2.databas2 to javafx.fxml;
    exports se.kth.databas2.databas2;
}