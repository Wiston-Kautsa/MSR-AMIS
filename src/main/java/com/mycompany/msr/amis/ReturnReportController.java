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

public class ReturnReportController implements Initializable {

    @FXML private ComboBox<String> cmbPerson;
    @FXML private ComboBox<String> cmbCondition;

    @FXML private TableView<ReturnRecord> tableReturns;

    @FXML private TableColumn<ReturnRecord, String> colAssetCode;
    @FXML private TableColumn<ReturnRecord, String> colSerialNumber;
    @FXML private TableColumn<ReturnRecord, String> colEquipmentName;
    @FXML private TableColumn<ReturnRecord, String> colCategory;
    @FXML private TableColumn<ReturnRecord, String> colSource;
    @FXML private TableColumn<ReturnRecord, String> colDateTaken;
    @FXML private TableColumn<ReturnRecord, String> colReason;
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
        colSerialNumber.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        colEquipmentName.setCellValueFactory(new PropertyValueFactory<>("equipmentName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colDateTaken.setCellValueFactory(new PropertyValueFactory<>("dateTaken"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("assignmentReason"));
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
                "SELECT r.asset_code, e.serial_number, e.name, e.category, e.source, d.date AS date_taken, " +
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
                "SELECT r.asset_code, e.serial_number, e.name, e.category, e.source, d.date AS date_taken, " +
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
        showAlert("Refresh", "Return report refreshed successfully.");
    }

    @FXML
    private void handleExport(ActionEvent event) {
        if (data.isEmpty()) {
            showAlert("No Data", "No return data to export.");
            return;
        }

        try {
            File file = FileLocationHelper.fileInDownloads("return_report.csv");
            OperationFeedbackHelper.showInfo(
                    "Export Starting",
                    "Preparing return report export.\n\nRows to export: " + data.size()
            );

            try (FileWriter writer = new FileWriter(file)) {
                writer.append("Asset Code,IMEI/Serial Number,Equipment Name,Category,Source,Date Taken,Reason,Returned By,Phone,NID,Return Condition,Remarks,Date Returned\n");

                for (ReturnRecord record : data) {
                    writer.append(csvSafe(record.getAssetCode())).append(",")
                            .append(csvSafe(record.getSerialNumber())).append(",")
                            .append(csvSafe(record.getEquipmentName())).append(",")
                            .append(csvSafe(record.getCategory())).append(",")
                            .append(csvSafe(record.getSource())).append(",")
                            .append(csvSafe(record.getDateTaken())).append(",")
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
                    "Return report exported successfully to:\n" + file.getAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError(
                    "Export Failed",
                    "Return report export failed:\n" + e.getMessage()
            );
        }
    }

    private ReturnRecord mapRecord(ResultSet rs) throws Exception {
        return new ReturnRecord(
                rs.getString("asset_code"),
                rs.getString("serial_number"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("source"),
                rs.getString("date_taken"),
                null,
                null,
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
        MenuItem refresh = new MenuItem("Refresh Return Report");
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
