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

public class AssignmentReportController implements Initializable {

    @FXML private ComboBox<String> cmbOfficer;
    @FXML private ComboBox<String> cmbEquipmentType;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private TableView<Assignment> tableAssignments;

    @FXML private TableColumn<Assignment, String> colAssignmentName;
    @FXML private TableColumn<Assignment, String> colOfficer;
    @FXML private TableColumn<Assignment, String> colPhone;
    @FXML private TableColumn<Assignment, String> colNID;
    @FXML private TableColumn<Assignment, String> colEquipment;
    @FXML private TableColumn<Assignment, String> colCategory;
    @FXML private TableColumn<Assignment, String> colStatus;
    @FXML private TableColumn<Assignment, String> colDate;

    private ObservableList<Assignment> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        cmbStatus.getItems().addAll("ACTIVE");

        colAssignmentName.setCellValueFactory(new PropertyValueFactory<>("assignmentName"));
        colOfficer.setCellValueFactory(new PropertyValueFactory<>("officerName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colNID.setCellValueFactory(new PropertyValueFactory<>("nid"));
        colEquipment.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        loadData();
    }

    // ================= LOAD DATA =================
    private void loadData() {

        data.clear();

        String sql = "SELECT person, department, equipment_type, date FROM assignments";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                data.add(new Assignment(
                        rs.getString("person"),          // assignmentName
                        rs.getString("person"),          // officerName
                        rs.getString("department"),      // phone
                        rs.getString("department"),      // nid
                        rs.getString("equipment_type"),  // equipment
                        rs.getString("equipment_type"),  // category
                        "ACTIVE",                        // status
                        rs.getString("date")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data:\n" + e.getMessage());
        }

        tableAssignments.setItems(data);
    }

    // ================= FILTER =================
    @FXML
    private void handleFilter(ActionEvent event) {

        String officer = cmbOfficer.getValue();
        String type = cmbEquipmentType.getValue();

        data.clear();

        String sql = "SELECT person, department, equipment_type, date FROM assignments WHERE 1=1";

        if (officer != null) sql += " AND LOWER(person) LIKE LOWER(?)";
        if (type != null) sql += " AND LOWER(equipment_type) LIKE LOWER(?)";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;

            if (officer != null) ps.setString(index++, "%" + officer + "%");
            if (type != null) ps.setString(index++, "%" + type + "%");

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                data.add(new Assignment(
                        rs.getString("person"),
                        rs.getString("person"),
                        rs.getString("department"),
                        rs.getString("department"),
                        rs.getString("equipment_type"),
                        rs.getString("equipment_type"),
                        "ACTIVE",
                        rs.getString("date")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Filter failed:\n" + e.getMessage());
        }

        tableAssignments.setItems(data);
    }

    // ================= REFRESH =================
    @FXML
    private void handleRefresh(ActionEvent event) {

        cmbOfficer.setValue(null);
        cmbEquipmentType.setValue(null);
        cmbStatus.setValue(null);

        loadData();
    }

    // ================= EXPORT =================
    @FXML
    private void handleExport(ActionEvent event) {

        if (data.isEmpty()) {
            showAlert("No Data", "No assignment data to export.");
            return;
        }

        try {
            String downloads = System.getProperty("user.home") + "/Downloads";
            File folder = new File(downloads, "MSR-AMIS");

            if (!folder.exists()) folder.mkdirs();

            File file = new File(folder, "assignment_report.csv");

            FileWriter writer = new FileWriter(file);

            writer.append("Assignment,Officer,Phone,NID,Equipment,Category,Status,Date\n");

            for (Assignment a : data) {
                writer.append(a.getAssignmentName()).append(",")
                      .append(a.getOfficerName()).append(",")
                      .append(a.getPhone()).append(",")
                      .append(a.getNid()).append(",")
                      .append(a.getEquipmentName()).append(",")
                      .append(a.getCategory()).append(",")
                      .append(a.getStatus()).append(",")
                      .append(a.getDate()).append("\n");
            }

            writer.close();

            showAlert("Success",
                    "Export completed successfully.\n\nSaved to:\n" +
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