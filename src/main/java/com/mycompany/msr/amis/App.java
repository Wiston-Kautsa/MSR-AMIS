package com.mycompany.msr.amis;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;   // ✅ FIXED (missing import)
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        DatabaseHandler.initializeDatabase();
        
        Parent root = FXMLLoader.load(
                getClass().getResource("Dashboards.fxml")
        );

        Scene scene = new Scene(root);

        stage.setTitle("MSR AMIS");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}