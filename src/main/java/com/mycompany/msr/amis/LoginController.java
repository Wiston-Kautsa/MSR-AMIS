package com.mycompany.msr.amis;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class LoginController implements Initializable {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private CheckBox chkShowPassword;
    @FXML private Label lblLoginStatus;
    @FXML private Label lblDefaultAdmin;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (txtPasswordVisible != null && txtPassword != null) {
            txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        }

        if (chkShowPassword != null && txtPasswordVisible != null && txtPassword != null) {
            chkShowPassword.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                txtPasswordVisible.setVisible(isSelected);
                txtPasswordVisible.setManaged(isSelected);
                txtPassword.setVisible(!isSelected);
                txtPassword.setManaged(!isSelected);
            });
        }

        if (lblDefaultAdmin != null) {
            lblDefaultAdmin.setText("Default Admin: admin@msr.local / admin123");
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

        User user;
        try {
            user = DatabaseHandler.authenticateUser(email, password);
        } catch (SecurityException e) {
            showStatus(e.getMessage());
            return;
        }
        if (user == null) {
            showStatus("Invalid email or password.");
            return;
        }

        try {
            Session.setCurrentUser(user);
            App.showDashboardPage();
        } catch (SecurityException e) {
            showStatus(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Login succeeded, but the dashboard could not be opened.");
        }
    }

    @FXML
    private void handleResetPassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Forgot Password");

        TextField identifierField = new TextField(txtEmail.getText() == null ? "" : txtEmail.getText().trim().toLowerCase());
        TextField codeField = new TextField();
        PasswordField newPasswordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        Label statusLabel = new Label("Enter your registered email address or username.");

        identifierField.setPromptText("Email or username");
        codeField.setPromptText("Reset code");
        newPasswordField.setPromptText("New password");
        confirmPasswordField.setPromptText("Confirm password");
        statusLabel.setWrapText(true);

        setResetFieldsEnabled(codeField, newPasswordField, confirmPasswordField, false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Email / Username:"), 0, 0);
        grid.add(identifierField, 1, 0);
        grid.add(new Label("Reset Code:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("New Password:"), 0, 2);
        grid.add(newPasswordField, 1, 2);
        grid.add(new Label("Confirm Password:"), 0, 3);
        grid.add(confirmPasswordField, 1, 3);
        grid.add(statusLabel, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        ButtonType sendCodeType = new ButtonType("Send Reset Code", ButtonBar.ButtonData.LEFT);
        ButtonType resetType = new ButtonType("Reset Password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                sendCodeType,
                resetType,
                ButtonType.CANCEL
        );

        Node sendCodeButton = dialog.getDialogPane().lookupButton(sendCodeType);
        Node resetButton = dialog.getDialogPane().lookupButton(resetType);
        resetButton.setDisable(true);

        sendCodeButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();

            String identifier = normalized(identifierField.getText());
            if (identifier.isBlank()) {
                statusLabel.setText("Enter your registered email address or username.");
                return;
            }

            try {
                String resetCode = PasswordUtils.generateNumericCode(6);
                String recipientEmail = DatabaseHandler.issuePasswordResetCode(
                        identifier,
                        resetCode,
                        LocalDateTime.now().plusMinutes(10)
                );
                try {
                    EmailService.sendPasswordResetCode(recipientEmail, resetCode);
                } catch (Exception e) {
                    DatabaseHandler.clearPasswordResetCode(identifier);
                    throw e;
                }
                setResetFieldsEnabled(codeField, newPasswordField, confirmPasswordField, true);
                resetButton.setDisable(false);
                statusLabel.setText("A reset code was sent to " + maskEmail(recipientEmail) + ". It expires in 10 minutes.");
            } catch (Exception e) {
                statusLabel.setText(e.getMessage());
            }
        });

        resetButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();

            String identifier = normalized(identifierField.getText());
            String resetCode = normalized(codeField.getText());
            String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

            if (identifier.isBlank() || resetCode.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                statusLabel.setText("Enter your account, reset code, and the new password twice.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                statusLabel.setText("Passwords do not match.");
                return;
            }

            try {
                DatabaseHandler.resetPasswordWithCode(identifier, resetCode, PasswordUtils.hash(newPassword));
                txtEmail.setText(identifier);
                txtPassword.clear();
                showStatus("Password reset successfully. Sign in with the new password.");
                dialog.setResult(ButtonType.OK);
                dialog.close();
            } catch (Exception e) {
                statusLabel.setText(e.getMessage());
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (chkShowPassword != null) {
                chkShowPassword.setSelected(false);
            }
        }
    }

    @FXML
    private void handleClear() {
        txtEmail.clear();
        txtPassword.clear();
        if (chkShowPassword != null) {
            chkShowPassword.setSelected(false);
        }
        showStatus("");
    }

    private void showStatus(String message) {
        if (lblLoginStatus != null) {
            lblLoginStatus.setText(message);
        }
    }

    private void setResetFieldsEnabled(TextField codeField, PasswordField newPasswordField,
                                       PasswordField confirmPasswordField, boolean enabled) {
        codeField.setDisable(!enabled);
        newPasswordField.setDisable(!enabled);
        confirmPasswordField.setDisable(!enabled);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private String maskEmail(String email) {
        String normalized = normalized(email);
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 1) {
            return normalized;
        }
        return normalized.charAt(0) + "***" + normalized.substring(atIndex - 1);
    }
}
