package com.mycompany.msr.amis;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DatabaseHandler {

    private static final String URL = "jdbc:sqlite:msr_amis.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    // ================= INIT DATABASE =================
    public static void initializeDatabase() {

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // EQUIPMENT
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS equipment (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT UNIQUE, " +
                            "name TEXT NOT NULL, " +
                            "category TEXT NOT NULL, " +
                            "serial_number TEXT UNIQUE NOT NULL, " +
                            "condition TEXT, " +
                            "source TEXT, " +
                            "entry_date TEXT DEFAULT (DATE('now')), " +
                            "status TEXT DEFAULT 'AVAILABLE' " +
                            "CHECK(status IN ('AVAILABLE','ASSIGNED','MAINTENANCE','RETIRED'))" +
                    ")"
            );

            // TRIGGER
            stmt.execute(
                    "CREATE TRIGGER IF NOT EXISTS generate_asset_code " +
                            "AFTER INSERT ON equipment " +
                            "BEGIN " +
                            "UPDATE equipment SET asset_code = " +
                            "CASE " +
                            "WHEN NEW.category = 'Laptop' THEN 'MSR-LTP-' || printf('%03d', NEW.id) " +
                            "WHEN NEW.category = 'Tablet' THEN 'MSR-TAB-' || printf('%03d', NEW.id) " +
                            "WHEN NEW.category = 'Phone' THEN 'MSR-PHN-' || printf('%03d', NEW.id) " +
                            "ELSE 'MSR-OTH-' || printf('%03d', NEW.id) " +
                            "END WHERE id = NEW.id; " +
                            "END;"
            );

            // ASSIGNMENTS
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS assignments (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "person TEXT, " +
                            "department TEXT, " +
                            "equipment_type TEXT, " +
                            "quantity INTEGER, " +
                            "date TEXT DEFAULT (DATE('now'))" +
                    ")"
            );

            // USERS
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "full_name TEXT NOT NULL, " +
                            "username TEXT UNIQUE NOT NULL, " +
                            "password TEXT NOT NULL, " +
                            "role TEXT DEFAULT 'USER', " +
                            "phone TEXT UNIQUE, " +
                            "email TEXT UNIQUE, " +
                            "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            );

            // ✅ DISTRIBUTION TABLE (correct placement)
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS distribution (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT, " +
                            "assignment_id INTEGER, " +
                            "name TEXT, " +
                            "phone TEXT, " +
                            "nid TEXT, " +
                            "date TEXT DEFAULT (DATE('now'))" +
                    ")"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= EQUIPMENT =================
    public static void insertEquipment(Equipment eq) throws Exception {

        String sql = "INSERT INTO equipment " +
                "(name, category, serial_number, condition, source, entry_date) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, eq.getName());
            ps.setString(2, eq.getCategory());
            ps.setString(3, eq.getSerialNumber());
            ps.setString(4, eq.getCondition());
            ps.setString(5, eq.getSource());
            ps.setString(6, eq.getEntryDate());

            ps.executeUpdate();
        }
    }

    public static ObservableList<Equipment> getAllEquipment() {

        ObservableList<Equipment> list = FXCollections.observableArrayList();

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM equipment")) {

            while (rs.next()) {
                list.add(new Equipment(
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("serial_number"),
                        rs.getString("source"),
                        rs.getString("condition"),
                        rs.getString("entry_date")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ================= ASSIGNMENTS =================
    public static void insertAssignment(String person, String dept, String type, int qty) throws Exception {

        String sql = "INSERT INTO assignments (person, department, equipment_type, quantity, date) VALUES (?, ?, ?, ?, DATE('now'))";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, person);
            ps.setString(2, dept);
            ps.setString(3, type);
            ps.setInt(4, qty);

            ps.executeUpdate();
        }
    }

    public static void deleteAssignment(int id) throws Exception {

        String sql = "DELETE FROM assignments WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static List<Assignment> getAssignments() {

        List<Assignment> list = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM assignments")) {

            while (rs.next()) {
                list.add(new Assignment(
                        rs.getInt("id"),
                        rs.getString("person"),
                        rs.getString("department"),
                        rs.getString("equipment_type"),
                        rs.getInt("quantity"),
                        rs.getString("date")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ================= USERS =================
    public static ObservableList<User> getUsers() {

        ObservableList<User> list = FXCollections.observableArrayList();

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users")) {

            while (rs.next()) {
                list.add(new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("phone"),
                        rs.getString("email")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void insertUser(String name, String username, String password,
                                  String role, String phone, String email) throws Exception {

        String sql = "INSERT INTO users (full_name, username, password, role, phone, email) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, role);
            ps.setString(5, phone);
            ps.setString(6, email);

            ps.executeUpdate();
        }
    }

    public static boolean emailExists(String email) {
        return exists("SELECT COUNT(*) FROM users WHERE email=?", email);
    }

    public static boolean phoneExists(String phone) {
        return exists("SELECT COUNT(*) FROM users WHERE phone=?", phone);
    }

    private static boolean exists(String sql, String value) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, value);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void deleteUser(int id) throws Exception {

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ================= STOCK =================
    public static int getAvailableStock(String type) {

        int available = 0;

        String sql =
            "SELECT COUNT(*) FROM equipment e " +
            "WHERE e.category = ? " +
            "AND e.asset_code NOT IN (SELECT asset_code FROM distribution)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                available = rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return available;
    }

    // ================= DISTRIBUTION =================
    public static void distributeEquipment(String assetCode,
                                           int assignmentId,
                                           String name,
                                           String phone,
                                           String nid) throws Exception {

        String checkSql = "SELECT COUNT(*) FROM distribution WHERE asset_code = ?";
        String insertSql = "INSERT INTO distribution (asset_code, assignment_id, name, phone, nid, date) " +
                           "VALUES (?, ?, ?, ?, ?, DATE('now'))";

        try (Connection conn = getConnection()) {

            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setString(1, assetCode);
            ResultSet rs = check.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                throw new Exception("Equipment already assigned");
            }

            PreparedStatement ps = conn.prepareStatement(insertSql);
            ps.setString(1, assetCode);
            ps.setInt(2, assignmentId);
            ps.setString(3, name);
            ps.setString(4, phone);
            ps.setString(5, nid);

            ps.executeUpdate();
        }
    }
    public static List<String> getAvailableEquipment() {

    List<String> list = new ArrayList<>();

    String sql =
        "SELECT asset_code FROM equipment " +
        "WHERE asset_code NOT IN (SELECT asset_code FROM distribution)";

    try (Connection conn = getConnection();
         Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {

        while (rs.next()) {
            list.add(rs.getString("asset_code"));
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return list;
}
}