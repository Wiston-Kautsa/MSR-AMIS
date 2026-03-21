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

import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.io.FileWriter;
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
    @FXML
    private void handleRefresh(ActionEvent event) {

        cmbCategory.setValue(null);
        cmbStatus.setValue(null);

        loadData();
    }

    // ================= EXPORT =================
    @FXML
    private void handleExport(ActionEvent event) {

        if (data.isEmpty()) {
            showAlert("No Data", "There is no data to export.");
            return;
        }

        try {
            // Get Downloads folder
            String downloadsPath = System.getProperty("user.home") + "/Downloads";

            // Create MSR-AMIS folder
            File folder = new File(downloadsPath, "MSR-AMIS");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // File name (you can add timestamp later if needed)
            File file = new File(folder, "inventory_report.csv");

            FileWriter writer = new FileWriter(file);

            writer.append("Asset Code,Name,Category,Serial,Condition,Status,Source,Date\n");

            for (Equipment e : data) {
                writer.append(e.getAssetCode()).append(",")
                      .append(e.getName()).append(",")
                      .append(e.getCategory()).append(",")
                      .append(e.getSerialNumber()).append(",")
                      .append(e.getCondition()).append(",")
                      .append(e.getStatus()).append(",")
                      .append(e.getSource()).append(",")
                      .append(e.getEntryDate()).append("\n");
            }

            writer.flush();
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