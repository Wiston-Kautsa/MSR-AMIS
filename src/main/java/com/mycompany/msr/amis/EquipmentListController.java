package com.mycompany.msr.amis;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ResourceBundle;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.binding.Bindings;

public class EquipmentListController implements Initializable {

    @FXML
    private TableView<Equipment> equipmentTable;

    @FXML
    private TableColumn<Equipment, String> colAssetCode;

    @FXML
    private TableColumn<Equipment, String> colSerial;

    @FXML
    private TableColumn<Equipment, String> colName;

    @FXML
    private TableColumn<Equipment, String> colCategory;

    @FXML
    private TableColumn<Equipment, String> colCondition;

    @FXML
    private TableColumn<Equipment, String> colSource;

    @FXML
    private TableColumn<Equipment, String> colStatus;

    @FXML
    private TableColumn<Equipment, String> colDate;

    @FXML
    private TextField txtSearch;

    private ObservableList<Equipment> equipmentList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        equipmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colAssetCode.setCellValueFactory(new PropertyValueFactory<>("assetCode"));
        colSerial.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCondition.setCellValueFactory(new PropertyValueFactory<>("condition"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("entryDate"));

        loadEquipment();
        enableRowMenu();
    }

    /* =============================
       LOAD EQUIPMENT
    ============================== */

    private void loadEquipment() {

        try (Connection conn = DatabaseHandler.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM equipment")) {

            equipmentList.clear();

            while (rs.next()) {

                equipmentList.add(new Equipment(
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

            equipmentTable.setItems(equipmentList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =============================
       CONTEXT MENU
    ============================== */

    private void enableRowMenu() {

        equipmentTable.setRowFactory(tv -> {

            TableRow<Equipment> row = new TableRow<>();

            ContextMenu menu = new ContextMenu();

            MenuItem edit = new MenuItem("Edit Equipment");
            MenuItem delete = new MenuItem("Delete Equipment");

            edit.setOnAction(e -> editEquipment(row.getItem()));
            delete.setOnAction(e -> deleteEquipment(row.getItem()));

            menu.getItems().addAll(edit, delete);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );

            return row;
        });
    }

    /* =============================
       SEARCH
    ============================== */

    @FXML
    private void searchEquipment(ActionEvent event) {

        String keyword = txtSearch.getText().toLowerCase();

        if (keyword.isEmpty()) {
            equipmentTable.setItems(equipmentList);
            return;
        }

        ObservableList<Equipment> filtered = FXCollections.observableArrayList();

        for (Equipment eq : equipmentList) {

            if (eq.getName().toLowerCase().contains(keyword)
                    || eq.getSerialNumber().toLowerCase().contains(keyword)
                    || eq.getAssetCode().toLowerCase().contains(keyword)
                    || eq.getCategory().toLowerCase().contains(keyword)) {

                filtered.add(eq);
            }
        }

        equipmentTable.setItems(filtered);
    }

    /* =============================
       EDIT EQUIPMENT
    ============================== */

    private void editEquipment(Equipment equipment) {

        if (equipment == null) {
            showMessage("Edit Equipment", "No equipment selected.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Equipment");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(equipment.getName());
        TextField serialField = new TextField(equipment.getSerialNumber());
        TextField categoryField = new TextField(equipment.getCategory());
        TextField conditionField = new TextField(equipment.getCondition());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Serial Number:"), 0, 1);
        grid.add(serialField, 1, 1);

        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryField, 1, 2);

        grid.add(new Label("Condition:"), 0, 3);
        grid.add(conditionField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {

            try (Connection conn = DatabaseHandler.getConnection()) {

                String sql = "UPDATE equipment SET serial_number=?, name=?, category=?, condition=? WHERE asset_code=?";

                PreparedStatement pst = conn.prepareStatement(sql);

                pst.setString(1, serialField.getText());
                pst.setString(2, nameField.getText());
                pst.setString(3, categoryField.getText());
                pst.setString(4, conditionField.getText());
                pst.setString(5, equipment.getAssetCode());

                pst.executeUpdate();

                loadEquipment();

                showMessage("Success", "Equipment updated successfully.");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* =============================
       DELETE EQUIPMENT
    ============================== */

    private void deleteEquipment(Equipment equipment) {

        if (equipment == null) {
            showMessage("Delete Equipment", "No equipment selected.");
            return;
        }

        try (Connection conn = DatabaseHandler.getConnection()) {

            String sql = "DELETE FROM equipment WHERE asset_code=?";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, equipment.getAssetCode());

            pst.executeUpdate();

            loadEquipment();

            showMessage("Deleted", "Equipment deleted successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =============================
       MESSAGE
    ============================== */

    private void showMessage(String title, String message) {

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}