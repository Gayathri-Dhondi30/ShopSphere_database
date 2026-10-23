package com.shopsphere.dao;

import com.shopsphere.entity.Admin;
import com.shopsphere.util.DBConnection;
import com.shopsphere.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Admin accounts and back-office management operations.
 */
public class AdminDAO {

    /**
     * Authenticates an administrator by username or email and plaintext password.
     * Updates last_login timestamp on successful login.
     */
    public Admin authenticate(String usernameOrEmail, String plainPassword) {
        String sql = "SELECT * FROM admins WHERE (LOWER(username) = ? OR LOWER(email) = ?) AND is_active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String term = usernameOrEmail.trim().toLowerCase();
            stmt.setString(1, term);
            stmt.setString(2, term);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordUtil.verifyPassword(plainPassword, storedHash)) {
                        Admin admin = mapResultSetToAdmin(rs);
                        updateLastLogin(admin.getAdminId());
                        return admin;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[AdminDAO] Admin login error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Creates a new administrator account with a hashed password.
     */
    public int createAdmin(Admin admin, String plainPassword) {
        String sql = "INSERT INTO admins (username, email, password_hash, full_name, role, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, admin.getUsername().trim());
            stmt.setString(2, admin.getEmail().trim().toLowerCase());
            stmt.setString(3, PasswordUtil.hashPassword(plainPassword));
            stmt.setString(4, admin.getFullName());
            stmt.setString(5, admin.getRole() != null ? admin.getRole() : "MANAGER");
            stmt.setBoolean(6, admin.isActive());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        admin.setAdminId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[AdminDAO] Error creating admin: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves admin by ID.
     */
    public Admin findById(int adminId) {
        String sql = "SELECT * FROM admins WHERE admin_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, adminId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAdmin(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AdminDAO] Error finding admin by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all administrators.
     */
    public List<Admin> findAll() {
        List<Admin> list = new ArrayList<>();
        String sql = "SELECT * FROM admins ORDER BY admin_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToAdmin(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AdminDAO] Error listing admins: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates last login timestamp.
     */
    private void updateLastLogin(int adminId) {
        String sql = "UPDATE admins SET last_login = CURRENT_TIMESTAMP WHERE admin_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, adminId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AdminDAO] Error updating last_login: " + e.getMessage());
        }
    }

    /**
     * Changes an administrator password.
     */
    public boolean changePassword(int adminId, String oldPassword, String newPassword) {
        Admin admin = findById(adminId);
        if (admin == null || !PasswordUtil.verifyPassword(oldPassword, admin.getPasswordHash())) {
            return false;
        }

        String sql = "UPDATE admins SET password_hash = ? WHERE admin_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, PasswordUtil.hashPassword(newPassword));
            stmt.setInt(2, adminId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AdminDAO] Error changing admin password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Activates or deactivates an admin account.
     */
    public boolean setAdminStatus(int adminId, boolean isActive) {
        String sql = "UPDATE admins SET is_active = ? WHERE admin_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isActive);
            stmt.setInt(2, adminId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AdminDAO] Error setting admin status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Provides key dashboard summary analytics for the administrator.
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                "  (SELECT COUNT(*) FROM users) AS total_users, " +
                "  (SELECT COUNT(*) FROM products WHERE is_active = TRUE) AS total_products, " +
                "  (SELECT COUNT(*) FROM orders) AS total_orders, " +
                "  (SELECT IFNULL(SUM(total_amount), 0) FROM orders WHERE payment_status = 'PAID') AS total_revenue, " +
                "  (SELECT COUNT(*) FROM products WHERE stock_quantity <= 10) AS low_stock_count";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.put("totalUsers", rs.getInt("total_users"));
                stats.put("totalProducts", rs.getInt("total_products"));
                stats.put("totalOrders", rs.getInt("total_orders"));
                stats.put("totalRevenue", rs.getDouble("total_revenue"));
                stats.put("lowStockCount", rs.getInt("low_stock_count"));
            }
        } catch (SQLException e) {
            System.err.println("[AdminDAO] Error fetching dashboard stats: " + e.getMessage());
        }
        return stats;
    }

    private Admin mapResultSetToAdmin(ResultSet rs) throws SQLException {
        return new Admin(
                rs.getInt("admin_id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                rs.getString("role"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("last_login")
        );
    }
}
