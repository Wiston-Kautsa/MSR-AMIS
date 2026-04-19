package com.mycompany.msr.amis;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DashboardsController implements Initializable {

    @FXML private StackPane contentArea;

    @FXML private Label lblAssetsEntered;
    @FXML private Label lblAvailableAssets;
    @FXML private Label lblIssuedAssets;
    @FXML private Label lblWaitingReturn;
    @FXML private PieChart equipmentPieChart;
    @FXML private PieChart borrowedPieChart;

    private final List<String> fixedCategories = Arrays.asList(
            "Laptop",
            "Tablet",
            "Phone",
            "Printer",
            "Projector",
            "Power Bank",
            "Other"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (lblAssetsEntered != null) {
            refreshDashboard();
        }
    }

    private void loadPage(String fxml) {
        try {
            String path = "/com/mycompany/msr/amis/" + fxml;
            URL resource = getClass().getResource(path);

            if (resource == null) {
                System.out.println("FXML not found: " + path);
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
        String path = "/com/mycompany/msr/amis/Dashboards.fxml";
        URL resource = getClass().getResource(path);

        if (resource == null) {
            System.out.println("Dashboards.fxml not found at: " + path);
            return;
        }

        try {
            Parent root = FXMLLoader.load(resource);

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshDashboard() {
        lblAssetsEntered.setText(String.valueOf(getTotalAssets()));
        lblAvailableAssets.setText(String.valueOf(getAvailableAssets()));
        lblIssuedAssets.setText(String.valueOf(getIssuedAssets()));
        lblWaitingReturn.setText(String.valueOf(getWaitingReturn()));
        loadAvailablePieChart();
        loadBorrowedPieChart();
    }

    private int getTotalAssets() {
        return executeCountQuery("SELECT COUNT(*) FROM equipment");
    }

    private int getAvailableAssets() {
        return executeCountQuery(
                "SELECT COUNT(*) FROM equipment " +
                        "WHERE asset_code NOT IN (" +
                        "SELECT asset_code FROM distribution WHERE returned = 0" +
                        ")"
        );
    }

    private int getIssuedAssets() {
        return executeCountQuery(
                "SELECT COUNT(DISTINCT e.asset_code) " +
                        "FROM equipment e " +
                        "WHERE e.asset_code IN (" +
                        "SELECT d.asset_code FROM distribution d WHERE d.returned = 0" +
                        ")"
        );
    }

    private int getWaitingReturn() {
        return executeCountQuery(
                "SELECT COUNT(DISTINCT d.asset_code) " +
                        "FROM distribution d " +
                        "WHERE d.returned = 0"
        );
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

    private void loadAvailablePieChart() {
        String sql =
                "SELECT " +
                        "CASE " +
                        "WHEN TRIM(COALESCE(e.category, '')) = '' THEN 'Other' " +
                        "WHEN LOWER(TRIM(e.category)) IN ('other', 'others') THEN 'Other' " +
                        "ELSE TRIM(e.category) " +
                        "END AS category, " +
                        "COUNT(*) AS total " +
                        "FROM equipment e " +
                        "WHERE e.asset_code NOT IN (" +
                        "SELECT asset_code FROM distribution WHERE returned = 0" +
                        ") " +
                        "GROUP BY " +
                        "CASE " +
                        "WHEN TRIM(COALESCE(e.category, '')) = '' THEN 'Other' " +
                        "WHEN LOWER(TRIM(e.category)) IN ('other', 'others') THEN 'Other' " +
                        "ELSE TRIM(e.category) " +
                        "END " +
                        "ORDER BY total DESC, category ASC";
        loadPieChartFromDatabase(equipmentPieChart, sql);
    }

    private void loadBorrowedPieChart() {
        String sql =
                "SELECT " +
                        "CASE " +
                        "WHEN TRIM(COALESCE(e.category, '')) = '' THEN 'Other' " +
                        "WHEN LOWER(TRIM(e.category)) IN ('other', 'others') THEN 'Other' " +
                        "ELSE TRIM(e.category) " +
                        "END AS category, " +
                        "COUNT(*) AS total " +
                        "FROM distribution d " +
                        "JOIN equipment e ON e.asset_code = d.asset_code " +
                        "WHERE d.returned = 0 " +
                        "GROUP BY " +
                        "CASE " +
                        "WHEN TRIM(COALESCE(e.category, '')) = '' THEN 'Other' " +
                        "WHEN LOWER(TRIM(e.category)) IN ('other', 'others') THEN 'Other' " +
                        "ELSE TRIM(e.category) " +
                        "END " +
                        "ORDER BY total DESC, category ASC";
        loadPieChartFromDatabase(borrowedPieChart, sql);
    }

    private void loadPieChartFromDatabase(PieChart chart, String sql) {
        if (chart == null) {
            return;
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        java.util.Map<String, Integer> dbData = new java.util.HashMap<>();

        try (Connection conn = DatabaseHandler.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                dbData.put(rs.getString("category"), rs.getInt("total"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String category : fixedCategories) {
            int count = dbData.getOrDefault(category, 0);
            data.add(new PieChart.Data(category, count == 0 ? 0.01 : count));
        }

        chart.setLabelsVisible(true);
        chart.setData(data);
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
    @FXML private void openReturnReport() { loadPage("ReturnReport.fxml"); }
    @FXML private void openOutstandingReport() { loadPage("OutstandingReport.fxml"); }
    @FXML private void openUsers() { loadPage("Users.fxml"); }
    @FXML private void openAboutUs() { loadPage("AboutUs.fxml"); }

    @FXML
    private void openLogout(ActionEvent event) {
        try {
            App.showLoginPage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
