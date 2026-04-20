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
import java.util.ResourceBundle;

public class ReturnEquipmentListController implements Initializable {

    @FXML private ComboBox<String> cmbPerson;
    @FXML private ComboBox<String> cmbCondition;

    @FXML private TableView<ReturnRecord> tableReturns;

    @FXML private TableColumn<ReturnRecord, String> colAssetCode;
    @FXML private TableColumn<ReturnRecord, String> colResponsibleOfficer;
    @FXML private TableColumn<ReturnRecord, String> colEquipmentType;
    @FXML private TableColumn<ReturnRecord, String> colAssignmentReason;
    @FXML private TableColumn<ReturnRecord, String> colReturnedBy;
    @FXML private TableColumn<ReturnRecord, String> colPhone;
    @FXML private TableColumn<ReturnRecord, String> colNID;
    @FXML private TableColumn<ReturnRecord, String> colCondition;
    @FXML private TableColumn<ReturnRecord, String> colRemarks;
    @FXML private TableColumn<ReturnRecord, String> colDate;

    private final ObservableList<ReturnRecord> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbCondition.getItems().addAll("Good", "Fair", "Damaged", "Faulty", "Lost");

        colAssetCode.setCellValueFactory(new PropertyValueFactory<>("assetCode"));
        colResponsibleOfficer.setCellValueFactory(new PropertyValueFactory<>("responsibleOfficer"));
        colEquipmentType.setCellValueFactory(new PropertyValueFactory<>("assignmentEquipmentType"));
        colAssignmentReason.setCellValueFactory(new PropertyValueFactory<>("assignmentReason"));
        colReturnedBy.setCellValueFactory(new PropertyValueFactory<>("returnedBy"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colNID.setCellValueFactory(new PropertyValueFactory<>("nid"));
        colCondition.setCellValueFactory(new PropertyValueFactory<>("returnCondition"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("returnDate"));

        setupContextMenu();
        loadData();
        loadPeople();
    }

    private void loadPeople() {
        cmbPerson.getItems().clear();

        String sql = "SELECT DISTINCT returned_by FROM returns WHERE returned_by IS NOT NULL AND TRIM(returned_by) <> ''";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                cmbPerson.getItems().add(rs.getString("returned_by"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        data.clear();

        String sql =
                "SELECT r.asset_code, a.person AS responsible_officer, " +
                "COALESCE(a.equipment_type, e.category, e.name) AS assignment_equipment_type, " +
                "a.reason AS assignment_reason, " +
                "r.returned_by, r.phone, r.nid, r.condition, r.remarks, r.return_date " +
                "FROM returns r " +
                "LEFT JOIN distribution d ON d.id = (" +
                "SELECT d2.id FROM distribution d2 WHERE d2.asset_code = r.asset_code ORDER BY d2.id DESC LIMIT 1" +
                ") " +
                "LEFT JOIN assignments a ON a.id = d.assignment_id " +
                "LEFT JOIN equipment e ON e.asset_code = r.asset_code " +
                "ORDER BY r.return_date DESC, r.id DESC";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                data.add(mapRecord(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data:\n" + e.getMessage());
        }

        tableReturns.setItems(data);
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        String person = cmbPerson.getValue();
        String condition = cmbCondition.getValue();

        data.clear();

        String sql =
                "SELECT r.asset_code, a.person AS responsible_officer, " +
                "COALESCE(a.equipment_type, e.category, e.name) AS assignment_equipment_type, " +
                "a.reason AS assignment_reason, " +
                "r.returned_by, r.phone, r.nid, r.condition, r.remarks, r.return_date " +
                "FROM returns r " +
                "LEFT JOIN distribution d ON d.id = (" +
                "SELECT d2.id FROM distribution d2 WHERE d2.asset_code = r.asset_code ORDER BY d2.id DESC LIMIT 1" +
                ") " +
                "LEFT JOIN assignments a ON a.id = d.assignment_id " +
                "LEFT JOIN equipment e ON e.asset_code = r.asset_code " +
                "WHERE 1=1";

        if (person != null && !person.isEmpty()) {
            sql += " AND LOWER(r.returned_by) LIKE LOWER(?)";
        }

        if (condition != null && !condition.isEmpty()) {
            sql += " AND LOWER(r.condition)=LOWER(?)";
        }

        sql += " ORDER BY r.return_date DESC, r.id DESC";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;

            if (person != null && !person.isEmpty()) {
                ps.setString(index++, "%" + person + "%");
            }

            if (condition != null && !condition.isEmpty()) {
                ps.setString(index, condition);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(mapRecord(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Filter failed:\n" + e.getMessage());
        }

        tableReturns.setItems(data);
    }

    private void handleRefresh() {
        cmbPerson.setValue(null);
        cmbCondition.setValue(null);
        loadData();
        showAlert("Refresh", "Return equipment list refreshed successfully.");
    }

    @FXML
    private void handleExport(ActionEvent event) {
        if (data.isEmpty()) {
            showAlert("No Data", "No return data to export.");
            return;
        }

        try {
            File file = FileLocationHelper.fileInDownloads("return_equipment_list.csv");
            OperationFeedbackHelper.showInfo(
                    "Export Starting",
                    "Preparing return equipment list export.\n\nRows to export: " + data.size()
            );

            try (FileWriter writer = new FileWriter(file)) {
                writer.append("Asset Code,Responsible Officer,Equipment Type,Assignment Reason,Returned By,Phone,NID,Return Condition,Remarks,Return Date\n");

                for (ReturnRecord record : data) {
                    writer.append(csvSafe(record.getAssetCode())).append(",")
                            .append(csvSafe(record.getResponsibleOfficer())).append(",")
                            .append(csvSafe(record.getAssignmentEquipmentType())).append(",")
                            .append(csvSafe(record.getAssignmentReason())).append(",")
                            .append(csvSafe(record.getReturnedBy())).append(",")
                            .append(csvSafe(record.getPhone())).append(",")
                            .append(csvSafe(record.getNid())).append(",")
                            .append(csvSafe(record.getReturnCondition())).append(",")
                            .append(csvSafe(record.getRemarks())).append(",")
                            .append(csvSafe(record.getReturnDate())).append("\n");
                }
            }

            OperationFeedbackHelper.showInfo(
                    "Export Complete",
                    "Return equipment list exported successfully to:\n" + file.getAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError(
                    "Export Failed",
                    "Return equipment list export failed:\n" + e.getMessage()
            );
        }
    }

    private ReturnRecord mapRecord(ResultSet rs) throws Exception {
        return new ReturnRecord(
                rs.getString("asset_code"),
                null,
                null,
                null,
                null,
                null,
                rs.getString("responsible_officer"),
                rs.getString("assignment_equipment_type"),
                rs.getString("assignment_reason"),
                rs.getString("returned_by"),
                rs.getString("phone"),
                rs.getString("nid"),
                rs.getString("condition"),
                rs.getString("remarks"),
                rs.getString("return_date")
        );
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

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem refresh = new MenuItem("Refresh Return Equipment List");
        refresh.setOnAction(event -> handleRefresh());
        menu.getItems().add(refresh);
        tableReturns.setContextMenu(menu);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
