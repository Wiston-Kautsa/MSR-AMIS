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

import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

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
        refreshDashboard(); // load stats on start
    }

    /* ================= DASHBOARD REFRESH ================= */

    private void refreshDashboard() {
        if (lblAssetsEntered != null) {
            lblAssetsEntered.setText(String.valueOf(getTotalAssets()));
        }

        if (lblAvailableAssets != null) {
            lblAvailableAssets.setText(String.valueOf(getAvailableAssets()));
        }

        if (lblIssuedAssets != null) {
            lblIssuedAssets.setText(String.valueOf(getIssuedAssets()));
        }

        if (lblWaitingReturn != null) {
            lblWaitingReturn.setText(String.valueOf(getWaitingReturn()));
        }
    }

    /* ================= DATABASE METHODS ================= */

    private int getTotalAssets() {
        String sql = "SELECT COUNT(*) FROM equipment";
        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getAvailableAssets() {
        String sql = "SELECT COUNT(*) FROM equipment WHERE status='AVAILABLE'";
        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getIssuedAssets() {
        String sql = "SELECT COUNT(*) FROM distribution WHERE returned=0";
        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getWaitingReturn() {
        String sql = "SELECT COUNT(*) FROM distribution WHERE returned=0";
        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /* ================= PAGE LOADER (FOR OTHER BUTTONS) ================= */

    private void loadPage(String fxml) {

        try {

            URL resource = getClass().getResource("/com/mycompany/msr/amis/" + fxml);

            if (resource == null) {
                System.out.println("FXML not found: " + fxml);
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
            System.out.println("ERROR loading page: " + fxml);
            e.printStackTrace();
        }
    }

    /* ================= DASHBOARD (SCENE SWITCH ONLY) ================= */

    @FXML
    private void openDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/com/mycompany/msr/amis/Dashboard.fxml")
            );

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            System.out.println("ERROR loading Dashboard.fxml");
            e.printStackTrace();
        }
    }

    /* ================= OTHER BUTTONS (CONTENT LOAD) ================= */

    @FXML
    private void openAddEquipment(ActionEvent event) {
        loadPage("AddEquipment.fxml");
    }

    @FXML
    private void openEquipmentList(ActionEvent event) {
        loadPage("EquipmentList.fxml");
    }

    @FXML
    private void openCreateAssignment(ActionEvent event) {
        loadPage("CreateAssignment.fxml");
    }

    @FXML
    private void openDistributeEquipment(ActionEvent event) {
        loadPage("DistributeEquipment.fxml");
    }

    @FXML
    private void openAssignmentList(ActionEvent event) {
        loadPage("AssignmentList.fxml");
    }

    @FXML
    private void openReturnEquipment(ActionEvent event) {
        loadPage("ReturnEquipment.fxml");
    }

    @FXML
    private void openInventoryReport(ActionEvent event) {
        loadPage("InventoryReport.fxml");
    }

    @FXML
    private void openAssignmentReport(ActionEvent event) {
        loadPage("AssignmentReport.fxml");
    }

    @FXML
    private void openDistributionReport(ActionEvent event) {
        loadPage("DistributionReport.fxml");
    }

    @FXML
    private void openReturnReport(ActionEvent event) {
        loadPage("ReturnReport.fxml");
    }

    @FXML
    private void openOutstandingReport(ActionEvent event) {
        loadPage("OutstandingReport.fxml");
    }

    @FXML
    private void openUsers(ActionEvent event) {
        loadPage("Users.fxml");
    }

    @FXML
    private void openLogout(ActionEvent event) {
        loadPage("login.fxml"); // or Login.fxml
    }
}