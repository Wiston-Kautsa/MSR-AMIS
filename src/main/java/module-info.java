module com.mycompany.msr.amis {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // Apache POI (automatic modules)
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;

    // JFoenix
    requires com.jfoenix;

    opens com.mycompany.msr.amis to javafx.fxml;

    exports com.mycompany.msr.amis;
}