/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.mycompany.msr.amis;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.collections.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class AssignmentListController implements Initializable {

    @FXML
    private TextField txtSearch;
    @FXML
    private TableView<?> tableAssignments;
    @FXML
    private TableColumn<?, ?> colId;
    @FXML
    private TableColumn<?, ?> colPerson;
    @FXML
    private TableColumn<?, ?> colDepartment;
    @FXML
    private TableColumn<?, ?> colEquipment;
    @FXML
    private TableColumn<?, ?> colQty;
    @FXML
    private TableColumn<?, ?> colAssigned;
    @FXML
    private TableColumn<?, ?> colStatus;
    @FXML
    private TableColumn<?, ?> colDate;
    @FXML
    private TableColumn<?, ?> colActions;

    // ================= DATA =================
    private ObservableList<Assignment> assignmentList = FXCollections.observableArrayList();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Cast TableView safely
        TableView<Assignment> table = (TableView<Assignment>) tableAssignments;

        // Bind columns
        ((TableColumn<Assignment, Integer>) colId).setCellValueFactory(new PropertyValueFactory<>("id"));
        ((TableColumn<Assignment, String>) colPerson).setCellValueFactory(new PropertyValueFactory<>("person"));
        ((TableColumn<Assignment, String>) colDepartment).setCellValueFactory(new PropertyValueFactory<>("department"));
        ((TableColumn<Assignment, String>) colEquipment).setCellValueFactory(new PropertyValueFactory<>("equipmentType"));
        ((TableColumn<Assignment, Integer>) colQty).setCellValueFactory(new PropertyValueFactory<>("quantity"));
        ((TableColumn<Assignment, String>) colDate).setCellValueFactory(new PropertyValueFactory<>("date"));

        // Assigned (default = 0 for now)
        ((TableColumn<Assignment, Integer>) colAssigned).setCellValueFactory(cell ->
                new javafx.beans.property.SimpleIntegerProperty(0).asObject()
        );

        // Status (default = PENDING)
        ((TableColumn<Assignment, String>) colStatus).setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty("PENDING")
        );

        table.setItems(assignmentList);

        loadAssignments();
        addActionButtons();
    }

    // ================= LOAD =================
    private void loadAssignments() {
        try {
            assignmentList.clear();
            assignmentList.addAll(DatabaseHandler.getAssignments());
        } catch (Exception e) {
            showError("Load Error", e.getMessage());
        }
    }

    // ================= SEARCH =================
    @FXML
    private void searchAssignment(ActionEvent event) {

        TableView<Assignment> table = (TableView<Assignment>) tableAssignments;

        String keyword = txtSearch.getText().toLowerCase();

        if (keyword.isEmpty()) {
            table.setItems(assignmentList);
            return;
        }

        ObservableList<Assignment> filtered = FXCollections.observableArrayList();

        for (Assignment a : assignmentList) {

            if (a.getPerson().toLowerCase().contains(keyword)
                    || a.getDepartment().toLowerCase().contains(keyword)
                    || a.getEquipmentType().toLowerCase().contains(keyword)) {

                filtered.add(a);
            }
        }

        table.setItems(filtered);
    }

    // ================= REFRESH =================
    @FXML
    private void refreshTable(ActionEvent event) {
        loadAssignments();
    }

    // ================= ACTION BUTTONS =================
    private void addActionButtons() {

        TableColumn<Assignment, Void> actionsCol = (TableColumn<Assignment, Void>) colActions;

        actionsCol.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");

            {
                btnEdit.setOnAction(event -> {
                    Assignment a = (Assignment) getTableView().getItems().get(getIndex());
                    editAssignment(a);
                });

                btnDelete.setOnAction(event -> {
                    Assignment a = (Assignment) getTableView().getItems().get(getIndex());
                    deleteAssignment(a);
                });
            }

            private final HBox pane = new HBox(5, btnEdit, btnDelete);

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    // ================= EDIT =================
    private void editAssignment(Assignment a) {

        if (a == null) return;

        showInfo("Edit assignment: " + a.getPerson());
    }

    // ================= DELETE =================
    private void deleteAssignment(Assignment a) {

        if (a == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this assignment?");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {

                try {
                    DatabaseHandler.deleteAssignment(a.getId());
                    loadAssignments();
                    showInfo("Deleted successfully");
                } catch (Exception e) {
                    showError("Delete Error", e.getMessage());
                }
            }
        });
    }

    // ================= ALERTS =================
    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }

}