package com.mycompany.msr.amis;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class DashboardsController implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML
    private Label lblAssetsEntered;

    @FXML
    private Label lblAvailableAssets;

    @FXML
    private Label lblIssuedAssets;

    @FXML
    private Label lblWaitingReturn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        if (lblAssetsEntered != null) {
            lblAssetsEntered.setText("0");
        }

        if (lblAvailableAssets != null) {
            lblAvailableAssets.setText("0");
        }

        if (lblIssuedAssets != null) {
            lblIssuedAssets.setText("0");
        }

        if (lblWaitingReturn != null) {
            lblWaitingReturn.setText("0");
        }

    }

    /* ================= PAGE LOADER ================= */

    private void loadPage(String fxml) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxml)
            );

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

    /* ================= DASHBOARD ================= */

    @FXML
    private void openDashboard(ActionEvent event) {

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource("Dashboards.fxml")
            );

            javafx.stage.Stage stage =
                    (javafx.stage.Stage) contentArea.getScene().getWindow();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /* ================= EQUIPMENT ================= */

    @FXML
    private void openAddEquipment(ActionEvent event) {
        loadPage("AddEquipment.fxml");
    }

    @FXML
    private void openEquipmentList(ActionEvent event) {
        loadPage("EquipmentList.fxml");
    }

    /* ================= ASSIGNMENTS ================= */

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

    /* ================= RETURNS ================= */

    @FXML
    private void openReturnEquipment(ActionEvent event) {
        loadPage("ReturnEquipment.fxml");
    }

    /* ================= REPORTS ================= */

    @FXML
    private void openInventoryReport(ActionEvent event) {
        loadPage("InventoryReport.fxml");
    }

    @FXML
    private void openAssignmentReport(ActionEvent event) {
        loadPage("AssignmentReport.fxml");
    }

    @FXML
    private void openOutstandingReport(ActionEvent event) {
        loadPage("OutstandingReport.fxml");
    }

    /* ================= USERS ================= */

    @FXML
    private void openUsers(ActionEvent event) {
        loadPage("Users.fxml");
    }

    /* ================= LOGOUT ================= */

    @FXML
    private void openLogout(ActionEvent event) {

        System.out.println("Logging out...");

        // Later you can load Login.fxml here

    }

}