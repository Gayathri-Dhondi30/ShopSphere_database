package com.shopsphere.dao;

import com.shopsphere.entity.User;
import com.shopsphere.util.DBConnection;
import com.shopsphere.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations (Registration, Authentication, Profile Management).
 */
public class UserDAO {

    /**
     * Registers a new user. Securely hashes the plaintext password and initializes
     * an associated shopping cart within a single database transaction.
     *
     * @param user          User entity containing profile details
     * @param plainPassword Raw password to hash and store
     * @return Generated user_id, or -1 if registration failed
     */
    public int registerUser(User user, String plainPassword) {
        String insertUserSql = "INSERT INTO users (full_name, email, password_hash, phone, address, city, state, postal_code, country) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String createCartSql = "INSERT INTO carts (user_id) VALUES (?)";

        String hashedPassword = PasswordUtil.hashPassword(plainPassword);
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            int generatedUserId = -1;
            try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, user.getFullName());
                userStmt.setString(2, user.getEmail().trim().toLowerCase());
                userStmt.setString(3, hashedPassword);
                userStmt.setString(4, user.getPhone());
                userStmt.setString(5, user.getAddress());
                userStmt.setString(6, user.getCity());
                userStmt.setString(7, user.getState());
                userStmt.setString(8, user.getPostalCode());
                userStmt.setString(9, user.getCountry() != null ? user.getCountry() : "India");

                int affected = userStmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return -1;
                }

                try (ResultSet rs = userStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedUserId = rs.getInt(1);
                        user.setUserId(generatedUserId);
                    }
                }
            }

            // Create initial shopping cart for the user
            try (PreparedStatement cartStmt = conn.prepareStatement(createCartSql)) {
                cartStmt.setInt(1, generatedUserId);
                cartStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
            return generatedUserId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[UserDAO] Error during rollback: " + ex.getMessage());
                }
            }
            System.err.println("[UserDAO] Error registering user: " + e.getMessage());
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Authenticates a user with email and plain text password.
     *
     * @param email         User email
     * @param plainPassword Raw password to verify against stored hash
     * @return User entity if authenticated successfully, null otherwise
     */
    public User authenticate(String email, String plainPassword) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordUtil.verifyPassword(plainPassword, storedHash)) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Authentication error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds a user by their unique ID.
     */
    public User findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error fetching user by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds a user by email address.
     */
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error fetching user by email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates profile details for an existing user.
     */
    public boolean updateUserProfile(User user) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, address = ?, city = ?, state = ?, postal_code = ?, country = ? " +
                "WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getAddress());
            stmt.setString(4, user.getCity());
            stmt.setString(5, user.getState());
            stmt.setString(6, user.getPostalCode());
            stmt.setString(7, user.getCountry());
            stmt.setInt(8, user.getUserId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating user profile: " + e.getMessage());
            return false;
        }
    }

    /**
     * Alias for updateUserProfile for convenience.
     */
    public boolean updateProfile(User user) {
        return updateUserProfile(user);
    }

    /**
     * Changes a user's password after verifying the old password.
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        User user = findById(userId);
        if (user == null || !PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            return false;
        }

        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, PasswordUtil.hashPassword(newPassword));
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error updating password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all registered users (for admin management).
     */
    public List<User> findAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error fetching all users: " + e.getMessage());
        }
        return list;
    }

    /**
     * Deletes a user by ID. Cascades to carts and wishlists.
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] Error deleting user: " + e.getMessage());
            return false;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("postal_code"),
                rs.getString("country"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
    }
}
