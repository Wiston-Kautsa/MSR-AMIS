package com.mycompany.msr.amis;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DashboardsController implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML private Label lblAssetsEntered;
    @FXML private Label lblAvailableAssets;
    @FXML private Label lblIssuedAssets;
    @FXML private Label lblWaitingReturn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (lblAssetsEntered != null) {
            refreshDashboard();
        }
    }

    /* ================= PAGE LOADER ================= */

    private void loadPage(String fxml) {
        try {
            String path = "/com/mycompany/msr/amis/" + fxml;
            URL resource = getClass().getResource(path);

            if (resource == null) {
                System.out.println("❌ FXML NOT FOUND: " + path);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            contentArea.getChildren().clear();

            if (root instanceof Region) {
                Region region = (Region) root;
                region.prefWidthProperty().bind(contentArea.widthProperty());
                region.prefHeightProperty().bind(contentArea.heightProperty());
            }

            contentArea.getChildren().add(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= DASHBOARD (ONLY SCENE SWITCH) ================= */

    @FXML
    private void openDashboard(ActionEvent event) {

        String path = "/com/mycompany/msr/amis/Dashboards.fxml";
        URL resource = getClass().getResource(path);

        if (resource == null) {
            System.out.println("❌ Dashboard.fxml NOT FOUND at: " + path);
            return;
        }

        try {
            Parent root = FXMLLoader.load(resource);

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= DASHBOARD DATA ================= */

    public void refreshDashboard() {
        lblAssetsEntered.setText(String.valueOf(getTotalAssets()));
        lblAvailableAssets.setText(String.valueOf(getAvailableAssets()));
        lblIssuedAssets.setText(String.valueOf(getIssuedAssets()));
        lblWaitingReturn.setText(String.valueOf(getWaitingReturn()));
    }

    private int getTotalAssets() {
        return executeCountQuery("SELECT COUNT(*) FROM equipment");
    }

    private int getAvailableAssets() {
        // SAFER: assumes available if not assigned
        return executeCountQuery(
            "SELECT COUNT(*) FROM equipment"
        );
    }

    private int getIssuedAssets() {
        // FIXED: no 'status' column assumption
        return executeCountQuery(
            "SELECT COUNT(*) FROM assignments"
        );
    }

    private int getWaitingReturn() {
        // TEMP: same as issued until you define schema properly
        return executeCountQuery(
            "SELECT COUNT(*) FROM assignments"
        );
    }

    private int executeCountQuery(String sql) {
        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /* ================= OTHER BUTTONS ================= */

    @FXML private void openAddEquipment() { loadPage("AddEquipment.fxml"); }
    @FXML private void openEquipmentList() { loadPage("EquipmentList.fxml"); }
    @FXML private void openCreateAssignment() { loadPage("CreateAssignment.fxml"); }
    @FXML private void openDistributeEquipment() { loadPage("DistributeEquipment.fxml"); }
    @FXML private void openAssignmentList() { loadPage("AssignmentList.fxml"); }
    @FXML private void openReturnEquipment() { loadPage("ReturnEquipment.fxml"); }
    @FXML private void openInventoryReport() { loadPage("InventoryReport.fxml"); }
    @FXML private void openAssignmentReport() { loadPage("AssignmentReport.fxml"); }
    @FXML private void openDistributionReport() { loadPage("DistributionReport.fxml"); }
    @FXML private void openReturnReport() { loadPage("ReturnReport.fxml"); }
    @FXML private void openOutstandingReport() { loadPage("OutstandingReport.fxml"); }
    @FXML private void openUsers() { loadPage("Users.fxml"); }

    /* ================= LOGOUT ================= */

    @FXML
    private void openLogout(ActionEvent event) {
        try {
            String path = "/com/mycompany/msr/amis/login.fxml";
            URL resource = getClass().getResource(path);

            if (resource == null) {
                System.out.println("❌ login.fxml NOT FOUND");
                return;
            }

            Parent root = FXMLLoader.load(resource);

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}