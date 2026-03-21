package com.mycompany.msr.amis;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {

    // ================= DATABASE =================
    private static final String URL = "jdbc:sqlite:amis.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // ================= INIT DATABASE =================
public static void initializeDatabase() {

    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement()) {

        // Equipment table
        stmt.execute(
                "CREATE TABLE IF NOT EXISTS equipment (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT, " +
                        "category TEXT, " +
                        "serial_number TEXT UNIQUE, " +
                        "condition TEXT, " +
                        "source TEXT, " +
                        "entry_date TEXT" +
                ")"
        );

        // Assignment table
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
        // Not implemented yet (no distribution module)
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

            // Assigned quantity
            String assignedSql = "SELECT COALESCE(SUM(quantity),0) FROM assignments WHERE equipment_type=?";
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

    String sql = "INSERT INTO equipment (name, category, serial_number, condition, source, entry_date) " +
                 "VALUES (?, ?, ?, ?, ?, ?)";

    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, name);
        ps.setString(2, category);
        ps.setString(3, serial);
        ps.setString(4, condition);
        ps.setString(5, source);
        ps.setString(6, date);

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
}