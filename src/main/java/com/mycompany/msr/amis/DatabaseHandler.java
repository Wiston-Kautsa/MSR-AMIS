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

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS equipment (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT NOT NULL UNIQUE, " +
                            "name TEXT NOT NULL, " +
                            "category TEXT, " +
                            "serial_number TEXT, " +
                            "condition TEXT, " +
                            "source TEXT, " +
                            "entry_date TEXT, " +
                            "status TEXT CHECK(status IN ('AVAILABLE','ASSIGNED','MAINTENANCE','RETIRED')) DEFAULT 'AVAILABLE'" +
                    ")"
            );

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

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS distribution (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT NOT NULL, " +
                            "assignment_id INTEGER NOT NULL, " +
                            "assigned_to TEXT, " +
                            "phone TEXT, " +
                            "nid TEXT, " +
                            "issued_date TEXT, " +
                            "returned INTEGER DEFAULT 0, " +
                            "FOREIGN KEY (asset_code) REFERENCES equipment(asset_code), " +
                            "FOREIGN KEY (assignment_id) REFERENCES assignments(id)" +
                    ")"
            );

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS returns (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT NOT NULL, " +
                            "assignment_id INTEGER, " +
                            "returned_by TEXT, " +
                            "phone TEXT, " +
                            "nid TEXT, " +
                            "condition TEXT, " +
                            "remarks TEXT, " +
                            "return_date TEXT, " +
                            "FOREIGN KEY (asset_code) REFERENCES equipment(asset_code), " +
                            "FOREIGN KEY (assignment_id) REFERENCES assignments(id)" +
                    ")"
            );

            // ✅ USERS TABLE
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "full_name TEXT NOT NULL, " +
                            "username TEXT UNIQUE NOT NULL, " +
                            "password TEXT NOT NULL, " +
                            "role TEXT DEFAULT 'USER', " +
                            "phone TEXT UNIQUE, " +     // ✅ FIX
                            "email TEXT UNIQUE, " +     // ✅ FIX
                            "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public static void updateAssignment(int id, String person, String dept, String type, int qty) throws Exception {

        String sql = "UPDATE assignments SET person=?, department=?, equipment_type=?, quantity=? WHERE id=?";

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
        String sql = "SELECT * FROM assignments ORDER BY id DESC";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

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

    public static boolean isAssignmentLocked(int id) {

        String sql = "SELECT COUNT(*) FROM distribution WHERE assignment_id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt(1) > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // ================= USERS =================

    public static ObservableList<User> getUsers() {

        ObservableList<User> list = FXCollections.observableArrayList();

        String sql = "SELECT * FROM users ORDER BY id DESC";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

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

    // ✅ NEW VALIDATION METHODS
    public static boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean phoneExists(String phone) {
        String sql = "SELECT COUNT(*) FROM users WHERE phone=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void deleteUser(int id) throws Exception {

        String sql = "DELETE FROM users WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ================= STOCK =================
    public static int getAvailableStock(String type) {

        int available = 0;

        String sql = "SELECT COUNT(*) FROM equipment WHERE category=? AND status='AVAILABLE'";

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
}