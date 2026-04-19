package com.mycompany.msr.amis;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class ReturnEquipmentController implements Initializable {

    @FXML private ComboBox<String> cmbAssignments;
    @FXML private TextField txtReturnedBy;
    @FXML private TextField txtPhone;
    @FXML private TextField txtNid;
    @FXML private ComboBox<String> cmbCondition;
    @FXML private TextField txtRemarks;
    @FXML private Label lblAssignOfficer;
    @FXML private Label lblAssignType;
    @FXML private Label lblAssignQty;
    @FXML private Label lblReturnProgress;
    @FXML private TextField txtAssetCode;
    @FXML private TextField txtOfficer;
    @FXML private TextField txtEquipmentType;
    @FXML private TableView<Return> tableReturns;
    @FXML private TableColumn<Return, String> colAsset;
    @FXML private TableColumn<Return, String> colReturnedBy;
    @FXML private TableColumn<Return, String> colPhone;
    @FXML private TableColumn<Return, String> colCondition;
    @FXML private TableColumn<Return, String> colDate;
    @FXML private Label lblFileName;
    @FXML private Button btnSaveReturns;

    private final ObservableList<Return> returnHistoryList = FXCollections.observableArrayList();
    private final Map<String, Assignment> assignmentMap = new LinkedHashMap<>();
    private final List<String> outstandingAssetCodes = new ArrayList<>();
    private final Set<String> stagedResolvedAssetCodes = new LinkedHashSet<>();
    private final List<StagedReturnItem> stagedReturnItems = new ArrayList<>();
    private final DataFormatter dataFormatter = new DataFormatter();

    private Assignment selectedAssignment;
    private int requiredReturnQty = 0;
    private File selectedFile;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbCondition.getItems().addAll("Good", "Fair", "Damaged", "Faulty", "Lost");

        setupTable();
        loadAssignmentsPendingReturn();
        loadReturnHistory();
        clearAssignmentDetails();
        updateSaveState();

        txtReturnedBy.setEditable(false);
        cmbAssignments.setOnAction(e -> populateAssignmentDetails());
        txtAssetCode.textProperty().addListener((obs, oldValue, newValue) -> populateReturnedByForAsset(newValue));
    }

    private void setupTable() {
        if (colAsset != null) {
            colAsset.setCellValueFactory(new PropertyValueFactory<>("assetCode"));
            colReturnedBy.setCellValueFactory(new PropertyValueFactory<>("returnedBy"));
            colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
            colCondition.setCellValueFactory(new PropertyValueFactory<>("condition"));
            colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            tableReturns.setItems(returnHistoryList);
        }
    }

    private void loadAssignmentsPendingReturn() {
        if (cmbAssignments == null) {
            return;
        }

        cmbAssignments.getItems().clear();
        assignmentMap.clear();

        List<Assignment> assignments = DatabaseHandler.getAssignmentsPendingReturn();
        for (Assignment assignment : assignments) {
            int outstandingCount = DatabaseHandler.getDistributedCountForAssignment(assignment.getId());
            if (outstandingCount <= 0) {
                continue;
            }

            String label = assignment.getPerson() + " (" + assignment.getEquipmentType() + ")";
            cmbAssignments.getItems().add(label);
            assignmentMap.put(label, assignment);
        }
    }

    private void populateAssignmentDetails() {
        selectedAssignment = assignmentMap.get(cmbAssignments.getValue());
        stagedResolvedAssetCodes.clear();
        stagedReturnItems.clear();
        outstandingAssetCodes.clear();

        if (selectedAssignment == null) {
            requiredReturnQty = 0;
            selectedFile = null;
            if (lblFileName != null) {
                lblFileName.setText("No file selected");
            }
            clearAssignmentDetails();
            clearEntryFields();
            updateSaveState();
            return;
        }

        requiredReturnQty = DatabaseHandler.getDistributedCountForAssignment(selectedAssignment.getId());
        outstandingAssetCodes.addAll(DatabaseHandler.getOutstandingAssetCodesForAssignment(selectedAssignment.getId()));
        selectedFile = null;

        lblAssignOfficer.setText(selectedAssignment.getPerson());
        lblAssignType.setText(selectedAssignment.getEquipmentType());
        lblAssignQty.setText(String.valueOf(requiredReturnQty));
        if (lblFileName != null) {
            lblFileName.setText("No file selected");
        }

        if (txtOfficer != null) {
            txtOfficer.setText(selectedAssignment.getPerson());
        }
        if (txtEquipmentType != null) {
            txtEquipmentType.setText(selectedAssignment.getEquipmentType());
        }

        clearEntryFields();
        updateSaveState();
    }

    private void loadReturnHistory() {
        returnHistoryList.clear();
        try (java.sql.Connection conn = DatabaseHandler.getConnection();
             java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(
                     "SELECT asset_code, returned_by, phone, nid, condition, return_date FROM returns ORDER BY return_date DESC, id DESC")) {
            while (rs.next()) {
                returnHistoryList.add(new Return(
                        rs.getString("asset_code"),
                        rs.getString("returned_by"),
                        rs.getString("phone"),
                        rs.getString("nid"),
                        rs.getString("condition"),
                        rs.getString("return_date")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleReturn(ActionEvent event) {
        if (selectedAssignment == null) {
            showWarning("Select an assignment first.");
            return;
        }

        try {
            StagedReturnItem item = buildDraft(
                    txtAssetCode.getText().trim(),
                    txtPhone.getText().trim(),
                    txtNid.getText().trim(),
                    cmbCondition.getValue(),
                    txtRemarks.getText().trim()
            );

            stagedReturnItems.add(item);
            stagedResolvedAssetCodes.add(item.originalAssetCode);

            clearEntryFields();
            updateSaveState();
            if (btnSaveReturns != null) {
                btnSaveReturns.setDisable(false);
            }

        } catch (Exception e) {
            showWarning(e.getMessage());
        }
    }

    @FXML
    private void saveReturns(ActionEvent event) {
        if (selectedAssignment == null) {
            showWarning("Select an assignment first.");
            return;
        }
        if (stagedReturnItems.isEmpty()) {
            showWarning("Add at least one returned equipment entry before saving.");
            return;
        }

        if (!confirmSaveWithRemainingItems()) {
            return;
        }

        try {
            List<String> newAssetCodes = new ArrayList<>();

            for (StagedReturnItem item : stagedReturnItems) {
                String remarks = item.remarks;
                if (item.replacement) {
                    String newAssetCode = DatabaseHandler.insertReplacementEquipment(
                            selectedAssignment.getEquipmentType(),
                            item.enteredIdentifier,
                            "Replacement return for " + item.originalAssetCode
                    );
                    newAssetCodes.add(newAssetCode);
                    remarks = appendRemark(
                            item.remarks,
                            "Replacement equipment recorded as " + newAssetCode +
                                    " using IMEI/Serial " + item.enteredIdentifier
                    );
                }

                DatabaseHandler.returnEquipment(
                        item.originalAssetCode,
                        item.returnedBy,
                        item.phone,
                        item.nid,
                        item.condition,
                        remarks
                );
            }

            resetAfterSave();
            loadAssignmentsPendingReturn();
            loadReturnHistory();

            if (newAssetCodes.isEmpty()) {
                showInfo("Equipment returns saved successfully.");
            } else {
                showInfo(
                        "Equipment returns saved successfully.\n\nNew replacement asset codes:\n" +
                                String.join("\n", newAssetCodes)
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Return failed: " + e.getMessage());
        }
    }

    @FXML
    private void clearForm(ActionEvent event) {
        clearEntryFields();
        stagedReturnItems.clear();
        stagedResolvedAssetCodes.clear();
        selectedFile = null;

        if (lblFileName != null) {
            lblFileName.setText("No file selected");
        }

        if (selectedAssignment == null) {
            if (cmbAssignments != null) {
                cmbAssignments.setValue(null);
            }
            clearAssignmentDetails();
        } else {
            if (txtOfficer != null) {
                txtOfficer.setText(selectedAssignment.getPerson());
            }
            if (txtEquipmentType != null) {
                txtEquipmentType.setText(selectedAssignment.getEquipmentType());
            }
        }

        updateSaveState();
    }

    @FXML
    private void handleBulkUpload(ActionEvent event) {
        if (selectedAssignment == null) {
            showWarning("Select an assignment first.");
            return;
        }
        if (selectedFile == null) {
            showWarning("Choose an Excel file first.");
            return;
        }

        try (Workbook workbook = new XSSFWorkbook(new FileInputStream(selectedFile))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<StagedReturnItem> uploadedItems = new ArrayList<>();
            Set<String> uploadedResolvedAssets = new LinkedHashSet<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String identifier = getCellValue(row, 0);
                String phone = getCellValue(row, 1);
                String nid = getCellValue(row, 2);
                String condition = getCellValue(row, 3);
                String remarks = getCellValue(row, 4);

                if (identifier.isBlank() && phone.isBlank() && nid.isBlank() && condition.isBlank() && remarks.isBlank()) {
                    continue;
                }

                StagedReturnItem item = buildDraft(identifier, phone, nid, condition, remarks, uploadedResolvedAssets);
                uploadedItems.add(item);
                uploadedResolvedAssets.add(item.originalAssetCode);
            }

            if (uploadedItems.isEmpty()) {
                showWarning("The selected file does not contain any return rows.");
                return;
            }

            stagedReturnItems.clear();
            stagedReturnItems.addAll(uploadedItems);
            stagedResolvedAssetCodes.clear();
            stagedResolvedAssetCodes.addAll(uploadedResolvedAssets);
            updateSaveState();

            if (!confirmSaveWithRemainingItems()) {
                return;
            }

            saveReturns(null);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Bulk upload failed: " + e.getMessage());
        }
    }

    @FXML
    private void downloadTemplate(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Return Template");
        chooser.setInitialFileName("return_bulk_template.xlsx");
        FileLocationHelper.useDownloadsDirectory(chooser);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        File targetFile = chooser.showSaveDialog(null);
        if (targetFile == null) {
            OperationFeedbackHelper.showWarning(
                    "Download Cancelled",
                    "Return template download was cancelled."
            );
            return;
        }

        OperationFeedbackHelper.showInfo(
                "Preparing Template",
                "Creating the return bulk template in Downloads."
        );

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Return Template");

            String[] headers = {"asset_code_or_imei", "phone", "nid", "condition", "remarks"};
            String[] sample = {"MSR-LTP-001", "0991234567", "MW123456", "Good", "Returned to store"};

            Row headerRow = sheet.createRow(0);
            Row sampleRow = sheet.createRow(1);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
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
                workbook.write(out);
            }

            OperationFeedbackHelper.showInfo(
                    "Template Ready",
                    "Return template downloaded successfully to:\n" + targetFile.getAbsolutePath()
            );
        } catch (Exception e) {
            e.printStackTrace();
            OperationFeedbackHelper.showError(
                    "Download Failed",
                    "Failed to download the return template."
            );
        }
    }

    @FXML
    private void chooseFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        FileLocationHelper.useDownloadsDirectory(chooser);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );
        selectedFile = chooser.showOpenDialog(null);
        if (selectedFile != null && lblFileName != null) {
            lblFileName.setText(selectedFile.getName());
            OperationFeedbackHelper.showInfo(
                    "File Selected",
                    "Ready to upload return data from:\n" + selectedFile.getName()
            );
        } else {
            OperationFeedbackHelper.showWarning(
                    "No File Selected",
                    "No Excel file was selected for return upload."
            );
        }
    }

    private StagedReturnItem buildDraft(String identifier, String phone, String nid, String condition, String remarks)
            throws Exception {
        return buildDraft(identifier, phone, nid, condition, remarks, stagedResolvedAssetCodes);
    }

    private StagedReturnItem buildDraft(
            String identifier,
            String phone,
            String nid,
            String condition,
            String remarks,
            Set<String> reservedAssets
    ) throws Exception {
        if (identifier == null || identifier.isBlank()) {
            throw new Exception("Asset Code / New IMEI is required.");
        }
        if (condition == null || condition.isBlank()) {
            throw new Exception("Please select a condition.");
        }

        String trimmedIdentifier = identifier.trim();
        boolean directOutstandingReturn = outstandingAssetCodes.contains(trimmedIdentifier);
        String originalAssetCode;

        if (directOutstandingReturn) {
            originalAssetCode = trimmedIdentifier;
        } else {
            originalAssetCode = getNextOutstandingAssetCode(reservedAssets);
            if (originalAssetCode == null) {
                throw new Exception("There are no remaining outstanding assets available for replacement.");
            }
        }

        if (reservedAssets.contains(originalAssetCode)) {
            throw new Exception("This asset has already been entered in the current save batch.");
        }

        Distribution distribution = DatabaseHandler.getCurrentDistributionForAsset(originalAssetCode);
        String returnedBy = distribution == null ? "" : distribution.getAssignedTo();
        if (returnedBy.isBlank()) {
            throw new Exception("The assigned person for this asset could not be found.");
        }

        txtReturnedBy.setText(returnedBy);

        return new StagedReturnItem(
                originalAssetCode,
                trimmedIdentifier,
                returnedBy,
                phone,
                nid,
                condition,
                remarks,
                !directOutstandingReturn
        );
    }

    private String getNextOutstandingAssetCode(Set<String> reservedAssets) {
        for (String assetCode : outstandingAssetCodes) {
            if (!reservedAssets.contains(assetCode)) {
                return assetCode;
            }
        }
        return null;
    }

    private boolean confirmSaveWithRemainingItems() {
        List<String> remainingAssets = new ArrayList<>();
        for (String assetCode : outstandingAssetCodes) {
            if (!stagedResolvedAssetCodes.contains(assetCode)) {
                remainingAssets.add(assetCode);
            }
        }

        long replacementCount = stagedReturnItems.stream().filter(item -> item.replacement).count();
        if (remainingAssets.isEmpty() && replacementCount == 0) {
            return true;
        }

        StringBuilder message = new StringBuilder("Save the current returns now?");
        if (!remainingAssets.isEmpty()) {
            message.append("\n\nThese assets will remain outstanding:\n")
                    .append(String.join("\n", remainingAssets));
        }
        if (replacementCount > 0) {
            message.append("\n\n")
                    .append(replacementCount)
                    .append(" replacement item(s) will be added as new equipment and given new asset codes.");
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Save");
        confirmation.setHeaderText(null);
        confirmation.setContentText(message.toString());
        Optional<ButtonType> result = confirmation.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void showWarning(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private void clearAssignmentDetails() {
        lblAssignOfficer.setText("");
        lblAssignType.setText("");
        lblAssignQty.setText("0");
        if (txtOfficer != null) {
            txtOfficer.clear();
        }
        if (txtEquipmentType != null) {
            txtEquipmentType.clear();
        }
    }

    private void clearEntryFields() {
        txtAssetCode.clear();
        txtReturnedBy.clear();
        txtPhone.clear();
        txtNid.clear();
        txtRemarks.clear();
        cmbCondition.setValue(null);
    }

    private void updateSaveState() {
        if (btnSaveReturns != null) {
            btnSaveReturns.setDisable(selectedAssignment == null || stagedReturnItems.isEmpty());
        }
        if (lblReturnProgress != null) {
            lblReturnProgress.setText("Entered Returns: " + stagedReturnItems.size() + " / " + requiredReturnQty);
        }
    }

    private void resetAfterSave() {
        stagedReturnItems.clear();
        stagedResolvedAssetCodes.clear();
        outstandingAssetCodes.clear();
        selectedAssignment = null;
        requiredReturnQty = 0;
        selectedFile = null;
        if (lblFileName != null) {
            lblFileName.setText("No file selected");
        }
        if (cmbAssignments != null) {
            cmbAssignments.getSelectionModel().clearSelection();
        }
        clearAssignmentDetails();
        clearEntryFields();
        updateSaveState();
    }

    private void populateReturnedByForAsset(String assetCode) {
        if (txtReturnedBy == null) {
            return;
        }
        if (selectedAssignment == null || assetCode == null || assetCode.isBlank()) {
            txtReturnedBy.clear();
            return;
        }

        String trimmedAssetCode = assetCode.trim();
        String lookupAssetCode = outstandingAssetCodes.contains(trimmedAssetCode)
                ? trimmedAssetCode
                : getNextOutstandingAssetCode(stagedResolvedAssetCodes);

        if (lookupAssetCode == null) {
            txtReturnedBy.clear();
            return;
        }

        Distribution distribution = DatabaseHandler.getCurrentDistributionForAsset(lookupAssetCode);
        txtReturnedBy.setText(distribution == null ? "" : distribution.getAssignedTo());
    }

    private String getCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return dataFormatter.formatCellValue(cell).trim();
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private String appendRemark(String existingRemarks, String extraRemark) {
        if (existingRemarks == null || existingRemarks.isBlank()) {
            return extraRemark;
        }
        return existingRemarks + " | " + extraRemark;
    }

    private static final class StagedReturnItem {
        private final String originalAssetCode;
        private final String enteredIdentifier;
        private final String returnedBy;
        private final String phone;
        private final String nid;
        private final String condition;
        private final String remarks;
        private final boolean replacement;

        private StagedReturnItem(
                String originalAssetCode,
                String enteredIdentifier,
                String returnedBy,
                String phone,
                String nid,
                String condition,
                String remarks,
                boolean replacement
        ) {
            this.originalAssetCode = originalAssetCode;
            this.enteredIdentifier = enteredIdentifier;
            this.returnedBy = returnedBy;
            this.phone = phone;
            this.nid = nid;
            this.condition = condition;
            this.remarks = remarks;
            this.replacement = replacement;
        }
    }
}
