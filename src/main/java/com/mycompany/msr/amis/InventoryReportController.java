package com.mycompany.msr.amis;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

public class InventoryReportController implements Initializable {

    @FXML
    private ComboBox<String> cmbCategory;
    @FXML
    private ComboBox<String> cmbStatus;

    @FXML
    private TableView<Equipment> tableInventory;

    @FXML
    private TableColumn<Equipment, String> colAssetCode;
    @FXML
    private TableColumn<Equipment, String> colName;
    @FXML
    private TableColumn<Equipment, String> colCategory;
    @FXML
    private TableColumn<Equipment, String> colSerial;
    @FXML
    private TableColumn<Equipment, String> colCondition;
    @FXML
    private TableColumn<Equipment, String> colStatus;
    @FXML
    private TableColumn<Equipment, String> colSource;
    @FXML
    private TableColumn<Equipment, String> colDate;

    private ObservableList<Equipment> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        cmbCategory.getItems().addAll(
                "Tablet", "Laptop", "Desktop", "Printer", "Router", "Other"
        );

        cmbStatus.getItems().addAll(
                "AVAILABLE", "ASSIGNED", "MAINTENANCE", "RETIRED"
        );

        colAssetCode.setCellValueFactory(new PropertyValueFactory<>("assetCode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colSerial.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        colCondition.setCellValueFactory(new PropertyValueFactory<>("condition"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("entryDate"));

        setupContextMenu();
        loadData();
    }

    // ================= LOAD DATA =================
    private void loadData() {

        data.clear();

        String sql = "SELECT * FROM equipment";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                data.add(new Equipment(
                        rs.getInt("id"),
                        rs.getString("asset_code"),
                        rs.getString("serial_number"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("condition"),
                        rs.getString("source"),
                        rs.getString("entry_date"),
                        rs.getString("status")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data:\n" + e.getMessage());
        }

        tableInventory.setItems(data);
    }

    // ================= FILTER =================
    @FXML
    private void handleFilter(ActionEvent event) {

        String category = cmbCategory.getValue();
        String status = cmbStatus.getValue();

        data.clear();

        String sql = "SELECT * FROM equipment WHERE 1=1";

        // Case-insensitive filtering
        if (category != null) sql += " AND LOWER(category)=LOWER(?)";
        if (status != null) sql += " AND LOWER(status)=LOWER(?)";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;

            if (category != null) ps.setString(index++, category);
            if (status != null) ps.setString(index++, status);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                data.add(new Equipment(
                        rs.getInt("id"),
                        rs.getString("asset_code"),
                        rs.getString("serial_number"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("condition"),
                        rs.getString("source"),
                        rs.getString("entry_date"),
                        rs.getString("status")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Filter failed:\n" + e.getMessage());
        }

        tableInventory.setItems(data);
    }

    // ================= REFRESH =================
    private void handleRefresh() {

        cmbCategory.setValue(null);
        cmbStatus.setValue(null);

        loadData();
        showAlert("Refresh", "Inventory report refreshed successfully.");
    }

    // ================= EXPORT =================
    @FXML
    private void handleExport(ActionEvent event) {

        if (data.isEmpty()) {
            showAlert("No Data", "There is no data to export.");
            return;
        }

        File file = FileLocationHelper.fileInDownloads("inventory_report.csv");
        OperationFeedbackHelper.showInfo(
                "Export Starting",
                "Preparing inventory report export.\n\nRows to export: " + data.size()
        );

        try (FileWriter writer = new FileWriter(file)) {

            writer.append("System Serial No.,Name,Category,IMEI/Serial Number,Condition,Status,Source,Date\n");

            for (Equipment e : data) {
                writer.append(csvSafe(e.getAssetCode())).append(",")
                      .append(csvSafe(e.getName())).append(",")
                      .append(csvSafe(e.getCategory())).append(",")
                      .append(csvSafe(e.getSerialNumber())).append(",")
                      .append(csvSafe(e.getCondition())).append(",")
                      .append(csvSafe(e.getStatus())).append(",")
                      .append(csvSafe(e.getSource())).append(",")
                      .append(csvSafe(e.getEntryDate())).append("\n");
            }

            OperationFeedbackHelper.showInfo(
                    "Export Complete",
                    "Inventory report exported successfully to:\n" + file.getAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError(
                    "Export Failed",
                    "Inventory report export failed:\n" + e.getMessage()
            );
        }
    }

    // ================= CSV SAFETY =================
    private String csvSafe(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem refresh = new MenuItem("Refresh Inventory Report");
        refresh.setOnAction(event -> handleRefresh());
        menu.getItems().add(refresh);
        tableInventory.setContextMenu(menu);
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
