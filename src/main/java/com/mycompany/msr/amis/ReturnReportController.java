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

public class ReturnReportController implements Initializable {

    @FXML
    private ComboBox<String> cmbPerson;
    @FXML
    private ComboBox<String> cmbCondition;

    @FXML
    private TableView<Return> tableReturns;

    @FXML
    private TableColumn<Return, String> colAssetCode;
    @FXML
    private TableColumn<Return, String> colReturnedBy;
    @FXML
    private TableColumn<Return, String> colPhone;
    @FXML
    private TableColumn<Return, String> colNID;
    @FXML
    private TableColumn<Return, String> colCondition;
    @FXML
    private TableColumn<Return, String> colDate;

    private ObservableList<Return> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Load condition options
        cmbCondition.getItems().addAll("GOOD", "DAMAGED", "FAULTY");

        // Table bindings
        colAssetCode.setCellValueFactory(new PropertyValueFactory<>("assetCode"));
        colReturnedBy.setCellValueFactory(new PropertyValueFactory<>("returnedBy"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colNID.setCellValueFactory(new PropertyValueFactory<>("nid"));
        colCondition.setCellValueFactory(new PropertyValueFactory<>("condition"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        loadData();
    }

    // ================= LOAD DATA =================
    private void loadData() {

        data.clear();

        String sql = "SELECT * FROM returns";

        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                data.add(new Return(
                        rs.getString("asset_code"),
                        rs.getString("returned_by"),
                        rs.getString("phone"),
                        rs.getString("nid"),
                        rs.getString("condition"),
                        rs.getString("return_date")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data:\n" + e.getMessage());
        }

        tableReturns.setItems(data);
    }

    // ================= FILTER =================
    @FXML
    private void handleFilter(ActionEvent event) {

        String person = cmbPerson.getValue();
        String condition = cmbCondition.getValue();

        data.clear();

        String sql = "SELECT * FROM returns WHERE 1=1";

        if (person != null && !person.isEmpty()) {
            sql += " AND LOWER(returned_by) LIKE LOWER(?)";
        }

        if (condition != null && !condition.isEmpty()) {
            sql += " AND LOWER(condition)=LOWER(?)";
        }

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;

            if (person != null && !person.isEmpty()) {
                ps.setString(index++, "%" + person + "%");
            }

            if (condition != null && !condition.isEmpty()) {
                ps.setString(index++, condition);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                data.add(new Return(
                        rs.getString("asset_code"),
                        rs.getString("returned_by"),
                        rs.getString("phone"),
                        rs.getString("nid"),
                        rs.getString("condition"),
                        rs.getString("return_date")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Filter failed:\n" + e.getMessage());
        }

        tableReturns.setItems(data);
    }

    // ================= REFRESH =================
    @FXML
    private void handleRefresh(ActionEvent event) {

        cmbPerson.setValue(null);
        cmbCondition.setValue(null);

        loadData();

        showAlert("Refresh", "Data refreshed successfully.");
    }

    // ================= EXPORT =================
    @FXML
    private void handleExport(ActionEvent event) {

        if (data.isEmpty()) {
            showAlert("No Data", "No return data to export.");
            return;
        }

        try {
            String downloads = System.getProperty("user.home") + "/Downloads";
            File folder = new File(downloads, "MSR-AMIS");

            if (!folder.exists()) folder.mkdirs();

            File file = new File(folder, "return_report.csv");

            FileWriter writer = new FileWriter(file);

            writer.append("Asset Code,Returned By,Phone,NID,Condition,Date\n");

            for (Return r : data) {
                writer.append(r.getAssetCode()).append(",")
                      .append(r.getReturnedBy()).append(",")
                      .append(r.getPhone()).append(",")
                      .append(r.getNid()).append(",")
                      .append(r.getCondition()).append(",")
                      .append(r.getDate()).append("\n");
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