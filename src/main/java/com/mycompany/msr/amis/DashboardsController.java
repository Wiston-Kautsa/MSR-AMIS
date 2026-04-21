package com.mycompany.msr.amis;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DashboardsController implements Initializable {

    @FXML private StackPane contentArea;

    @FXML private Label lblTotalAssets;
    @FXML private Label lblAvailableAssets;
    @FXML private Label lblBorrowedThisMonth;
    @FXML private Label lblBorrowedBreakdown;
    @FXML private Label lblReturnedAssets;
    @FXML private Label lblUtilizationRate;
    @FXML private Label lblAvailabilityRate;
    @FXML private Button btnBackupSync;
    @FXML private Button btnAuditLogs;
    @FXML private Button btnUsers;
    @FXML private PieChart assetStatusPieChart;
    @FXML private ProgressBar progressUtilization;
    @FXML private ProgressBar progressAvailability;
    @FXML private VBox alertsContainer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (btnUsers != null) {
            btnUsers.setManaged(AccessControl.canManageUsers());
            btnUsers.setVisible(AccessControl.canManageUsers());
        }
        if (btnBackupSync != null) {
            boolean allowed = Session.hasRole(AccessControl.ROLE_SUPER_ADMIN, AccessControl.ROLE_ADMIN);
            btnBackupSync.setManaged(allowed);
            btnBackupSync.setVisible(allowed);
        }
        if (btnAuditLogs != null) {
            btnAuditLogs.setManaged(AccessControl.canViewAuditLogs());
            btnAuditLogs.setVisible(AccessControl.canViewAuditLogs());
        }
        if (lblTotalAssets != null) {
            refreshDashboard();
        }
    }

    private void loadPage(String fxml) {
        try {
            String path = "/com/mycompany/msr/amis/" + fxml;
            URL resource = getClass().getResource(path);

            if (resource == null) {
                OperationFeedbackHelper.showError("Navigation Error", "The requested page could not be loaded.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            contentArea.getChildren().clear();

            if (root instanceof Region) {
                Region region = (Region) root;
                region.prefWidthProperty().bind(contentArea.widthProperty());
                region.prefHeightProperty().bind(contentArea.heightProperty());
            }

            contentArea.getChildren().add(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openDashboard(ActionEvent event) {
        try {
            App.showDashboardPage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshDashboard() {
        int totalAssets = getTotalAssets();
        int availableAssets = getAvailableAssets();
        int borrowedThisMonth = getBorrowedThisMonth();
        int stillInUseFromBorrowedThisMonth = getStillInUseFromBorrowedThisMonth();
        int returnedThisMonth = getReturnedThisMonth();

        lblTotalAssets.setText(String.valueOf(totalAssets));
        lblAvailableAssets.setText(String.valueOf(availableAssets));
        lblBorrowedThisMonth.setText(String.valueOf(borrowedThisMonth));
        if (lblBorrowedBreakdown != null) {
            lblBorrowedBreakdown.setText(
                    "Returned: " + returnedThisMonth + "  |  Still In Use: " + stillInUseFromBorrowedThisMonth
            );
        }
        lblReturnedAssets.setText(String.valueOf(returnedThisMonth));

        double utilizationRate = totalAssets == 0 ? 0 : (double) getAssetsInUse() / totalAssets;
        double availabilityRate = totalAssets == 0 ? 0 : (double) availableAssets / totalAssets;

        lblUtilizationRate.setText(String.format("%.0f%%", utilizationRate * 100));
        lblAvailabilityRate.setText(String.format("%.0f%%", availabilityRate * 100));
        progressUtilization.setProgress(utilizationRate);
        progressAvailability.setProgress(availabilityRate);

        loadAssetStatusChart(availableAssets, borrowedThisMonth, returnedThisMonth);
        loadAlerts(borrowedThisMonth, returnedThisMonth);
    }

    private int getTotalAssets() {
        return executeCountQuery("SELECT COUNT(*) FROM equipment");
    }

    private int getAvailableAssets() {
        return executeCountQuery(
                "SELECT COUNT(*) FROM equipment " +
                        "WHERE asset_code NOT IN (" +
                        "SELECT asset_code FROM distribution WHERE returned = 0" +
                        ") " +
                        "AND asset_code NOT IN (" +
                        "SELECT asset_code FROM returns" +
                        ")"
        );
    }

    private int getAssetsInUse() {
        return executeCountQuery(
                "SELECT COUNT(DISTINCT e.asset_code) " +
                        "FROM equipment e " +
                        "WHERE e.asset_code IN (" +
                        "SELECT d.asset_code FROM distribution d WHERE d.returned = 0" +
                        ")"
        );
    }

    private int getBorrowedThisMonth() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        String sql = "SELECT COUNT(*) FROM distribution WHERE DATE(date) >= ? AND DATE(date) <= DATE('now')";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, monthStart.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getReturnedThisMonth() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        String sql = "SELECT COUNT(*) FROM returns WHERE DATE(return_date) >= ? AND DATE(return_date) <= DATE('now')";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, monthStart.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getStillInUseFromBorrowedThisMonth() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        String sql =
                "SELECT COUNT(*) FROM distribution " +
                        "WHERE DATE(date) >= ? AND DATE(date) <= DATE('now') AND returned = 0";

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, monthStart.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int executeCountQuery(String sql) {
        try (Connection conn = DatabaseHandler.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void loadAssetStatusChart(int availableAssets, int borrowedThisMonth, int returnedThisMonth) {
        if (assetStatusPieChart == null) {
            return;
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        if (availableAssets > 0) {
            data.add(new PieChart.Data("Available", availableAssets));
        }
        if (borrowedThisMonth > 0) {
            data.add(new PieChart.Data("Borrowed This Month", borrowedThisMonth));
        }
        if (returnedThisMonth > 0) {
            data.add(new PieChart.Data("Returned This Month", returnedThisMonth));
        }
        if (data.isEmpty()) {
            data.add(new PieChart.Data("No Data", 1));
        }

        assetStatusPieChart.setLabelsVisible(true);
        assetStatusPieChart.setData(data);
    }

    private void loadAlerts(int borrowedThisMonth, int returnedThisMonth) {
        if (alertsContainer == null) {
            return;
        }

        alertsContainer.getChildren().clear();

        addAlertItem("Monthly borrowing", borrowedThisMonth + " asset(s) were borrowed this month.");

        int outstandingWithRemarks = executeCountQuery(
                "SELECT COUNT(*) FROM distribution " +
                        "WHERE returned = 0 AND outstanding_remarks IS NOT NULL AND TRIM(outstanding_remarks) <> ''"
        );
        if (outstandingWithRemarks > 0) {
            addAlertItem(
                    "Outstanding return reasons captured",
                    outstandingWithRemarks + " outstanding asset(s) already have return remarks recorded."
            );
        }

        if (returnedThisMonth == 0) {
            addAlertItem("Monthly returns", "No returns have been recorded yet this month.");
        }

        if (alertsContainer.getChildren().isEmpty()) {
            alertsContainer.getChildren().add(createEmptyState("No alerts right now."));
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private VBox createEmptyState(String text) {
        VBox item = new VBox();
        item.getStyleClass().add("dashboard-feed-item");
        Label label = new Label(text);
        label.getStyleClass().add("dashboard-feed-copy");
        label.setWrapText(true);
        item.getChildren().add(label);
        return item;
    }

    private void addAlertItem(String titleText, String bodyText) {
        VBox item = new VBox(4);
        item.getStyleClass().addAll("dashboard-feed-item", "dashboard-alert-item");

        Label title = new Label(titleText);
        title.getStyleClass().add("dashboard-feed-title");

        Label body = new Label(bodyText);
        body.getStyleClass().add("dashboard-feed-copy");
        body.setWrapText(true);

        item.getChildren().addAll(title, body);
        alertsContainer.getChildren().add(item);
    }

    @FXML
    private void handleRefreshDashboard(ActionEvent event) {
        refreshDashboard();
    }

    @FXML private void openAddEquipment() { loadPage("AddEquipment.fxml"); }
    @FXML private void openEquipmentList() { loadPage("EquipmentList.fxml"); }
    @FXML private void openCreateAssignment() { loadPage("CreateAssignment.fxml"); }
    @FXML private void openDistributeEquipment() { loadPage("DistributeEquipment.fxml"); }
    @FXML private void openAssignmentList() { loadPage("AssignmentList.fxml"); }
    @FXML private void openReturnEquipment() { loadPage("ReturnEquipment.fxml"); }
    @FXML private void openInventoryReport() { loadPage("InventoryReport.fxml"); }
    @FXML private void openAssignmentReport() { loadPage("AssignmentReport.fxml"); }
    @FXML private void openDistributionReport() { loadPage("DistributionReport.fxml"); }
    @FXML private void openAssetHistory() { loadPage("AssetHistory.fxml"); }
    @FXML private void openReturnEquipmentList() { loadPage("ReturnEquipmentList.fxml"); }
    @FXML private void openReturnReport() { loadPage("ReturnReport.fxml"); }
    @FXML private void openOutstandingReport() { loadPage("OutstandingReport.fxml"); }
    @FXML private void openBackupSync() {
        try {
            AccessControl.requireRole(AccessControl.ROLE_SUPER_ADMIN, AccessControl.ROLE_ADMIN);
            loadPage("BackupSync.fxml");
        } catch (SecurityException e) {
            OperationFeedbackHelper.showError("Access Denied", e.getMessage());
        }
    }
    @FXML private void openAuditLogs() {
        try {
            AccessControl.requireRole(AccessControl.ROLE_SUPER_ADMIN, AccessControl.ROLE_ADMIN);
            loadPage("AuditLogs.fxml");
        } catch (SecurityException e) {
            OperationFeedbackHelper.showError("Access Denied", e.getMessage());
        }
    }
    @FXML private void openUsers() {
        try {
            AccessControl.requireRole(AccessControl.ROLE_SUPER_ADMIN, AccessControl.ROLE_ADMIN);
            loadPage("Users.fxml");
        } catch (SecurityException e) {
            OperationFeedbackHelper.showError("Access Denied", e.getMessage());
        }
    }
    @FXML private void openAboutUs() { loadPage("AboutUs.fxml"); }

    @FXML
    private void openLogout(ActionEvent event) {
        try {
            Session.clear();
            App.showLoginPage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
