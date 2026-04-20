package com.mycompany.msr.amis;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class AssetHistoryController implements Initializable {

    @FXML private TextField txtAssetCode;
    @FXML private Label lblAssetCode;
    @FXML private Label lblSerialNumber;
    @FXML private Label lblEquipmentName;
    @FXML private Label lblCategory;
    @FXML private Label lblEntryDate;
    @FXML private Label lblCurrentStatus;

    @FXML private TableView<AssetHistoryRecord> tableHistory;
    @FXML private TableColumn<AssetHistoryRecord, String> colActivityDate;
    @FXML private TableColumn<AssetHistoryRecord, String> colEventType;
    @FXML private TableColumn<AssetHistoryRecord, String> colActor;
    @FXML private TableColumn<AssetHistoryRecord, String> colAffectedPerson;
    @FXML private TableColumn<AssetHistoryRecord, String> colDetails;
    @FXML private TableColumn<AssetHistoryRecord, String> colStatus;

    private final ObservableList<AssetHistoryRecord> data = FXCollections.observableArrayList();
    private String currentAssetCode = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colActivityDate.setCellValueFactory(new PropertyValueFactory<>("activityDate"));
        colEventType.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        colActor.setCellValueFactory(new PropertyValueFactory<>("actor"));
        colAffectedPerson.setCellValueFactory(new PropertyValueFactory<>("affectedPerson"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableHistory.setItems(data);
        setupContextMenu();
        clearSummary();
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String assetCode = txtAssetCode.getText() == null ? "" : txtAssetCode.getText().trim();
        if (assetCode.isBlank()) {
            showAlert("Search", "Enter an asset code.");
            return;
        }

        currentAssetCode = assetCode;
        if (!loadSummary(assetCode)) {
            data.clear();
            showAlert("Not Found", "No asset was found for code: " + assetCode);
            return;
        }

        loadHistory(assetCode);
    }

    @FXML
    private void handleClear(ActionEvent event) {
        currentAssetCode = "";
        txtAssetCode.clear();
        data.clear();
        clearSummary();
    }

    @FXML
    private void handleExport(ActionEvent event) {
        if (currentAssetCode.isBlank() || data.isEmpty()) {
            showAlert("No Data", "Search for an asset with history before exporting.");
            return;
        }

        try {
            File file = FileLocationHelper.fileInDownloads("asset_history_" + currentAssetCode + ".csv");
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("Asset Code,Activity Date,Event Type,Actor,Affected Person,Details,Status\n");
                for (AssetHistoryRecord record : data) {
                    writer.append(csvSafe(currentAssetCode)).append(",")
                            .append(csvSafe(record.getActivityDate())).append(",")
                            .append(csvSafe(record.getEventType())).append(",")
                            .append(csvSafe(record.getActor())).append(",")
                            .append(csvSafe(record.getAffectedPerson())).append(",")
                            .append(csvSafe(record.getDetails())).append(",")
                            .append(csvSafe(record.getStatus())).append("\n");
                }
            }

            OperationFeedbackHelper.showInfo(
                    "Export Complete",
                    "Asset history exported successfully to:\n" + file.getAbsolutePath()
            );
        } catch (Exception e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError("Export Failed", "Asset history export failed:\n" + e.getMessage());
        }
    }

    private boolean loadSummary(String assetCode) {
        String sql =
                "SELECT asset_code, serial_number, name, category, entry_date, status " +
                "FROM equipment WHERE LOWER(TRIM(asset_code)) = LOWER(TRIM(?))";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, assetCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    clearSummary();
                    return false;
                }

                lblAssetCode.setText(rs.getString("asset_code"));
                lblSerialNumber.setText(valueOrDash(rs.getString("serial_number")));
                lblEquipmentName.setText(valueOrDash(rs.getString("name")));
                lblCategory.setText(valueOrDash(rs.getString("category")));
                lblEntryDate.setText(valueOrDash(rs.getString("entry_date")));
                lblCurrentStatus.setText(valueOrDash(rs.getString("status")));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load asset summary:\n" + e.getMessage());
            clearSummary();
            return false;
        }
    }

    private void loadHistory(String assetCode) {
        data.clear();

        String sql =
                "SELECT activity_date, event_type, actor, affected_person, details, status " +
                "FROM (" +
                "    SELECT e.entry_date AS activity_date, " +
                "           'REGISTERED' AS event_type, " +
                "           COALESCE(e.source, 'System') AS actor, " +
                "           '' AS affected_person, " +
                "           'Asset added: ' || COALESCE(e.name, '') || " +
                "           CASE WHEN COALESCE(TRIM(e.category), '') = '' THEN '' ELSE ' | Category: ' || TRIM(e.category) END || " +
                "           CASE WHEN COALESCE(TRIM(e.condition), '') = '' THEN '' ELSE ' | Condition: ' || TRIM(e.condition) END || " +
                "           CASE WHEN COALESCE(TRIM(e.serial_number), '') = '' THEN '' ELSE ' | Serial: ' || TRIM(e.serial_number) END AS details, " +
                "           COALESCE(e.status, '') AS status, " +
                "           0 AS event_order, " +
                "           e.id AS record_id " +
                "    FROM equipment e " +
                "    WHERE LOWER(TRIM(e.asset_code)) = LOWER(TRIM(?)) " +
                "    UNION ALL " +
                "    SELECT d.date AS activity_date, " +
                "           'ISSUED' AS event_type, " +
                "           COALESCE(a.person, '') AS actor, " +
                "           COALESCE(d.assigned_to, '') AS affected_person, " +
                "           CASE WHEN COALESCE(TRIM(a.reason), '') = '' THEN 'Asset issued' ELSE 'Reason: ' || TRIM(a.reason) END || " +
                "           CASE WHEN COALESCE(TRIM(a.department), '') = '' THEN '' ELSE ' | Department: ' || TRIM(a.department) END || " +
                "           CASE WHEN COALESCE(TRIM(a.equipment_type), '') = '' THEN '' ELSE ' | Equipment type: ' || TRIM(a.equipment_type) END || " +
                "           CASE WHEN COALESCE(TRIM(d.phone), '') = '' THEN '' ELSE ' | Phone: ' || TRIM(d.phone) END || " +
                "           CASE WHEN COALESCE(TRIM(d.nid), '') = '' THEN '' ELSE ' | NID: ' || TRIM(d.nid) END AS details, " +
                "           CASE WHEN d.returned = 1 THEN 'RETURNED' ELSE 'ASSIGNED' END AS status, " +
                "           1 AS event_order, " +
                "           d.id AS record_id " +
                "    FROM distribution d " +
                "    LEFT JOIN assignments a ON a.id = d.assignment_id " +
                "    WHERE LOWER(TRIM(d.asset_code)) = LOWER(TRIM(?)) " +
                "    UNION ALL " +
                "    SELECT r.return_date AS activity_date, " +
                "           'RETURNED' AS event_type, " +
                "           COALESCE(r.returned_by, '') AS actor, " +
                "           '' AS affected_person, " +
                "           CASE WHEN COALESCE(TRIM(r.condition), '') = '' THEN 'Asset returned' ELSE 'Condition: ' || TRIM(r.condition) END || " +
                "           CASE WHEN COALESCE(TRIM(r.remarks), '') = '' THEN '' ELSE ' | Remarks: ' || TRIM(r.remarks) END || " +
                "           CASE WHEN COALESCE(TRIM(r.phone), '') = '' THEN '' ELSE ' | Phone: ' || TRIM(r.phone) END || " +
                "           CASE WHEN COALESCE(TRIM(r.nid), '') = '' THEN '' ELSE ' | NID: ' || TRIM(r.nid) END AS details, " +
                "           'AVAILABLE' AS status, " +
                "           2 AS event_order, " +
                "           r.id AS record_id " +
                "    FROM returns r " +
                "    WHERE LOWER(TRIM(r.asset_code)) = LOWER(TRIM(?)) " +
                ") history " +
                "ORDER BY COALESCE(activity_date, '') DESC, event_order DESC, record_id DESC";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, assetCode);
            ps.setString(2, assetCode);
            ps.setString(3, assetCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(new AssetHistoryRecord(
                            rs.getString("activity_date"),
                            rs.getString("event_type"),
                            rs.getString("actor"),
                            rs.getString("affected_person"),
                            rs.getString("details"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load asset history:\n" + e.getMessage());
        }
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem refresh = new MenuItem("Refresh Asset History");
        refresh.setOnAction(event -> {
            if (!currentAssetCode.isBlank()) {
                loadSummary(currentAssetCode);
                loadHistory(currentAssetCode);
            }
        });
        menu.getItems().add(refresh);
        tableHistory.setContextMenu(menu);
    }

    private void clearSummary() {
        lblAssetCode.setText("-");
        lblSerialNumber.setText("-");
        lblEquipmentName.setText("-");
        lblCategory.setText("-");
        lblEntryDate.setText("-");
        lblCurrentStatus.setText("-");
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
