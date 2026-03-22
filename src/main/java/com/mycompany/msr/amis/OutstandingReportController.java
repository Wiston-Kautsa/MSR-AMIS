package com.mycompany.msr.amis;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.io.File;
import java.io.FileWriter;

public class OutstandingReportController implements Initializable {

    @FXML
    private ComboBox<String> cmbPerson;

    @FXML
    private TableView<Distribution> tableOutstanding;

    @FXML
    private TableColumn<Distribution, String> colAssetCode;
    @FXML
    private TableColumn<Distribution, String> colAssignedTo;
    @FXML
    private TableColumn<Distribution, String> colPhone;
    @FXML
    private TableColumn<Distribution, String> colNID;

    // ✅ FIXED: String → Integer
    @FXML
    private TableColumn<Distribution, Integer> colAssignmentId;

    @FXML
    private TableColumn<Distribution, String> colDate;
    @FXML
    private TableColumn<Distribution, String> colStatus;

    private ObservableList<Distribution> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        colAssetCode.setCellValueFactory(new PropertyValueFactory<>("assetCode"));
        colAssignedTo.setCellValueFactory(new PropertyValueFactory<>("assignedTo"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colNID.setCellValueFactory(new PropertyValueFactory<>("nid"));

        // ✅ FIXED type consistency
        colAssignmentId.setCellValueFactory(new PropertyValueFactory<>("assignmentId"));

        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadData();
    }

    // ================= LOAD DATA =================
    private void loadData() {

        data.clear();

        String sql = "SELECT * FROM distribution WHERE returned = 0";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                Distribution d = new Distribution(
                        rs.getString("asset_code"),
                        "",
                        rs.getString("assigned_to"),
                        rs.getString("phone"),
                        rs.getString("nid")
                );

                // ✅ FIXED: pass int directly
                d.setAssignmentId(rs.getInt("assignment_id"));

                d.setStatus("OUTSTANDING");

                data.add(d);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data:\n" + e.getMessage());
        }

        tableOutstanding.setItems(data);
    }

    // ================= FILTER =================
    @FXML
    private void handleFilter(ActionEvent event) {

        String person = cmbPerson.getValue();

        data.clear();

        String sql = "SELECT * FROM distribution WHERE returned = 0";

        if (person != null && !person.isEmpty()) {
            sql += " AND LOWER(assigned_to) LIKE LOWER(?)";
        }

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (person != null && !person.isEmpty()) {
                ps.setString(1, "%" + person + "%");
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Distribution d = new Distribution(
                        rs.getString("asset_code"),
                        "",
                        rs.getString("assigned_to"),
                        rs.getString("phone"),
                        rs.getString("nid")
                );

                // ✅ FIXED
                d.setAssignmentId(rs.getInt("assignment_id"));

                d.setStatus("OUTSTANDING");

                data.add(d);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Filter failed:\n" + e.getMessage());
        }

        tableOutstanding.setItems(data);
    }

    // ================= REFRESH =================
    @FXML
    private void handleRefresh(ActionEvent event) {

        cmbPerson.setValue(null);
        loadData();

        showAlert("Refresh", "Outstanding data refreshed.");
    }

    // ================= EXPORT =================
    @FXML
    private void handleExport(ActionEvent event) {

        if (data.isEmpty()) {
            showAlert("No Data", "No outstanding data to export.");
            return;
        }

        try {
            String downloads = System.getProperty("user.home") + "/Downloads";
            File folder = new File(downloads, "MSR-AMIS");

            if (!folder.exists()) folder.mkdirs();

            File file = new File(folder, "outstanding_report.csv");

            FileWriter writer = new FileWriter(file);

            writer.append("Asset Code,Assigned To,Phone,NID,Assignment ID,Date,Status\n");

            for (Distribution d : data) {
                writer.append(d.getAssetCode()).append(",")
                      .append(d.getAssignedTo()).append(",")
                      .append(d.getPhone()).append(",")
                      .append(d.getNid()).append(",")
                      .append(String.valueOf(d.getAssignmentId())).append(",") // safe
                      .append(d.getDate()).append(",")
                      .append(d.getStatus()).append("\n");
            }

            writer.close();

            showAlert("Success",
                    "Export completed successfully.\nSaved to:\n" +
                    file.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Export failed:\n" + e.getMessage());
        }
    }

    // ================= ALERT =================
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}