package com.mycompany.msr.amis;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

import com.mycompany.msr.amis.DatabaseHandler;

public class CreateAssignmentController implements Initializable {

    // ================= UI =================
    @FXML private TextField txtPerson;
    @FXML private TextField txtDepartment;
    @FXML private TextField txtEquipmentType;
    @FXML private TextField txtQuantity;

    @FXML private TableView<Assignment> tableAssignments;

    @FXML private TableColumn<Assignment, String> colPerson;
    @FXML private TableColumn<Assignment, String> colDepartment;
    @FXML private TableColumn<Assignment, String> colEquipment;
    @FXML private TableColumn<Assignment, Integer> colQty;
    @FXML private TableColumn<Assignment, String> colDate;

    // ================= DATA =================
    private final ObservableList<Assignment> assignmentList = FXCollections.observableArrayList();
    private Integer editingId = null;

    // ================= INIT =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupRightClickMenu();
        loadAssignments();
    }

    // ================= TABLE =================
    private void setupTable() {

        colPerson.setCellValueFactory(c -> c.getValue().personProperty());
        colDepartment.setCellValueFactory(c -> c.getValue().departmentProperty());
        colEquipment.setCellValueFactory(c -> c.getValue().equipmentTypeProperty());
        colQty.setCellValueFactory(c -> c.getValue().quantityProperty().asObject());
        colDate.setCellValueFactory(c -> c.getValue().dateProperty());

        tableAssignments.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableAssignments.setItems(assignmentList);
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

    // ================= SAVE =================
    @FXML
    private void saveAssignment() {

        String person = safe(txtPerson);
        String dept = safe(txtDepartment);
        String type = safe(txtEquipmentType);
        String qtyText = safe(txtQuantity);

        if (person.isEmpty() || type.isEmpty() || qtyText.isEmpty()) {
            showWarning("Missing Fields", "Person, Equipment Type, and Quantity are required.");
            return;
        }

        int qty;

        try {
            qty = Integer.parseInt(qtyText);
            if (qty <= 0) {
                showWarning("Invalid Quantity", "Quantity must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showWarning("Invalid Input", "Quantity must be a valid number.");
            return;
        }

        try {

            int available = DatabaseHandler.getAvailableStock(type);
            if (qty > available && editingId == null) {
                showWarning("Stock Error", "Not enough inventory available. Available: " + available);
                return;
            }

            if (editingId != null) {

                if (DatabaseHandler.isAssignmentLocked(editingId)) {
                    showWarning("Locked", "Assignment already used. Cannot edit.");
                    return;
                }

                DatabaseHandler.updateAssignment(editingId, person, dept, type, qty);
                editingId = null;

            } else {

                DatabaseHandler.insertAssignment(person, dept, type, qty);
            }

            clearForm();
            loadAssignments();

        } catch (Exception e) {
            showError("Database Error", e.getMessage());
        }
    }

    // ================= CLEAR =================
    @FXML
    private void clearForm() {

        txtPerson.clear();
        txtDepartment.clear();
        txtEquipmentType.clear();
        txtQuantity.clear();

        editingId = null;

        if (tableAssignments != null) {
            tableAssignments.getSelectionModel().clearSelection();
        }
    }

    // ================= RIGHT CLICK MENU =================
    private void setupRightClickMenu() {

        tableAssignments.setRowFactory(tv -> {

            TableRow<Assignment> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();

            MenuItem edit = new MenuItem("Edit Assignment");
            MenuItem delete = new MenuItem("Delete Assignment");

            edit.setOnAction(e -> {
                Assignment selected = row.getItem();
                if (selected != null) {
                    editAssignment(selected);
                }
            });

            delete.setOnAction(e -> {
                Assignment selected = row.getItem();
                if (selected != null) {
                    deleteAssignment(selected);
                }
            });

            menu.getItems().addAll(edit, delete);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );

            return row;
        });
    }

    // ================= EDIT =================
    private void editAssignment(Assignment selected) {

        if (DatabaseHandler.isAssignmentLocked(selected.getId())) {
            showWarning("Locked", "This assignment cannot be edited.");
            return;
        }

        editingId = selected.getId();

        txtPerson.setText(selected.getPerson());
        txtDepartment.setText(selected.getDepartment());
        txtEquipmentType.setText(selected.getEquipmentType());
        txtQuantity.setText(String.valueOf(selected.getQuantity()));
    }

    // ================= DELETE =================
    private void deleteAssignment(Assignment selected) {

        if (DatabaseHandler.isAssignmentLocked(selected.getId())) {
            showWarning("Locked", "This assignment cannot be deleted.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this assignment?");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    DatabaseHandler.deleteAssignment(selected.getId());
                    loadAssignments();
                } catch (Exception e) {
                    showError("Delete Error", e.getMessage());
                }
            }
        });
    }

    // ================= HELPERS =================
    private String safe(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    // ================= ALERTS =================
    private void showWarning(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }
}