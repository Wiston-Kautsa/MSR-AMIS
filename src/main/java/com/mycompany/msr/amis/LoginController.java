package com.mycompany.msr.amis;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class LoginController implements Initializable {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblLoginStatus;
    @FXML private Label lblDefaultAdmin;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (lblDefaultAdmin != null) {
            lblDefaultAdmin.setText("First login: admin@msr.local / admin123");
        }
    }

    @FXML
    private void handleLogin() {
        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim().toLowerCase();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showStatus("Enter email and password.");
            return;
        }

        User user = DatabaseHandler.authenticateUser(email, password);
        if (user == null) {
            showStatus("Invalid email or password.");
            return;
        }

        try {
            App.showDashboardPage();
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Login succeeded, but the dashboard could not be opened.");
        }
    }

    @FXML
    private void handleResetPassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");

        TextField emailField = new TextField(txtEmail.getText() == null ? "" : txtEmail.getText().trim().toLowerCase());
        PasswordField newPasswordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();

        emailField.setPromptText("Email");
        newPasswordField.setPromptText("New password");
        confirmPasswordField.setPromptText("Confirm password");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Reset Password", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (email.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showStatus("Enter email and both password fields.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showStatus("Passwords do not match.");
            return;
        }

        if (!DatabaseHandler.userExistsByEmail(email)) {
            showStatus("No user was found with that email.");
            return;
        }

        boolean reset = DatabaseHandler.resetUserPasswordByEmail(email, PasswordUtils.hash(newPassword));
        if (!reset) {
            showStatus("Password reset failed.");
            return;
        }

        txtEmail.setText(email);
        txtPassword.clear();
        showStatus("Password reset successfully. Sign in with the new password.");
    }

    @FXML
    private void handleClear() {
        txtEmail.clear();
        txtPassword.clear();
        showStatus("");
    }

    private void showStatus(String message) {
        if (lblLoginStatus != null) {
            lblLoginStatus.setText(message);
        }
    }
}
