package com.mycompany.msr.amis;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class CreateAssignmentController implements Initializable {

    // ================= UI =================
    @FXML private ComboBox<String> cmbPerson;
    @FXML private TextField txtDepartment;
    @FXML private TextField txtEquipmentType;
    @FXML private TextField txtQuantity;

    @FXML private TableView<Assignment> tableAssignments;

    @FXML private TableColumn<Assignment, String> colPerson;
    @FXML private TableColumn<Assignment, String> colDepartment;
    @FXML private TableColumn<Assignment, String> colEquipment;
    @FXML private TableColumn<Assignment, Integer> colQty;
    @FXML private TableColumn<Assignment, String> colDate;

    private final ObservableList<Assignment> assignmentList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupRightClickMenu();
        loadUsers();
        loadAssignments();

        if (cmbPerson != null) {
            cmbPerson.setPromptText("Select Responsible Person");

            cmbPerson.setOnAction(e -> {
                if ("➕ Add New User".equals(cmbPerson.getValue())) {
                    openAddUserDialog();
                }
            });
        }
    }

    // ================= LOAD USERS =================
    private void loadUsers() {

        if (cmbPerson == null) {
            System.out.println("cmbPerson not injected!");
            return;
        }

        cmbPerson.getItems().clear();

        try {
            for (User u : DatabaseHandler.getUsers()) {
                cmbPerson.getItems().add(u.getFullName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        cmbPerson.getItems().add("➕ Add New User");
    }

    // ================= ADD USER =================
    private void openAddUserDialog() {

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Add User");

        TextField nameField = new TextField();
        TextField phoneField = new TextField();
        TextField emailField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);

        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                return new String[]{
                        nameField.getText(),
                        phoneField.getText(),
                        emailField.getText()
                };
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();

        result.ifPresent(data -> {

            String name = data[0].trim();
            String phone = data[1].trim();
            String email = data[2].trim();

            if (name.isEmpty()) {
                showWarning("Error", "Name required");
                return;
            }

            if (!email.isEmpty() && DatabaseHandler.emailExists(email)) {
                showWarning("Duplicate", "Email already exists");
                return;
            }

            if (!phone.isEmpty() && DatabaseHandler.phoneExists(phone)) {
                showWarning("Duplicate", "Phone already exists");
                return;
            }

            try {
                String username = name.replaceAll(" ", "").toLowerCase();
                String password = "1234";

                DatabaseHandler.insertUser(name, username, password, "USER", phone, email);

                loadUsers();
                cmbPerson.setValue(name);

            } catch (Exception e) {
                showError("Error", "Failed to add user");
            }
        });
    }

    // ================= TABLE =================
    private void setupTable() {
        colPerson.setCellValueFactory(c -> c.getValue().personProperty());
        colDepartment.setCellValueFactory(c -> c.getValue().departmentProperty());
        colEquipment.setCellValueFactory(c -> c.getValue().equipmentTypeProperty());
        colQty.setCellValueFactory(c -> c.getValue().quantityProperty().asObject());
        colDate.setCellValueFactory(c -> c.getValue().dateProperty());

        tableAssignments.setItems(assignmentList);
    }

    private void loadAssignments() {
        assignmentList.clear();
        assignmentList.addAll(DatabaseHandler.getAssignments());
    }

    // ================= RIGHT CLICK MENU =================
    private void setupRightClickMenu() {

        tableAssignments.setRowFactory(tv -> {

            TableRow<Assignment> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();

            MenuItem delete = new MenuItem("Delete Assignment");

            delete.setOnAction(e -> {
                Assignment selected = row.getItem();
                if (selected != null) {
                    try {
                        DatabaseHandler.deleteAssignment(selected.getId());
                        loadAssignments();
                    } catch (Exception ex) {
                        showError("Error", ex.getMessage());
                    }
                }
            });

            menu.getItems().add(delete);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );

            return row;
        });
    }

    // ================= SAVE =================
    @FXML
    private void saveAssignment() {

        String person = cmbPerson != null && cmbPerson.getValue() != null ? cmbPerson.getValue() : "";

        if (person.equals("➕ Add New User")) {
            showWarning("Invalid Selection", "Please select a real user.");
            return;
        }

        String dept = safe(txtDepartment);
        String type = safe(txtEquipmentType).toUpperCase();
        String qtyText = safe(txtQuantity);

        if (person.isEmpty() || type.isEmpty() || qtyText.isEmpty()) {
            showWarning("Missing Fields", "Required fields missing.");
            return;
        }

        int qty;

        try {
            qty = Integer.parseInt(qtyText);
            if (qty <= 0) throw new Exception();
        } catch (Exception e) {
            showWarning("Invalid", "Quantity must be valid.");
            return;
        }

        try {
            int available = DatabaseHandler.getAvailableStock(type);

            if (qty > available) {
                showWarning("Stock", "Not enough stock.");
                return;
            }

            DatabaseHandler.insertAssignment(person, dept, type, qty);

            clearForm();
            loadAssignments();

        } catch (Exception e) {
            showError("DB Error", e.getMessage());
        }
    }

    // ================= CLEAR =================
    @FXML
    private void clearForm() {
        if (cmbPerson != null) cmbPerson.setValue(null);
        txtDepartment.clear();
        txtEquipmentType.clear();
        txtQuantity.clear();
    }

    private String safe(TextField f) {
        return f.getText() == null ? "" : f.getText().trim();
    }

    private void showWarning(String t, String m) {
        new Alert(Alert.AlertType.WARNING, m).showAndWait();
    }

    private void showError(String t, String m) {
        new Alert(Alert.AlertType.ERROR, m).showAndWait();
    }
}