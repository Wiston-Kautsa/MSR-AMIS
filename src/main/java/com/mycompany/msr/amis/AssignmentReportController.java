package com.mycompany.msr.amis;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AssignmentReportController implements Initializable {

    @FXML private ComboBox<String> cmbPerson;
    @FXML private ComboBox<String> cmbEquipmentType;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private TableView<Assignment> tableAssignments;

    @FXML private TableColumn<Assignment, String> colPerson;
    @FXML private TableColumn<Assignment, String> colDepartment;
    @FXML private TableColumn<Assignment, String> colEquipment;
    @FXML private TableColumn<Assignment, String> colReason;
    @FXML private TableColumn<Assignment, Integer> colQuantity;
    @FXML private TableColumn<Assignment, String> colStatus;
    @FXML private TableColumn<Assignment, String> colDate;

    private final ObservableList<Assignment> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        cmbStatus.getItems().addAll("PENDING", "PARTIAL", "ENROLLED");

        colPerson.setCellValueFactory(new PropertyValueFactory<>("person"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colEquipment.setCellValueFactory(new PropertyValueFactory<>("equipmentType"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(getAssignmentStatus(cell.getValue())));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        setupContextMenu();
        loadData();
        loadFilters();
    }

    // ================= LOAD DATA =================
    private void loadData() {
        data.clear();
        try {
            data.addAll(DatabaseHandler.getAssignments());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data:\n" + e.getMessage());
        }

        tableAssignments.setItems(data);
    }

    private void loadFilters() {
        cmbPerson.getItems().clear();
        cmbEquipmentType.getItems().clear();

        try {
            for (Assignment assignment : DatabaseHandler.getAssignments()) {
                addIfMissing(cmbPerson, assignment.getPerson());
                addIfMissing(cmbEquipmentType, assignment.getEquipmentType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= FILTER =================
    @FXML
    private void handleFilter(ActionEvent event) {
        String person = cmbPerson.getValue();
        String type = cmbEquipmentType.getValue();
        String status = cmbStatus.getValue();

        data.clear();

        try {
            for (Assignment assignment : DatabaseHandler.getAssignments()) {
                if (!matchesFilter(assignment.getPerson(), person)) {
                    continue;
                }
                if (!matchesFilter(assignment.getEquipmentType(), type)) {
                    continue;
                }
                if (!matchesFilter(getAssignmentStatus(assignment), status)) {
                    continue;
                }
                data.add(assignment);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Filter failed:\n" + e.getMessage());
        }

        tableAssignments.setItems(data);
    }

    // ================= REFRESH =================
    private void handleRefresh() {
        cmbPerson.setValue(null);
        cmbEquipmentType.setValue(null);
        cmbStatus.setValue(null);
        loadData();
        loadFilters();
        showAlert("Refresh", "Assignment report refreshed successfully.");
    }

    // ================= EXPORT =================
    @FXML
    private void handleExport(ActionEvent event) {

        if (data.isEmpty()) {
            showAlert("No Data", "No assignment data to export.");
            return;
        }

        File file = FileLocationHelper.fileInDownloads("assignment_report.csv");
        OperationFeedbackHelper.showInfo(
                "Export Starting",
                "Preparing assignment report export.\n\nRows to export: " + data.size()
        );

        try (FileWriter writer = new FileWriter(file)) {

            writer.append("Responsible Person,Department,Equipment,Reason,Quantity,Status,Date\n");

            for (Assignment a : data) {
                writer.append(csvSafe(a.getPerson())).append(",")
                      .append(csvSafe(a.getDepartment())).append(",")
                      .append(csvSafe(a.getEquipmentType())).append(",")
                      .append(csvSafe(a.getReason())).append(",")
                      .append(String.valueOf(a.getQuantity())).append(",")
                      .append(csvSafe(getAssignmentStatus(a))).append(",")
                      .append(csvSafe(a.getDate())).append("\n");
            }

            OperationFeedbackHelper.showInfo(
                    "Export Complete",
                    "Assignment report exported successfully to:\n" + file.getAbsolutePath()
            );

        } catch (IOException e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError(
                    "Export Failed",
                    "Assignment report export failed:\n" + e.getMessage()
            );
        }
    }

    private String csvSafe(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getAssignmentStatus(Assignment assignment) {
        int distributed = DatabaseHandler.getDistributedCountForAssignment(assignment.getId());
        if (distributed <= 0) {
            return "PENDING";
        }
        if (distributed < assignment.getQuantity()) {
            return "PARTIAL";
        }
        return "ENROLLED";
    }

    private boolean matchesFilter(String value, String filter) {
        return filter == null || filter.isBlank() || (value != null && value.equalsIgnoreCase(filter));
    }

    private void addIfMissing(ComboBox<String> comboBox, String value) {
        if (value == null || value.isBlank() || comboBox.getItems().contains(value)) {
            return;
        }
        comboBox.getItems().add(value);
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem refresh = new MenuItem("Refresh Assignment Report");
        refresh.setOnAction(event -> handleRefresh());
        menu.getItems().add(refresh);
        tableAssignments.setContextMenu(menu);
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
