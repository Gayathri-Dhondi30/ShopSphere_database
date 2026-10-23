package com.shopsphere.dao;

import com.shopsphere.entity.Category;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Category operations.
 */
public class CategoryDAO {

    /**
     * Adds a new product category.
     *
     * @return Generated category_id, or -1 if failed
     */
    public int addCategory(Category category) {
        String sql = "INSERT INTO categories (category_name, description, image_url, is_active) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, category.getCategoryName());
            stmt.setString(2, category.getDescription());
            stmt.setString(3, category.getImageUrl());
            stmt.setBoolean(4, category.isActive());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        category.setCategoryId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CategoryDAO] Error adding category: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves category by ID.
     */
    public Category findById(int categoryId) {
        String sql = "SELECT * FROM categories WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategory(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CategoryDAO] Error finding category by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves category by name.
     */
    public Category findByName(String categoryName) {
        String sql = "SELECT * FROM categories WHERE category_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoryName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategory(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CategoryDAO] Error finding category by name: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all active categories for store navigation.
     */
    public List<Category> findAllActive() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE is_active = TRUE ORDER BY category_name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToCategory(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CategoryDAO] Error fetching active categories: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all categories (including inactive, for admin view).
     */
    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY category_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToCategory(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CategoryDAO] Error fetching all categories: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates an existing category.
     */
    public boolean updateCategory(Category category) {
        String sql = "UPDATE categories SET category_name = ?, description = ?, image_url = ?, is_active = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category.getCategoryName());
            stmt.setString(2, category.getDescription());
            stmt.setString(3, category.getImageUrl());
            stmt.setBoolean(4, category.isActive());
            stmt.setInt(5, category.getCategoryId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CategoryDAO] Error updating category: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a category by ID (will fail if products reference it due to ON DELETE RESTRICT).
     */
    public boolean deleteCategory(int categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CategoryDAO] Error deleting category: " + e.getMessage());
            return false;
        }
    }

    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        return new Category(
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getString("description"),
                rs.getString("image_url"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at")
        );
    }
}
