module gruppo2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens gruppo2 to javafx.fxml;
    exports gruppo2;
}