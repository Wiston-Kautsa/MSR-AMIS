package com.mycompany.msr.amis;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class UsersController implements Initializable {

    @FXML private TextField txtName;
    @FXML private TextField txtUsername;
    @FXML private TextField txtPhone;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private TextField txtEmail;

    @FXML private TableView<User> tableUsers;

    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colEmail;

    private ObservableList<User> data;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        cmbRole.getItems().addAll("ADMIN", "USER");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        loadUsers();
    }

    // ================= LOAD USERS =================
    private void loadUsers() {
        data = DatabaseHandler.getUsers();

        // ✔ FIX: force refresh
        tableUsers.setItems(null);
        tableUsers.setItems(data);
    }

    // ================= ADD USER =================
    @FXML
    private void handleAddUser(ActionEvent event) {

        String name = txtName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // ✔ FIX: avoid null
        String role = cmbRole.getValue() != null ? cmbRole.getValue() : "USER";

        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill all required fields.");
            return;
        }

        try {
            DatabaseHandler.insertUser(name, username, password, role, phone, email);

            clearForm();
            loadUsers();

            showAlert("Success", "User added successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Username already exists or invalid input.");
        }
    }

    // ================= DELETE USER =================
    @FXML
    private void handleDeleteUser(ActionEvent event) {

        User selected = tableUsers.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Error", "Select a user first.");
            return;
        }

        try {
            DatabaseHandler.deleteUser(selected.getId());
            loadUsers();

            showAlert("Success", "User deleted successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Delete failed.");
        }
    }

    // ================= CLEAR FORM =================
    private void clearForm() {
        txtName.clear();
        txtUsername.clear();
        txtPassword.clear();
        txtPhone.clear();
        txtEmail.clear();
        cmbRole.setValue(null);
    }

    // ================= ALERT =================
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}