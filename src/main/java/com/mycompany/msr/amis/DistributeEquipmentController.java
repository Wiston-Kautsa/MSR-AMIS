package com.mycompany.msr.amis;

import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellType;

public class DistributeEquipmentController implements Initializable {

    // ================= UI =================
    @FXML private ComboBox<String> cmbAssignments;
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


    // ================= DATA =================
    private ObservableList<Distribution> stagedData = FXCollections.observableArrayList();
    private Map<String, Assignment> assignmentMap = new HashMap<>();
    private Set<String> usedAssets = new HashSet<>();

    private int requiredQty = 0;
    private Assignment selectedAssignment = null;
    private File selectedFile;
    @FXML
    private Button btnClear;

    // ================= INIT =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadAssignments();
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

    // ================= ADD MANUAL =================
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

        if (txtName.getText().isEmpty() ||
            txtPhone.getText().isEmpty() ||
            txtNid.getText().isEmpty()) {

            showWarning("Fill all fields");
            return;
        }

        String assetCode = "MANUAL-" + (stagedData.size() + 1);

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
    }

    // ================= FILE =================
    @FXML
    private void chooseExcelFile(ActionEvent event) {

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        selectedFile = chooser.showOpenDialog(null);

        if (selectedFile != null) {
            lblSelectedFile.setText(selectedFile.getName());
        }
    }

    // ================= UPLOAD =================
    @FXML
    private void uploadExcel() {

        if (selectedAssignment == null) {
            showWarning("Select assignment first");
            return;
        }

        if (selectedFile == null) {
            showWarning("Select file first");
            return;
        }

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(selectedFile))) {

            Sheet sheet = wb.getSheetAt(0);

            if (sheet.getRow(0).getLastCellNum() < 5) {
                showError("Invalid Excel format (need 5 columns)");
                return;
            }

            stagedData.clear();
            usedAssets.clear();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row r = sheet.getRow(i);
                if (r == null) continue;

                String asset = getCell(r, 0);

                if (usedAssets.contains(asset)) continue;

                stagedData.add(new Distribution(
                        0,
                        asset,
                        getCell(r, 1),
                        getCell(r, 2),
                        getCell(r, 3),
                        getCell(r, 4),
                        LocalDate.now()
                ));

                usedAssets.add(asset);
            }

            updateProgress();

        } catch (Exception e) {
            showError("Error reading Excel file");
        }
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

        // TODO: Save to DB
        showInfo("Saved successfully (implement DB next)");
    }

    // ================= UTIL =================
    private String getCell(Row row, int index) {

        Cell cell = row.getCell(index);
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                double v = cell.getNumericCellValue();
                return (v == (long) v) ? String.valueOf((long) v) : String.valueOf(v);
            default: return "";
        }
    }

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
    
    @FXML
private void clearForm() {
    txtName.clear();
    txtPhone.clear();
    txtNid.clear();
}
}