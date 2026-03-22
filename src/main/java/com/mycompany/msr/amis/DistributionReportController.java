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

public class DistributionReportController implements Initializable {

    @FXML
    private ComboBox<String> cmbPerson;
    @FXML
    private ComboBox<String> cmbStatus;

    @FXML
    private TableView<Distribution> tableDistribution;

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
    private TableColumn<Distribution, String> colStatus;
    @FXML
    private TableColumn<Distribution, String> colDate;

    private ObservableList<Distribution> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        cmbStatus.getItems().addAll("ACTIVE", "RETURNED");

        colAssetCode.setCellValueFactory(new PropertyValueFactory<>("assetCode"));
        colAssignedTo.setCellValueFactory(new PropertyValueFactory<>("assignedTo"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colNID.setCellValueFactory(new PropertyValueFactory<>("nid"));

        // ✅ FIXED
        colAssignmentId.setCellValueFactory(new PropertyValueFactory<>("assignmentId"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        loadData();
    }

    // ================= LOAD DATA =================
    private void loadData() {

        data.clear();

        String sql = "SELECT * FROM distribution";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                String status = rs.getInt("returned") == 0 ? "ACTIVE" : "RETURNED";

                Distribution d = new Distribution(
                        rs.getString("asset_code"),
                        "",
                        rs.getString("assigned_to"),
                        rs.getString("phone"),
                        rs.getString("nid")
                );

                // ✅ FIXED
                d.setAssignmentId(rs.getInt("assignment_id"));

                d.setStatus(status);

                data.add(d);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data:\n" + e.getMessage());
        }

        tableDistribution.setItems(data);
    }

    // ================= FILTER =================
    @FXML
    private void handleFilter(ActionEvent event) {

        String person = cmbPerson.getValue();
        String status = cmbStatus.getValue();

        data.clear();

        String sql = "SELECT * FROM distribution WHERE 1=1";

        if (person != null && !person.isEmpty()) {
            sql += " AND LOWER(assigned_to) LIKE LOWER(?)";
        }

        if (status != null && !status.isEmpty()) {
            sql += " AND returned=?";
        }

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;

            if (person != null && !person.isEmpty()) {
                ps.setString(index++, "%" + person + "%");
            }

            if (status != null && !status.isEmpty()) {
                ps.setInt(index++, status.equals("ACTIVE") ? 0 : 1);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                String stat = rs.getInt("returned") == 0 ? "ACTIVE" : "RETURNED";

                Distribution d = new Distribution(
                        rs.getString("asset_code"),
                        "",
                        rs.getString("assigned_to"),
                        rs.getString("phone"),
                        rs.getString("nid")
                );

                // ✅ FIXED
                d.setAssignmentId(rs.getInt("assignment_id"));

                d.setStatus(stat);

                data.add(d);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Filter failed:\n" + e.getMessage());
        }

        tableDistribution.setItems(data);
    }

    // ================= REFRESH =================
    @FXML
    private void handleRefresh(ActionEvent event) {

        cmbPerson.setValue(null);
        cmbStatus.setValue(null);

        loadData();

        showAlert("Refresh", "Data refreshed successfully.");
    }

    // ================= EXPORT =================
    @FXML
    private void handleExport(ActionEvent event) {

        if (data.isEmpty()) {
            showAlert("No Data", "No distribution data to export.");
            return;
        }

        try {
            String downloads = System.getProperty("user.home") + "/Downloads";
            File folder = new File(downloads, "MSR-AMIS");

            if (!folder.exists()) folder.mkdirs();

            File file = new File(folder, "distribution_report.csv");

            FileWriter writer = new FileWriter(file);

            writer.append("Asset Code,Assigned To,Phone,NID,Assignment ID,Status,Date\n");

            for (Distribution d : data) {
                writer.append(d.getAssetCode()).append(",")
                      .append(d.getAssignedTo()).append(",")
                      .append(d.getPhone()).append(",")
                      .append(d.getNid()).append(",")
                      .append(String.valueOf(d.getAssignmentId())).append(",") // safe
                      .append(d.getStatus()).append(",")
                      .append(d.getDate()).append("\n");
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