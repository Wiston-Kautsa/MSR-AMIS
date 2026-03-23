package com.mycompany.msr.amis;

import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class DistributeEquipmentController implements Initializable {

    // ================= UI =================
    @FXML private ComboBox<String> cmbAssignments;
    @FXML private ComboBox<String> equipmentCombo; // ✅ FIXED

    @FXML private Label lblProgress;
    @FXML private Label lblSelectedAssignment;
    @FXML private Label lblAssignmentStats;
    @FXML private Label lblSelectedFile;

    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtNid;

    @FXML private TableView<Distribution> distributionTable;
    @FXML private TableColumn<Distribution, String> colDistAsset;
    @FXML private TableColumn<Distribution, String> colDistName;
    @FXML private TableColumn<Distribution, String> colDistPhone;
    @FXML private TableColumn<Distribution, String> colDistNid;

    @FXML private Button btnSave;
    @FXML private Button btnAdd;
    @FXML private Button btnClear;

    // ================= DATA =================
    private ObservableList<Distribution> stagedData = FXCollections.observableArrayList();
    private Map<String, Assignment> assignmentMap = new HashMap<>();
    private Set<String> usedAssets = new HashSet<>();
    private File selectedFile;

    private int requiredQty = 0;
    private Assignment selectedAssignment = null;

    // ================= INIT =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        setupTable();
        loadAssignments();

        // Defensive check
        if (equipmentCombo != null) {
            loadAvailableEquipment();
        } else {
            System.out.println("ERROR: equipmentCombo not injected from FXML");
        }

        btnSave.setDisable(true);

        cmbAssignments.setOnAction(e -> loadAssignmentDetails());
    }

    // ================= TABLE =================
    private void setupTable() {
        colDistAsset.setCellValueFactory(cell -> cell.getValue().assetCodeProperty());
        colDistName.setCellValueFactory(cell -> cell.getValue().assignedToProperty());
        colDistPhone.setCellValueFactory(cell -> cell.getValue().phoneProperty());
        colDistNid.setCellValueFactory(cell -> cell.getValue().nidProperty());

        distributionTable.setItems(stagedData);
    }

    // ================= LOAD ASSIGNMENTS =================
    private void loadAssignments() {
        cmbAssignments.getItems().clear();
        assignmentMap.clear();

        List<Assignment> list = DatabaseHandler.getAssignments();

        for (Assignment a : list) {
            String label = a.getPerson() + " (" + a.getEquipmentType() + ")";
            cmbAssignments.getItems().add(label);
            assignmentMap.put(label, a);
        }
    }

    // ================= LOAD EQUIPMENT =================
    private void loadAvailableEquipment() {

        if (equipmentCombo == null) return;

        equipmentCombo.getItems().clear();

        List<String> equipmentList = DatabaseHandler.getAvailableEquipment();

        if (equipmentList != null) {
            equipmentCombo.getItems().addAll(equipmentList);
        }
    }

    // ================= LOAD DETAILS =================
    private void loadAssignmentDetails() {
        selectedAssignment = assignmentMap.get(cmbAssignments.getValue());
        if (selectedAssignment == null) return;

        requiredQty = selectedAssignment.getQuantity();

        lblSelectedAssignment.setText(selectedAssignment.getPerson());
        stagedData.clear();
        usedAssets.clear();

        updateProgress();
    }

    // ================= ADD =================
    @FXML
    private void addManualAssignment(ActionEvent event) {

        if (selectedAssignment == null) {
            showWarning("Select assignment first");
            return;
        }

        if (stagedData.size() >= requiredQty) {
            showWarning("Limit reached");
            return;
        }

        if (equipmentCombo.getValue() == null) {
            showWarning("Select equipment");
            return;
        }

        if (txtName.getText().isEmpty() ||
            txtPhone.getText().isEmpty() ||
            txtNid.getText().isEmpty()) {

            showWarning("Fill all fields");
            return;
        }

        String assetCode = equipmentCombo.getValue();

        if (usedAssets.contains(assetCode)) {
            showWarning("Equipment already used");
            return;
        }

        stagedData.add(new Distribution(
                0,
                assetCode,
                "",
                txtName.getText(),
                txtPhone.getText(),
                txtNid.getText(),
                LocalDate.now()
        ));

        usedAssets.add(assetCode);

        clearFields();
        updateProgress();
    }

    private void clearFields() {
        txtName.clear();
        txtPhone.clear();
        txtNid.clear();
        equipmentCombo.setValue(null);
    }

    // ================= SAVE =================
    @FXML
    private void saveEquipment(ActionEvent event) {

        if (selectedAssignment == null) {
            showError("Select assignment first");
            return;
        }

        if (stagedData.size() != requiredQty) {
            showError("Incomplete entries");
            return;
        }

        try {
            for (Distribution d : stagedData) {
                DatabaseHandler.distributeEquipment(
                        d.getAssetCode(),
                        selectedAssignment.getId(),
                        d.getAssignedTo(),
                        d.getPhone(),
                        d.getNid()
                );
            }

            showInfo("Saved successfully");

            stagedData.clear();
            usedAssets.clear();
            updateProgress();
            loadAvailableEquipment();

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ================= PROGRESS =================
    private void updateProgress() {

        int entered = stagedData.size();
        int remaining = requiredQty - entered;

        lblProgress.setText("Assigned: " + entered + " / " + requiredQty);

        lblAssignmentStats.setText(
                "Required: " + requiredQty +
                " | Entered: " + entered +
                " | Remaining: " + remaining
        );

        btnAdd.setDisable(entered >= requiredQty);
        btnSave.setDisable(entered != requiredQty);
    }

    // ================= ALERTS =================
    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showWarning(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    // ================= FXML ACTIONS =================
    @FXML
    private void clearForm() {
        clearFields();
    }

    @FXML
    private void chooseExcelFile() {

FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select Excel File");

    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
    );

    selectedFile = fileChooser.showOpenDialog(null);

    if (selectedFile != null) {
        lblSelectedFile.setText(selectedFile.getName());
    }
    }

    @FXML
    private void uploadExcel() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Upload");
        alert.setHeaderText(null);
        alert.setContentText("Excel upload not implemented yet.");
        alert.showAndWait();
    }
    
}