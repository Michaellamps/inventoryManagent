module com.example.inventorymanagent {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.controlsfx.controls;

    opens com.example.inventorymanagent to javafx.fxml;
    exports com.example.inventorymanagent;
}