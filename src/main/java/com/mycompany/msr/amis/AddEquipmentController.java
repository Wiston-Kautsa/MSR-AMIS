package com.mycompany.msr.amis;

import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

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

        cmbCategory.setEditable(true);
        cmbCondition.getItems().addAll("New", "Used", "Good", "Fair", "Damaged", "Faulty");

        dateEntry.setValue(LocalDate.now());

        loadEquipmentFromDatabase();
        loadCategories();
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

    private void loadCategories() {
        cmbCategory.getItems().clear();
        cmbCategory.getItems().addAll(DatabaseHandler.getEquipmentCategories());
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
            loadCategories();
            showInfo("Equipment saved successfully");
            clearForm(null);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to save equipment: " + e.getMessage());
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
        FileLocationHelper.useDownloadsDirectory(chooser);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        selectedFile = chooser.showOpenDialog(null);

        if (selectedFile != null) {
            lblSelectedFile.setText(selectedFile.getName());
            OperationFeedbackHelper.showInfo(
                    "File Selected",
                    "Ready to upload:\n" + selectedFile.getName() +
                            "\n\nClick Upload to import the equipment records."
            );
        } else {
            OperationFeedbackHelper.showWarning(
                    "No File Selected",
                    "No Excel file was selected."
            );
        }
    }

    @FXML
    private void downloadTemplate(ActionEvent event) {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Bulk Enrolment Template");
        chooser.setInitialFileName("equipment_bulk_template.xlsx");
        FileLocationHelper.useDownloadsDirectory(chooser);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        File targetFile = chooser.showSaveDialog(null);

        if (targetFile == null) {
            OperationFeedbackHelper.showWarning(
                    "Download Cancelled",
                    "Template download was cancelled."
            );
            return;
        }

        OperationFeedbackHelper.showInfo(
                "Preparing Template",
                "Creating the bulk equipment template in Downloads."
        );

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Equipment Template");

            String[] headers = {
                    "name",
                    "category",
                    "imei_serial_number",
                    "source",
                    "condition"
            };

            String[] sample = {
                    "Tablet",
                    "Tablet",
                    "SN-001",
                    "World Bank",
                    "New"
            };

            Row headerRow = sheet.createRow(0);
            Row sampleRow = sheet.createRow(1);

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(headers[i]);
                headerCell.setCellStyle(headerStyle);

                sampleRow.createCell(i).setCellValue(sample[i]);
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(targetFile)) {
                wb.write(out);
            }

            OperationFeedbackHelper.showInfo(
                    "Template Ready",
                    "Template downloaded successfully to:\n" + targetFile.getAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError(
                    "Download Failed",
                    "Failed to download the template."
            );
        }
    }

    // ================= UPLOAD =================
    @FXML
    private void uploadExcel() {

        if (selectedFile == null) {
            OperationFeedbackHelper.showWarning(
                    "Upload Blocked",
                    "Select an Excel file first before uploading."
            );
            return;
        }

        OperationFeedbackHelper.showInfo(
                "Upload Starting",
                "Reading equipment data from:\n" + selectedFile.getName()
        );

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(selectedFile))) {

            Sheet sheet = wb.getSheetAt(0);

            if (sheet.getRow(0) == null || sheet.getRow(0).getLastCellNum() < 5) {
                OperationFeedbackHelper.showError(
                    "Invalid File",
                    "The Excel file must contain 5 columns."
                );
                return;
            }

            int inserted = 0;

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
                        LocalDate.now().toString()
                );

                try {
                    DatabaseHandler.insertEquipment(eq);
                    inserted++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            loadEquipmentFromDatabase();
            loadCategories();
            OperationFeedbackHelper.showInfo(
                    "Upload Complete",
                    "Equipment upload completed.\n\nImported records: " + inserted
            );

        } catch (Exception e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError(
                "Upload Failed",
                "Error reading the Excel file.\n\n" + e.getMessage()
            );
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
