package com.mycompany.msr.amis;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {

    // ================= DATABASE =================
    private static final String URL = "jdbc:sqlite:msr_amis.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // ================= INIT DATABASE =================
    public static void initializeDatabase() {

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // ================= EQUIPMENT =================
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS equipment (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT UNIQUE, " +
                            "name TEXT, " +
                            "category TEXT, " +
                            "serial_number TEXT UNIQUE, " +
                            "condition TEXT, " +
                            "source TEXT, " +
                            "entry_date TEXT, " +
                            "status TEXT DEFAULT 'AVAILABLE'" +
                    ")"
            );

            // ================= ASSIGNMENTS =================
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS assignments (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "person TEXT, " +
                            "department TEXT, " +
                            "equipment_type TEXT, " +
                            "quantity INTEGER, " +
                            "date TEXT" +
                    ")"
            );

            // ================= DISTRIBUTION =================
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS distribution (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT, " +
                            "assignment_id INTEGER, " +
                            "assigned_to TEXT, " +
                            "phone TEXT, " +
                            "nid TEXT, " +
                            "issued_date TEXT, " +
                            "returned INTEGER DEFAULT 0" +
                    ")"
            );

            // ================= RETURNS =================
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS returns (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT, " +
                            "assignment_id INTEGER, " +
                            "returned_by TEXT, " +
                            "phone TEXT, " +
                            "nid TEXT, " +
                            "condition TEXT, " +
                            "remarks TEXT, " +
                            "return_date TEXT" +
                    ")"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= INSERT ASSIGNMENT =================
    public static void insertAssignment(String person, String dept, String type, int qty) throws Exception {

        String sql = "INSERT INTO assignments (person, department, equipment_type, quantity, date) " +
                     "VALUES (?, ?, ?, ?, DATE('now'))";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, person);
            ps.setString(2, dept);
            ps.setString(3, type);
            ps.setInt(4, qty);

            ps.executeUpdate();
        }
    }

    // ================= UPDATE ASSIGNMENT =================
    public static void updateAssignment(int id, String person, String dept, String type, int qty) throws Exception {

        String sql = "UPDATE assignments " +
                     "SET person=?, department=?, equipment_type=?, quantity=? " +
                     "WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, person);
            ps.setString(2, dept);
            ps.setString(3, type);
            ps.setInt(4, qty);
            ps.setInt(5, id);

            ps.executeUpdate();
        }
    }

    // ================= DELETE ASSIGNMENT =================
    public static void deleteAssignment(int id) throws Exception {

        String sql = "DELETE FROM assignments WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ================= GET ASSIGNMENTS =================
    public static List<Assignment> getAssignments() {

        List<Assignment> list = new ArrayList<>();

        String sql = "SELECT * FROM assignments ORDER BY id DESC";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                Assignment a = new Assignment(
                        rs.getInt("id"),
                        rs.getString("person"),
                        rs.getString("department"),
                        rs.getString("equipment_type"),
                        rs.getInt("quantity"),
                        rs.getString("date")
                );

                list.add(a);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ================= LOCK CHECK =================
    public static boolean isAssignmentLocked(int id) {
        // You can later check distribution table
        return false;
    }

    // ================= INVENTORY =================
    public static int getAvailableStock(String type) {

        int total = 0;
        int assigned = 0;

        try (Connection conn = getConnection()) {

            // Total equipment
            String totalSql = "SELECT COUNT(*) FROM equipment WHERE category=?";
            try (PreparedStatement ps = conn.prepareStatement(totalSql)) {

                ps.setString(1, type);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    total = rs.getInt(1);
                }
            }

            // Assigned (real issued items)
            String assignedSql =
                    "SELECT COUNT(*) FROM distribution d " +
                    "JOIN assignments a ON d.assignment_id = a.id " +
                    "WHERE a.equipment_type=? AND d.returned=0";

            try (PreparedStatement ps = conn.prepareStatement(assignedSql)) {

                ps.setString(1, type);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    assigned = rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return total - assigned;
    }

    // ================= ADD EQUIPMENT =================
    public static void insertEquipment(String name, String category, String serial,
                                       String condition, String source, String date) throws Exception {

        String assetCode = "AST-" + System.currentTimeMillis();

        String sql = "INSERT INTO equipment (asset_code, name, category, serial_number, condition, source, entry_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, assetCode);
            ps.setString(2, name);
            ps.setString(3, category);
            ps.setString(4, serial);
            ps.setString(5, condition);
            ps.setString(6, source);
            ps.setString(7, date);
            ps.setString(8, "AVAILABLE");

            ps.executeUpdate();
        }
    }

    // ================= CHECK SERIAL =================
    public static boolean serialExists(String serial) {

        String sql = "SELECT COUNT(*) FROM equipment WHERE serial_number=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, serial);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // ================= GET ASSIGNMENT ENTRY =================
    public static List<Assignment> getAssignmentEntries(int assignmentId) {

        List<Assignment> list = new ArrayList<>();

        String sql = "SELECT * FROM assignments WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Assignment a = new Assignment(
                        rs.getInt("id"),
                        rs.getString("person"),
                        rs.getString("department"),
                        rs.getString("equipment_type"),
                        rs.getInt("quantity"),
                        rs.getString("date")
                );

                list.add(a);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ================= RETURN EQUIPMENT =================
    public static void returnEquipment(String assetCode, String returnedBy,
                                       String phone, String nid,
                                       String condition, String remarks) throws Exception {

        try (Connection conn = getConnection()) {

            conn.setAutoCommit(false);

            // Insert return record
            String insertReturn =
                    "INSERT INTO returns (asset_code, returned_by, phone, nid, condition, remarks, return_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, DATE('now'))";

            try (PreparedStatement ps = conn.prepareStatement(insertReturn)) {

                ps.setString(1, assetCode);
                ps.setString(2, returnedBy);
                ps.setString(3, phone);
                ps.setString(4, nid);
                ps.setString(5, condition);
                ps.setString(6, remarks);

                ps.executeUpdate();
            }

            // Update equipment
            String updateEquipment =
                    "UPDATE equipment SET status='AVAILABLE' WHERE asset_code=?";

            try (PreparedStatement ps = conn.prepareStatement(updateEquipment)) {

                ps.setString(1, assetCode);
                ps.executeUpdate();
            }

            // Update distribution
            String updateDistribution =
                    "UPDATE distribution SET returned=1 WHERE asset_code=?";

            try (PreparedStatement ps = conn.prepareStatement(updateDistribution)) {

                ps.setString(1, assetCode);
                ps.executeUpdate();
            }

            conn.commit();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}