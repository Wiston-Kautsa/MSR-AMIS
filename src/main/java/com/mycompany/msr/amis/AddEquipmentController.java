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
import java.util.ResourceBundle;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class AddEquipmentController implements Initializable {

    // ================= UI =================
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField txtSerialNumber;
    @FXML private TextField txtSource;
    @FXML private ComboBox<String> cmbCondition;
    @FXML private DatePicker dateEntry;

    @FXML private Label lblSelectedFile;

    @FXML private TableView<Equipment> equipmentTable;
    @FXML private TableColumn<Equipment, String> colName;
    @FXML private TableColumn<Equipment, String> colCategory;
    @FXML private TableColumn<Equipment, String> colSerial;
    @FXML private TableColumn<Equipment, String> colCondition;
    @FXML private TableColumn<Equipment, String> colDate;

    // ================= DATA =================
    private ObservableList<Equipment> equipmentList = FXCollections.observableArrayList();
    private File selectedFile;

    // ================= INIT =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        setupTable();

        cmbCategory.getItems().addAll("Tablet", "Laptop", "Phone");
        cmbCondition.getItems().addAll("New", "Used", "Damaged");

        dateEntry.setValue(LocalDate.now());

        loadEquipmentFromDatabase();
    }

    // ================= TABLE =================
    private void setupTable() {

        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colCategory.setCellValueFactory(cell -> cell.getValue().categoryProperty());
        colSerial.setCellValueFactory(cell -> cell.getValue().serialNumberProperty());
        colCondition.setCellValueFactory(cell -> cell.getValue().conditionProperty());
        colDate.setCellValueFactory(cell -> cell.getValue().entryDateProperty());

        equipmentTable.setItems(equipmentList);
    }

    // ================= LOAD =================
    private void loadEquipmentFromDatabase() {
        equipmentList.clear();
        equipmentList.addAll(DatabaseHandler.getAllEquipment());
    }

    // ================= SAVE =================
    @FXML
    private void saveEquipment(ActionEvent event) {

        if (txtName.getText().isEmpty()
                || cmbCategory.getValue() == null
                || txtSerialNumber.getText().isEmpty()
                || cmbCondition.getValue() == null
                || dateEntry.getValue() == null) {

            showWarning("Please fill all required fields");
            return;
        }

        Equipment eq = new Equipment(
                txtName.getText(),
                cmbCategory.getValue(),
                txtSerialNumber.getText(),
                txtSource.getText(),
                cmbCondition.getValue(),
                dateEntry.getValue().toString()
        );

        try {
            DatabaseHandler.insertEquipment(eq);
            loadEquipmentFromDatabase();
            showInfo("Equipment saved successfully");
            clearForm(null);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to save equipment");
        }
    }

    // ================= CLEAR =================
    @FXML
    private void clearForm(ActionEvent event) {

        txtName.clear();
        txtSerialNumber.clear();
        txtSource.clear();

        cmbCategory.getSelectionModel().clearSelection();
        cmbCondition.getSelectionModel().clearSelection();

        dateEntry.setValue(LocalDate.now());
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

        if (selectedFile == null) {
            showWarning("Select file first");
            return;
        }

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(selectedFile))) {

            Sheet sheet = wb.getSheetAt(0);

            if (sheet.getRow(0).getLastCellNum() < 6) {
                showError("Invalid Excel format (must have 6 columns)");
                return;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row r = sheet.getRow(i);
                if (r == null) continue;

                String name = getCell(r, 0);
                String category = getCell(r, 1);
                String serial = getCell(r, 2);

                // skip empty rows
                if (name.isEmpty() || category.isEmpty() || serial.isEmpty()) continue;

                Equipment eq = new Equipment(
                        name,
                        category,
                        serial,
                        getCell(r, 3),
                        getCell(r, 4),
                        getCell(r, 5)
                );

                try {
                    DatabaseHandler.insertEquipment(eq);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            loadEquipmentFromDatabase();
            showInfo("Excel uploaded successfully");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error reading Excel file");
        }
    }

    // ================= UTIL =================
    private String getCell(Row row, int index) {

        Cell cell = row.getCell(index);
        if (cell == null) return "";

        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }

        if (cell.getCellType() == CellType.NUMERIC) {

            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().toString();
            }

            double v = cell.getNumericCellValue();
            return (v == (long) v)
                    ? String.valueOf((long) v)
                    : String.valueOf(v);
        }

        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }

        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getCellFormula();
        }

        return "";
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
}