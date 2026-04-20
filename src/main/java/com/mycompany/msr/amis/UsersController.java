package com.mycompany.msr.amis;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class UsersController implements Initializable {
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";


    @FXML private TextField txtName;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private ComboBox<String> cmbDepartment;
    @FXML private TextField txtEmail;
    @FXML private Label lblUserStatus;

    @FXML private TableView<User> tableUsers;

    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colDepartment;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colStatus;

    private ObservableList<User> data;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        AccessControl.requireRole(AccessControl.ROLE_SUPER_ADMIN, AccessControl.ROLE_ADMIN);
        configureRoleChoices(cmbRole);
        loadDepartments();

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupUsersTableMenu();
        loadUsers();
    }

    private void setupUsersTableMenu() {
        if (tableUsers == null) {
            return;
        }

        tableUsers.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();

            MenuItem edit = new MenuItem("Edit User");
            edit.setOnAction(e -> editUser(row.getItem()));

            MenuItem delete = new MenuItem("Delete User");
            delete.setOnAction(e -> deleteUser(row.getItem()));

            MenuItem toggleStatus = new MenuItem();
            toggleStatus.textProperty().bind(Bindings.createStringBinding(
                    () -> {
                        User user = row.getItem();
                        if (user == null) {
                            return "Toggle Status";
                        }
                        return AccessControl.STATUS_FROZEN.equalsIgnoreCase(user.getStatus())
                                ? "Activate User"
                                : "Freeze User";
                    },
                    row.itemProperty()
            ));
            toggleStatus.setOnAction(e -> toggleUserStatus(row.getItem()));

            MenuItem refresh = new MenuItem("Refresh Users");
            refresh.setOnAction(e -> refreshUsers());

            menu.getItems().addAll(edit, delete, toggleStatus, refresh);

            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );

            return row;
        });
    }

    private void loadUsers() {
        data = DatabaseHandler.getUsers();
        tableUsers.setItems(null);
        tableUsers.setItems(data);
    }

    private void loadDepartments() {
        if (cmbDepartment == null) {
            return;
        }
        cmbDepartment.getItems().clear();
        cmbDepartment.getItems().addAll(DatabaseHandler.getDepartments());
        cmbDepartment.setEditable(true);
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        String name = txtName.getText().trim();
        String email = txtEmail.getText().trim().toLowerCase();
        String password = txtPassword.getText().trim();
        String role = cmbRole.getValue() != null ? cmbRole.getValue() : "USER";
        String department = comboText(cmbDepartment);

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || department.isEmpty()) {
            showAlert("Error", "Please fill full name, department, email, and password.");
            return;
        }

        if (!email.matches(EMAIL_PATTERN)) {
            showAlert("Error", "Enter a valid email address.");
            return;
        }

        if (DatabaseHandler.emailExists(email)) {
            showAlert("Error", "Email already exists. Use a different email.");
            return;
        }

        try {
            String hashedPassword = PasswordUtils.hash(password);
            DatabaseHandler.insertUser(name, hashedPassword, role, department, email);

            clearForm();
            loadDepartments();
            loadUsers();
            showStatus("User information added successfully.");

            showAlert("Success", "User added successfully.");

        } catch (SecurityException e) {
            showAlert("Access Denied", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "User creation failed.");
        }
    }

    @FXML
    private void handleDeleteUser(ActionEvent event) {
        deleteUser(tableUsers.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleRefreshUsers(ActionEvent event) {
        refreshUsers();
    }

    @FXML
    private void handleEditUser(ActionEvent event) {
        editUser(tableUsers.getSelectionModel().getSelectedItem());
    }

    private void editUser(User selected) {
        if (selected == null) {
            showAlert("Error", "Select a user first.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit User");

        TextField nameField = new TextField(selected.getFullName());
        TextField emailField = new TextField(selected.getEmail());
        ComboBox<String> roleField = new ComboBox<>();
        configureRoleChoices(roleField);
        roleField.setValue(selected.getRole());
        ComboBox<String> departmentField = new ComboBox<>();
        departmentField.getItems().addAll(DatabaseHandler.getDepartments());
        departmentField.setEditable(true);
        departmentField.getEditor().setText(selected.getDepartment());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Leave blank to keep current password");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(roleField, 1, 2);
        grid.add(new Label("Department:"), 0, 3);
        grid.add(departmentField, 1, 3);
        grid.add(new Label("New Password:"), 0, 4);
        grid.add(passwordField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Save", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String name = nameField.getText().trim();
        String email = emailField.getText().trim().toLowerCase();
        String role = roleField.getValue() != null ? roleField.getValue() : "USER";
        String department = comboText(departmentField);
        String password = passwordField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || department.isEmpty()) {
            showAlert("Error", "Full name, department, and email are required.");
            return;
        }

        if (!email.matches(EMAIL_PATTERN)) {
            showAlert("Error", "Enter a valid email address.");
            return;
        }

        if (DatabaseHandler.emailExistsForOtherUser(email, selected.getId())) {
            showAlert("Error", "Email already exists. Use a different email.");
            return;
        }

        if ("ADMIN".equalsIgnoreCase(selected.getRole())
                && !"ADMIN".equalsIgnoreCase(role)
                && DatabaseHandler.getAdminCount() <= 1) {
            showAlert("Error", "The last admin account cannot be changed to a non-admin role.");
            return;
        }

        try {
            String hashedPassword = password.isEmpty() ? "" : PasswordUtils.hash(password);
            boolean updated = DatabaseHandler.updateUser(selected.getId(), name, hashedPassword, role, department, email);

            if (!updated) {
                showAlert("Error", "User was not updated.");
                return;
            }

            loadDepartments();
            loadUsers();
            selectUserById(selected.getId());
            showStatus("User information has been edited successfully.");
            showAlert("Success", "User updated successfully.");

        } catch (SecurityException e) {
            showAlert("Access Denied", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "User update failed.");
        }
    }

    private void deleteUser(User selected) {
        if (selected == null) {
            showAlert("Error", "Select a user first.");
            return;
        }

        if ("ADMIN".equalsIgnoreCase(selected.getRole()) && DatabaseHandler.getAdminCount() <= 1) {
            showAlert("Error", "The last admin account cannot be deleted.");
            return;
        }

        try {
            DatabaseHandler.deleteUser(selected.getId());
            loadUsers();
            loadDepartments();
            tableUsers.getSelectionModel().clearSelection();
            showStatus("User information deleted successfully.");
            showAlert("Success", "User deleted successfully.");

        } catch (SecurityException e) {
            showAlert("Access Denied", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Delete failed.");
        }
    }

    private void toggleUserStatus(User selected) {
        if (selected == null) {
            showAlert("Error", "Select a user first.");
            return;
        }

        String nextStatus = AccessControl.STATUS_FROZEN.equalsIgnoreCase(selected.getStatus())
                ? AccessControl.STATUS_ACTIVE
                : AccessControl.STATUS_FROZEN;

        try {
            boolean updated = DatabaseHandler.updateUserStatus(selected.getId(), nextStatus);
            if (!updated) {
                showAlert("Error", "User status was not updated.");
                return;
            }

            loadUsers();
            showStatus("User status updated to " + nextStatus + ".");
            showAlert("Success", "User status updated successfully.");
        } catch (SecurityException e) {
            showAlert("Access Denied", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Status update failed.");
        }
    }

    private void refreshUsers() {
        loadDepartments();
        loadUsers();
        tableUsers.getSelectionModel().clearSelection();
        showStatus("Users and departments refreshed.");
    }

    private void clearForm() {
        txtName.clear();
        txtPassword.clear();
        txtEmail.clear();
        cmbRole.setValue(null);
        cmbDepartment.setValue(null);
        cmbDepartment.getEditor().clear();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String comboText(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return "";
        }
        String editorText = comboBox.getEditor().getText();
        if (editorText != null && !editorText.isBlank()) {
            return editorText.trim();
        }
        if (comboBox.getValue() != null && !comboBox.getValue().isBlank()) {
            return comboBox.getValue().trim();
        }
        return "";
    }

    private void showStatus(String message) {
        if (lblUserStatus != null) {
            lblUserStatus.setText(message);
        }
    }

    private void selectUserById(int userId) {
        if (tableUsers == null || data == null) {
            return;
        }

        for (User user : data) {
            if (user.getId() == userId) {
                tableUsers.getSelectionModel().select(user);
                tableUsers.scrollTo(user);
                return;
            }
        }
    }

    private void configureRoleChoices(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.getItems().clear();
        if (Session.hasRole(AccessControl.ROLE_SUPER_ADMIN)) {
            comboBox.getItems().addAll(
                    AccessControl.ROLE_ADMIN,
                    AccessControl.ROLE_USER
            );
        } else {
            comboBox.getItems().add(AccessControl.ROLE_USER);
        }
    }

}
