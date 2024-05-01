module gruppo2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens gruppo2 to javafx.fxml;
    exports gruppo2;
}