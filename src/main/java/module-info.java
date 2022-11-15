module com.example.tsbv3 {
    requires javafx.controls;
    requires javafx.fxml;


    opens interfaz to javafx.fxml;
    exports interfaz;
}