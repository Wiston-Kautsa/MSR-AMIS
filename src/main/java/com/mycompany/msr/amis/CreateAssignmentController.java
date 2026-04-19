package com.mycompany.msr.amis;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class CreateAssignmentController implements Initializable {

    private static final String DEFAULT_DEPARTMENT = "NLGFC";

    @FXML private ComboBox<String> cmbPerson;
    @FXML private TextField txtDepartment;
    @FXML private ComboBox<String> cmbEquipmentType;
    @FXML private TextField txtQuantity;
    @FXML private Label lblAvailableStock;

    @FXML private TableView<Assignment> tableAssignments;

    @FXML private TableColumn<Assignment, String> colPerson;
    @FXML private TableColumn<Assignment, String> colDepartment;
    @FXML private TableColumn<Assignment, String> colEquipment;
    @FXML private TableColumn<Assignment, Integer> colQty;
    @FXML private TableColumn<Assignment, String> colStatus;
    @FXML private TableColumn<Assignment, String> colDate;

    private final ObservableList<Assignment> assignmentList = FXCollections.observableArrayList();
    private final Map<String, Integer> availableStockByCategory = new LinkedHashMap<>();
    private final Map<String, User> usersByName = new LinkedHashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupRightClickMenu();
        loadUsers();
        loadEquipmentTypes();
        loadAssignments();

        if (cmbPerson != null) {
            cmbPerson.setPromptText("Select Responsible Person");
            cmbPerson.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldValue, newValue) -> fillDepartmentFromSelectedUser()
            );
        }

        if (txtDepartment != null) {
            txtDepartment.setText(DEFAULT_DEPARTMENT);
        }

        if (cmbEquipmentType != null) {
            cmbEquipmentType.setPromptText("Select Equipment Group");
            cmbEquipmentType.setOnAction(event -> updateAvailableStockLabel());
        }

        updateAvailableStockLabel();
    }

    private void loadUsers() {
        if (cmbPerson == null) {
            return;
        }

        cmbPerson.getItems().clear();
        usersByName.clear();

        try {
            for (User u : DatabaseHandler.getUsers()) {
                cmbPerson.getItems().add(u.getFullName());
                usersByName.put(u.getFullName(), u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillDepartmentFromSelectedUser() {
        if (cmbPerson == null || txtDepartment == null) {
            return;
        }

        User selectedUser = usersByName.get(cmbPerson.getValue());
        if (selectedUser == null) {
            txtDepartment.setText(DEFAULT_DEPARTMENT);
            return;
        }

        String department = selectedUser.getDepartment();
        txtDepartment.setText((department == null || department.isBlank()) ? DEFAULT_DEPARTMENT : department);
    }

    private void loadEquipmentTypes() {
        if (cmbEquipmentType == null) {
            return;
        }

        availableStockByCategory.clear();
        availableStockByCategory.putAll(DatabaseHandler.getAvailableStockByCategory());

        cmbEquipmentType.getItems().clear();
        cmbEquipmentType.getItems().addAll(availableStockByCategory.keySet());
    }

    private void updateAvailableStockLabel() {
        if (lblAvailableStock == null) {
            return;
        }

        String type = cmbEquipmentType != null ? cmbEquipmentType.getValue() : null;
        if (type == null || type.isBlank()) {
            lblAvailableStock.setText("Available in stock: Select equipment group");
            return;
        }

        int available = availableStockByCategory.getOrDefault(type, 0);
        lblAvailableStock.setText("Available in stock: " + available);
    }

    private void setupTable() {
        colPerson.setCellValueFactory(c -> c.getValue().personProperty());
        colDepartment.setCellValueFactory(c -> c.getValue().departmentProperty());
        colEquipment.setCellValueFactory(c -> c.getValue().equipmentTypeProperty());
        colQty.setCellValueFactory(c -> c.getValue().quantityProperty().asObject());
        colStatus.setCellValueFactory(c -> {
            Assignment assignment = c.getValue();
            int distributed = DatabaseHandler.getDistributedCountForAssignment(assignment.getId());
            String status;
            if (distributed == 0) {
                status = "PENDING";
            } else if (distributed < assignment.getQuantity()) {
                status = "PARTIAL";
            } else {
                status = "ENROLLED";
            }
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        colDate.setCellValueFactory(c -> c.getValue().dateProperty());

        tableAssignments.setItems(assignmentList);
    }

    private void loadAssignments() {
        assignmentList.clear();
        assignmentList.addAll(DatabaseHandler.getAssignments());
    }

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
                        loadEquipmentTypes();
                        updateAvailableStockLabel();
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

    @FXML
    private void saveAssignment() {
        String person = cmbPerson != null && cmbPerson.getValue() != null ? cmbPerson.getValue() : "";
        String dept = safe(txtDepartment);
        String type = cmbEquipmentType != null && cmbEquipmentType.getValue() != null
                ? cmbEquipmentType.getValue().trim()
                : "";
        String qtyText = safe(txtQuantity);

        if (dept.isEmpty()) {
            User selectedUser = usersByName.get(person);
            if (selectedUser != null && selectedUser.getDepartment() != null && !selectedUser.getDepartment().isBlank()) {
                dept = selectedUser.getDepartment().trim();
                txtDepartment.setText(dept);
            }
        }

        if (person.isEmpty() || type.isEmpty() || qtyText.isEmpty()) {
            showWarning("Missing Fields", "Responsible person, equipment group, and quantity are required.");
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyText);
            if (qty <= 0) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            showWarning("Invalid", "Quantity must be a valid positive number.");
            return;
        }

        try {
            int available = DatabaseHandler.getAvailableStock(type);
            if (qty > available) {
                showWarning("Stock", "Only " + available + " " + type + " item(s) are currently available.");
                return;
            }

            DatabaseHandler.insertAssignment(person, dept.isEmpty() ? DEFAULT_DEPARTMENT : dept, type, qty);
            clearForm();
            loadAssignments();
            loadEquipmentTypes();
            updateAvailableStockLabel();

        } catch (Exception e) {
            showError("DB Error", e.getMessage());
        }
    }

    @FXML
    private void clearForm() {
        if (cmbPerson != null) {
            cmbPerson.setValue(null);
        }
        if (cmbEquipmentType != null) {
            cmbEquipmentType.setValue(null);
        }
        txtDepartment.setText(DEFAULT_DEPARTMENT);
        txtQuantity.clear();
        updateAvailableStockLabel();
    }

    @FXML
    private void exportAssignments() {
        ObservableList<Assignment> itemsToExport = tableAssignments.getItems();

        if (itemsToExport == null || itemsToExport.isEmpty()) {
            showWarning("No Data", "There are no assignment records to export.");
            return;
        }

        File file = FileLocationHelper.fileInDownloads("assignment_list.csv");
        OperationFeedbackHelper.showInfo(
                "Export Starting",
                "Preparing assignment list export.\n\nRows to export: " + itemsToExport.size()
        );

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("Person,Department,Equipment,Quantity,Status,Date\n");

            for (Assignment assignment : itemsToExport) {
                int distributed = DatabaseHandler.getDistributedCountForAssignment(assignment.getId());
                String status;
                if (distributed == 0) {
                    status = "PENDING";
                } else if (distributed < assignment.getQuantity()) {
                    status = "PARTIAL";
                } else {
                    status = "ENROLLED";
                }

                writer.append(csvSafe(assignment.getPerson())).append(",")
                        .append(csvSafe(assignment.getDepartment())).append(",")
                        .append(csvSafe(assignment.getEquipmentType())).append(",")
                        .append(String.valueOf(assignment.getQuantity())).append(",")
                        .append(status).append(",")
                        .append(csvSafe(assignment.getDate())).append("\n");
            }

            OperationFeedbackHelper.showInfo(
                    "Export Complete",
                    "Assignment list exported successfully to:\n" + file.getAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError(
                    "Export Failed",
                    "Failed to export the assignment list."
            );
        }
    }

    private String safe(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
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

    private void showWarning(String title, String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }

    private void showError(String title, String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
