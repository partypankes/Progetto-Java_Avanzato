module org.gruppo2.progetto_javaavanzato {
    requires javafx.controls;
    requires javafx.fxml;


    opens gruppo2 to javafx.fxml;
    exports gruppo2;
}