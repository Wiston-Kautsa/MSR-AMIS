package com.mycompany.msr.amis;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                            "department TEXT, " +
                            "phone TEXT UNIQUE, " +
                            "email TEXT UNIQUE, " +
                            "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            );

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS departments (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT UNIQUE NOT NULL" +
                    ")"
            );

            // ✅ DISTRIBUTION TABLE (correct placement)
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS distribution (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT, " +
                            "assignment_id INTEGER, " +
                            "assigned_to TEXT, " +
                            "phone TEXT, " +
                            "nid TEXT, " +
                            "date TEXT DEFAULT (DATE('now')), " +
                            "returned INTEGER DEFAULT 0 " +
                            "CHECK(returned IN (0,1))" +
                    ")"
            );

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS returns (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "asset_code TEXT NOT NULL, " +
                            "returned_by TEXT NOT NULL, " +
                            "phone TEXT, " +
                            "nid TEXT, " +
                            "condition TEXT, " +
                            "remarks TEXT, " +
                            "return_date TEXT DEFAULT (DATE('now'))" +
                    ")"
            );

            migrateDatabase(conn);
            ensureDefaultAdmin(conn);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void migrateDatabase(Connection conn) throws SQLException {
        ensureColumn(conn, "users", "department", "TEXT");
        ensureColumn(conn, "distribution", "assigned_to", "TEXT");
        ensureColumn(conn, "distribution", "returned", "INTEGER DEFAULT 0 CHECK(returned IN (0,1))");
        ensureColumn(conn, "distribution", "date", "TEXT DEFAULT (DATE('now'))");
        ensureColumn(conn, "returns", "remarks", "TEXT");

        try (Statement stmt = conn.createStatement()) {
            if (hasColumn(conn, "distribution", "name")) {
                stmt.executeUpdate(
                        "UPDATE distribution " +
                        "SET assigned_to = name " +
                        "WHERE assigned_to IS NULL AND name IS NOT NULL"
                );
            }
            stmt.executeUpdate(
                    "UPDATE distribution " +
                    "SET returned = 0 " +
                    "WHERE returned IS NULL"
            );
            stmt.executeUpdate(
                    "INSERT OR IGNORE INTO departments(name) " +
                    "SELECT DISTINCT TRIM(department) FROM users " +
                    "WHERE department IS NOT NULL AND TRIM(department) <> ''"
            );
        }
    }

    private static void ensureDefaultAdmin(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users";
        String insertSql =
                "INSERT INTO users (full_name, username, password, role, department, phone, email) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Statement countStatement = conn.createStatement();
             ResultSet rs = countStatement.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String email = "admin@msr.local";
        String password = PasswordUtils.hash("admin123");

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            insertDepartmentIfMissing(conn, "Administration");
            ps.setString(1, "System Administrator");
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, "ADMIN");
            ps.setString(5, "Administration");
            ps.setString(6, null);
            ps.setString(7, email);
            ps.executeUpdate();
        }
    }

    private static void ensureColumn(Connection conn, String tableName, String columnName, String definition)
            throws SQLException {
        if (hasColumn(conn, tableName, columnName)) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private static boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + tableName + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
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

    public static String insertReplacementEquipment(String equipmentType, String serialNumber, String source) throws Exception {
        String insertSql = "INSERT INTO equipment (name, category, serial_number, condition, source, entry_date) VALUES (?, ?, ?, ?, ?, DATE('now'))";
        String selectSql = "SELECT asset_code FROM equipment WHERE serial_number = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement insert = conn.prepareStatement(insertSql);
                 PreparedStatement select = conn.prepareStatement(selectSql)) {

                insert.setString(1, equipmentType);
                insert.setString(2, equipmentType);
                insert.setString(3, serialNumber);
                insert.setString(4, "New");
                insert.setString(5, source);
                insert.executeUpdate();

                select.setString(1, serialNumber);
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        String assetCode = rs.getString("asset_code");
                        conn.commit();
                        return assetCode;
                    }
                }

                throw new Exception("Replacement equipment was saved but asset code could not be resolved.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static ObservableList<Equipment> getAllEquipment() {

        ObservableList<Equipment> list = FXCollections.observableArrayList();

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT * FROM equipment ORDER BY entry_date DESC, id DESC"
             )) {

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

    public static List<Assignment> getAssignmentsPendingReturn() {
        List<Assignment> list = new ArrayList<>();

        String sql =
                "SELECT DISTINCT a.id, a.person, a.department, a.equipment_type, a.quantity, a.date " +
                "FROM assignments a " +
                "INNER JOIN distribution d ON d.assignment_id = a.id " +
                "WHERE d.returned = 0 " +
                "ORDER BY a.date DESC, a.id DESC";

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
                        rs.getString("department"),
                        rs.getString("phone"),
                        rs.getString("email")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static User authenticateUser(String email, String plainPassword) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?) LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String storedPassword = rs.getString("password");
                if (!PasswordUtils.verify(plainPassword, storedPassword)) {
                    return null;
                }

                return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        storedPassword,
                        rs.getString("role"),
                        rs.getString("department"),
                        rs.getString("phone"),
                        rs.getString("email")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean userExistsByEmail(String email) {
        return exists("SELECT COUNT(*) FROM users WHERE LOWER(email)=LOWER(?)", email);
    }

    public static boolean resetUserPasswordByEmail(String email, String hashedPassword) {
        String sql = "UPDATE users SET password=? WHERE LOWER(email)=LOWER(?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hashedPassword);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void insertUser(String name, String password,
                                  String role, String department, String email) throws Exception {

        String sql = "INSERT INTO users (full_name, username, password, role, department, phone, email) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            insertDepartmentIfMissing(conn, department);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, role);
            ps.setString(5, department);
            ps.setString(6, null);
            ps.setString(7, email);

            ps.executeUpdate();
        }
    }

    public static boolean emailExists(String email) {
        return exists("SELECT COUNT(*) FROM users WHERE email=?", email);
    }

    public static boolean emailExistsForOtherUser(String email, int userId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE email=? AND id<>?"
             )) {

            ps.setString(1, email);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    public static boolean updateUser(int id, String name, String password, String role, String department, String email) throws Exception {
        String sql;

        // A real edit must update the selected database row by ID.
        // Without WHERE id = ?, the save action may appear to work in the dialog
        // while nothing meaningful is persisted to the actual record.
        if (password == null || password.isBlank()) {
            sql = "UPDATE users SET full_name=?, username=?, role=?, department=?, email=? WHERE id=?";
        } else {
            sql = "UPDATE users SET full_name=?, username=?, password=?, role=?, department=?, email=? WHERE id=?";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            insertDepartmentIfMissing(conn, department);

            ps.setString(1, name);
            ps.setString(2, email);

            if (password == null || password.isBlank()) {
                ps.setString(3, role);
                ps.setString(4, department);
                ps.setString(5, email);
                ps.setInt(6, id);
            } else {
                ps.setString(3, password);
                ps.setString(4, role);
                ps.setString(5, department);
                ps.setString(6, email);
                ps.setInt(7, id);
            }

            int rowsAffected = ps.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);
            return rowsAffected > 0;
        }
    }

    public static List<String> getDepartments() {
        List<String> departments = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM departments ORDER BY name")) {

            while (rs.next()) {
                departments.add(rs.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return departments;
    }

    public static void updateDepartment(String oldName, String newName) throws Exception {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertDepartmentIfMissing(conn, newName);

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET department=? WHERE TRIM(department)=TRIM(?)"
                )) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE assignments SET department=? WHERE TRIM(department)=TRIM(?)"
                )) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM departments WHERE TRIM(name)=TRIM(?)"
                )) {
                    ps.setString(1, oldName);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static void deleteDepartment(String department) throws Exception {
        try (Connection conn = getConnection()) {
            if (isDepartmentInUse(conn, department)) {
                throw new Exception("Department is still in use by users or assignments.");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM departments WHERE TRIM(name)=TRIM(?)"
            )) {
                ps.setString(1, department);
                ps.executeUpdate();
            }
        }
    }

    private static boolean isDepartmentInUse(Connection conn, String department) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT " +
                        "(SELECT COUNT(*) FROM users WHERE TRIM(department)=TRIM(?)) + " +
                        "(SELECT COUNT(*) FROM assignments WHERE TRIM(department)=TRIM(?))"
        )) {
            ps.setString(1, department);
            ps.setString(2, department);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static void insertDepartmentIfMissing(Connection conn, String department) throws SQLException {
        if (department == null || department.isBlank()) {
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO departments(name) VALUES (?)"
        )) {
            ps.setString(1, department.trim());
            ps.executeUpdate();
        }
    }

    // ================= STOCK =================
    public static int getAvailableStock(String type) {

        int available = 0;

        String sql =
            "SELECT COUNT(*) FROM equipment e " +
            "WHERE e.category = ? " +
            "AND e.asset_code NOT IN (" +
            "SELECT asset_code FROM distribution WHERE returned = 0" +
            ")";

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

    public static Map<String, Integer> getAvailableStockByCategory() {
        Map<String, Integer> stockByCategory = new LinkedHashMap<>();

        String sql =
            "SELECT TRIM(category) AS category, COUNT(*) AS total " +
            "FROM equipment e " +
            "WHERE e.asset_code NOT IN (" +
            "SELECT asset_code FROM distribution WHERE returned = 0" +
            ") " +
            "GROUP BY TRIM(category) " +
            "ORDER BY TRIM(category)";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String category = rs.getString("category");
                if (category != null && !category.isBlank()) {
                    stockByCategory.put(category, rs.getInt("total"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return stockByCategory;
    }

    // ================= DISTRIBUTION =================
    public static void distributeEquipment(String assetCode,
                                           int assignmentId,
                                           String name,
                                           String phone,
                                           String nid) throws Exception {

        String checkSql = "SELECT COUNT(*) FROM distribution WHERE asset_code = ? AND returned = 0";
        String insertSql = "INSERT INTO distribution (asset_code, assignment_id, assigned_to, phone, nid, date, returned) " +
                           "VALUES (?, ?, ?, ?, ?, DATE('now'), 0)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setString(1, assetCode);
                    ResultSet rs = check.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new Exception("Equipment already assigned");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, assetCode);
                    ps.setInt(2, assignmentId);
                    ps.setString(3, name);
                    ps.setString(4, phone);
                    ps.setString(5, nid);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static void distributeEquipmentBatch(int assignmentId, List<Distribution> distributions) throws Exception {
        String checkSql = "SELECT COUNT(*) FROM distribution WHERE asset_code = ? AND returned = 0";
        String insertSql = "INSERT INTO distribution (asset_code, assignment_id, assigned_to, phone, nid, date, returned) " +
                "VALUES (?, ?, ?, ?, ?, DATE('now'), 0)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement check = conn.prepareStatement(checkSql);
                 PreparedStatement insert = conn.prepareStatement(insertSql)) {

                for (Distribution distribution : distributions) {
                    check.setString(1, distribution.getAssetCode());
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new Exception("Equipment already assigned: " + distribution.getAssetCode());
                        }
                    }

                    insert.setString(1, distribution.getAssetCode());
                    insert.setInt(2, assignmentId);
                    insert.setString(3, distribution.getAssignedTo());
                    insert.setString(4, distribution.getPhone());
                    insert.setString(5, distribution.getNid());
                    insert.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public static List<String> getAvailableEquipment() {

        List<String> list = new ArrayList<>();

        String sql =
            "SELECT asset_code FROM equipment " +
            "WHERE asset_code NOT IN (SELECT asset_code FROM distribution WHERE returned = 0)";

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

    public static List<String> getAvailableEquipmentByCategory(String category) {
        List<String> list = new ArrayList<>();

        String sql =
            "SELECT asset_code FROM equipment " +
            "WHERE TRIM(category) = TRIM(?) " +
            "AND asset_code NOT IN (SELECT asset_code FROM distribution WHERE returned = 0) " +
            "ORDER BY asset_code";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("asset_code"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static List<Distribution> getCurrentDistributions() {
        List<Distribution> list = new ArrayList<>();

        String sql =
            "SELECT id, asset_code, assigned_to, phone, nid, date " +
            "FROM distribution " +
            "WHERE returned = 0 " +
            "ORDER BY date DESC, id DESC";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Distribution distribution = new Distribution(
                        rs.getInt("id"),
                        rs.getString("asset_code"),
                        "",
                        rs.getString("assigned_to"),
                        rs.getString("phone"),
                        rs.getString("nid"),
                        java.time.LocalDate.parse(rs.getString("date"))
                );
                list.add(distribution);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static Distribution getCurrentDistributionForAssignment(int assignmentId) {
        String sql =
                "SELECT id, asset_code, assigned_to, phone, nid, date " +
                "FROM distribution " +
                "WHERE assignment_id = ? AND returned = 0 " +
                "ORDER BY date DESC, id DESC " +
                "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, assignmentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Distribution(
                            rs.getInt("id"),
                            rs.getString("asset_code"),
                            "",
                            rs.getString("assigned_to"),
                            rs.getString("phone"),
                            rs.getString("nid"),
                            java.time.LocalDate.parse(rs.getString("date"))
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Distribution getCurrentDistributionForAsset(String assetCode) {
        String sql =
                "SELECT id, asset_code, assigned_to, phone, nid, date " +
                "FROM distribution " +
                "WHERE asset_code = ? AND returned = 0 " +
                "ORDER BY date DESC, id DESC " +
                "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, assetCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Distribution(
                            rs.getInt("id"),
                            rs.getString("asset_code"),
                            "",
                            rs.getString("assigned_to"),
                            rs.getString("phone"),
                            rs.getString("nid"),
                            java.time.LocalDate.parse(rs.getString("date"))
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<String> getOutstandingAssetCodesForAssignment(int assignmentId) {
        List<String> assetCodes = new ArrayList<>();

        String sql =
                "SELECT asset_code " +
                "FROM distribution " +
                "WHERE assignment_id = ? AND returned = 0 " +
                "ORDER BY date DESC, id DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, assignmentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assetCodes.add(rs.getString("asset_code"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return assetCodes;
    }

    // ================= RETURN EQUIPMENT =================
    public static void returnEquipment(String assetCode, String returnedBy,
                                       String phone, String nid,
                                       String condition, String remarks) throws Exception {

        String updateSql = "UPDATE distribution SET returned = 1 WHERE asset_code = ? AND returned = 0";
        String insertSql = "INSERT INTO returns (asset_code, returned_by, phone, nid, condition, remarks, return_date) " +
                           "VALUES (?, ?, ?, ?, ?, ?, DATE('now'))";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = conn.prepareStatement(updateSql)) {
                    ps1.setString(1, assetCode);
                    int updated = ps1.executeUpdate();
                    if (updated == 0) {
                        throw new Exception("Asset not found or already returned: " + assetCode);
                    }
                }

                try (PreparedStatement ps2 = conn.prepareStatement(insertSql)) {
                    ps2.setString(1, assetCode);
                    ps2.setString(2, returnedBy);
                    ps2.setString(3, phone);
                    ps2.setString(4, nid);
                    ps2.setString(5, condition);
                    ps2.setString(6, remarks);
                    ps2.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ================= DISTRIBUTED ASSET LOOKUP =================
    public static boolean isAssetCurrentlyDistributed(String assetCode) {
        String sql = "SELECT COUNT(*) FROM distribution WHERE asset_code = ? AND returned = 0";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, assetCode);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getDistributedCountForAssignment(int assignmentId) {
        String sql = "SELECT COUNT(*) FROM distribution WHERE assignment_id = ? AND returned = 0";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
