package com.mycompany.msr.amis;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class ReturnEquipmentController implements Initializable {

    @FXML
    private ComboBox<?> cmbAssignments;
    @FXML
    private TextField txtReturnedBy;
    @FXML
    private TextField txtPhone;
    @FXML
    private TextField txtNid;
    @FXML
    private ComboBox<String> cmbCondition;   // FIX: typed
    @FXML
    private TextField txtRemarks;
    @FXML
    private Label lblAssignOfficer;
    @FXML
    private Label lblAssignType;
    @FXML
    private Label lblAssignQty;
    @FXML
    private TextField txtAssetCode;
    @FXML
    private TextField txtOfficer;
    @FXML
    private TextField txtEquipmentType;
    @FXML
    private TableView<?> tableReturns;
    @FXML
    private TableColumn<?, ?> colAsset;
    @FXML
    private TableColumn<?, ?> colReturnedBy;
    @FXML
    private TableColumn<?, ?> colPhone;
    @FXML
    private TableColumn<?, ?> colCondition;
    @FXML
    private TableColumn<?, ?> colDate;
    @FXML
    private Label lblFileName;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ✅ FIX: populate condition dropdown
        cmbCondition.getItems().addAll(
                "Good",
                "Fair",
                "Damaged",
                "Faulty",
                "Lost"
        );
    }

    @FXML
    private void handleReturn(ActionEvent event) {

        // ✅ Basic validation (minimal)
        if (txtAssetCode.getText().isEmpty()) {
            System.out.println("Asset Code required");
            return;
        }

        if (cmbCondition.getValue() == null) {
            System.out.println("Select condition");
            return;
        }

        // TEMP (until DB logic added)
        System.out.println("Returning asset: " + txtAssetCode.getText());
        System.out.println("Condition: " + cmbCondition.getValue());

    }

    @FXML
    private void clearForm(ActionEvent event) {

        txtAssetCode.clear();
        txtOfficer.clear();
        txtEquipmentType.clear();
        txtReturnedBy.clear();
        txtPhone.clear();
        txtNid.clear();
        txtRemarks.clear();

        cmbCondition.setValue(null);

    }

    @FXML
    private void handleBulkUpload(ActionEvent event) {

        // placeholder
        System.out.println("Bulk upload clicked");

    }

    @FXML
    private void chooseFile(ActionEvent event) {

        // placeholder
        lblFileName.setText("File selected");

    }

}