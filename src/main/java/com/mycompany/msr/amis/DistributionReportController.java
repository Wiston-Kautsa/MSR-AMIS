package com.mycompany.msr.amis;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class DistributionReportController implements Initializable {

    @FXML private ComboBox<String> cmbPerson;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private TableView<Distribution> tableDistribution;

    @FXML private TableColumn<Distribution, String> colAssetCode;
    @FXML private TableColumn<Distribution, String> colResponsiblePerson;
    @FXML private TableColumn<Distribution, String> colAssignedTo;
    @FXML private TableColumn<Distribution, String> colPhone;
    @FXML private TableColumn<Distribution, String> colNID;
    @FXML private TableColumn<Distribution, String> colStatus;
    @FXML private TableColumn<Distribution, String> colDate;

    private final ObservableList<Distribution> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbStatus.getItems().addAll("ACTIVE", "RETURNED");

        colAssetCode.setCellValueFactory(new PropertyValueFactory<>("assetCode"));
        colResponsiblePerson.setCellValueFactory(new PropertyValueFactory<>("responsiblePerson"));
        colAssignedTo.setCellValueFactory(new PropertyValueFactory<>("assignedTo"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colNID.setCellValueFactory(new PropertyValueFactory<>("nid"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        setupContextMenu();
        loadData();
        loadPeople();
    }

    private void loadPeople() {
        cmbPerson.getItems().clear();

        String sql = "SELECT DISTINCT assigned_to FROM distribution WHERE assigned_to IS NOT NULL AND TRIM(assigned_to) <> ''";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                cmbPerson.getItems().add(rs.getString("assigned_to"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private void loadData() {
        data.clear();

        String sql =
                "SELECT d.*, a.person AS responsible_person " +
                "FROM distribution d " +
                "LEFT JOIN assignments a ON a.id = d.assignment_id";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                data.add(mapDistribution(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data:\n" + e.getMessage());
        }

        tableDistribution.setItems(data);
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        String person = cmbPerson.getValue();
        String status = cmbStatus.getValue();

        data.clear();

        String sql =
                "SELECT d.*, a.person AS responsible_person " +
                "FROM distribution d " +
                "LEFT JOIN assignments a ON a.id = d.assignment_id " +
                "WHERE 1=1";

        if (person != null && !person.isEmpty()) {
            sql += " AND LOWER(d.assigned_to) LIKE LOWER(?)";
        }

        if (status != null && !status.isEmpty()) {
            sql += " AND d.returned = ?";
        }

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;

            if (person != null && !person.isEmpty()) {
                ps.setString(index++, "%" + person + "%");
            }

            if (status != null && !status.isEmpty()) {
                ps.setInt(index, status.equals("ACTIVE") ? 0 : 1);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(mapDistribution(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Filter failed:\n" + e.getMessage());
        }

        tableDistribution.setItems(data);
    }

    private void handleRefresh() {
        cmbPerson.setValue(null);
        cmbStatus.setValue(null);
        loadData();
        showAlert("Refresh", "Data refreshed successfully.");
    }

    @FXML
    private void handleExport(ActionEvent event) {
        if (data.isEmpty()) {
            showAlert("No Data", "No distribution data to export.");
            return;
        }

        try {
            File file = FileLocationHelper.fileInDownloads("distribution_report.csv");
            OperationFeedbackHelper.showInfo(
                    "Export Starting",
                    "Preparing distribution report export.\n\nRows to export: " + data.size()
            );

            try (FileWriter writer = new FileWriter(file)) {
                writer.append("Asset Code,Responsible Person,Assigned To,Phone,NID,Status,Date\n");

                for (Distribution distribution : data) {
                    writer.append(csvSafe(distribution.getAssetCode())).append(",")
                            .append(csvSafe(distribution.getResponsiblePerson())).append(",")
                            .append(csvSafe(distribution.getAssignedTo())).append(",")
                            .append(csvSafe(distribution.getPhone())).append(",")
                            .append(csvSafe(distribution.getNid())).append(",")
                            .append(csvSafe(distribution.getStatus())).append(",")
                            .append(csvSafe(distribution.getDate())).append("\n");
                }
            }

            OperationFeedbackHelper.showInfo(
                    "Export Complete",
                    "Distribution report exported successfully to:\n" + file.getAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError(
                    "Export Failed",
                    "Distribution report export failed:\n" + e.getMessage()
            );
        }
    }

    private Distribution mapDistribution(ResultSet rs) throws Exception {
        String status = rs.getInt("returned") == 0 ? "ACTIVE" : "RETURNED";

        Distribution distribution = new Distribution(
                rs.getString("asset_code"),
                "",
                rs.getString("assigned_to"),
                rs.getString("phone"),
                rs.getString("nid")
        );

        distribution.setAssignmentId(rs.getInt("assignment_id"));
        distribution.setResponsiblePerson(rs.getString("responsible_person"));
        distribution.setDistributionDate(parseDate(rs.getString("date")));
        distribution.setStatus(status);

        return distribution;
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem refresh = new MenuItem("Refresh Distribution List");
        refresh.setOnAction(event -> handleRefresh());
        menu.getItems().add(refresh);
        tableDistribution.setContextMenu(menu);
    }

    private String csvSafe(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
