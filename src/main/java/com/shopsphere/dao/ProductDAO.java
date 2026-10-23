package com.shopsphere.dao;

import com.shopsphere.entity.Product;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Product catalog management, searching, filtering, and stock adjustments.
 */
public class ProductDAO {

    /**
     * Adds a new product to the catalog.
     *
     * @return Generated product_id, or -1 if failed
     */
    public int addProduct(Product product) {
        String sql = "INSERT INTO products (category_id, product_name, brand, description, price, stock_quantity, image_url, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, product.getCategoryId());
            stmt.setString(2, product.getProductName());
            stmt.setString(3, product.getBrand());
            stmt.setString(4, product.getDescription());
            stmt.setDouble(5, product.getPrice());
            stmt.setInt(6, product.getStockQuantity());
            stmt.setString(7, product.getImageUrl());
            stmt.setBoolean(8, product.isActive());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        product.setProductId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error adding product: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves product by ID joined with category name.
     */
    public Product findById(int productId) {
        String sql = "SELECT p.*, c.category_name FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error finding product by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all active products.
     */
    public List<Product> findAllActive() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.category_name FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.is_active = TRUE ORDER BY p.product_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error fetching active products: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves products by category.
     */
    public List<Product> findByCategory(int categoryId) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.category_name FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.category_id = ? AND p.is_active = TRUE ORDER BY p.product_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error fetching products by category: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves products by brand.
     */
    public List<Product> findByBrand(String brand) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.category_name FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE LOWER(p.brand) = LOWER(?) AND p.is_active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, brand);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error fetching products by brand: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves distinct brand names for the frontend brand filter.
     */
    public List<String> findAllBrands() {
        List<String> brands = new ArrayList<>();
        String sql = "SELECT DISTINCT brand FROM products WHERE is_active = TRUE AND brand IS NOT NULL ORDER BY brand ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                brands.add(rs.getString("brand"));
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error fetching brands: " + e.getMessage());
        }
        return brands;
    }

    /**
     * Keyword search across product name, brand, and description.
     */
    public List<Product> searchProducts(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.category_name FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.is_active = TRUE AND ( " +
                "  LOWER(p.product_name) LIKE ? OR " +
                "  LOWER(p.brand) LIKE ? OR " +
                "  LOWER(p.description) LIKE ? " +
                ") ORDER BY p.product_id DESC";

        String pattern = "%" + (keyword != null ? keyword.trim().toLowerCase() : "") + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error searching products: " + e.getMessage());
        }
        return list;
    }

    /**
     * Multi-criteria filter supporting category, brand, and min/max price range.
     */
    public List<Product> filterProducts(Integer categoryId, String brand, Double minPrice, Double maxPrice) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT p.*, c.category_name FROM products p ")
                .append("LEFT JOIN categories c ON p.category_id = c.category_id ")
                .append("WHERE p.is_active = TRUE ");

        List<Object> params = new ArrayList<>();

        if (categoryId != null && categoryId > 0) {
            sql.append("AND p.category_id = ? ");
            params.add(categoryId);
        }
        if (brand != null && !brand.trim().isEmpty()) {
            sql.append("AND LOWER(p.brand) = LOWER(?) ");
            params.add(brand.trim());
        }
        if (minPrice != null && minPrice >= 0) {
            sql.append("AND p.price >= ? ");
            params.add(minPrice);
        }
        if (maxPrice != null && maxPrice >= 0) {
            sql.append("AND p.price <= ? ");
            params.add(maxPrice);
        }

        sql.append("ORDER BY p.price ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error filtering products: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates an existing product details.
     */
    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET category_id = ?, product_name = ?, brand = ?, description = ?, " +
                "price = ?, stock_quantity = ?, image_url = ?, is_active = ? WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, product.getCategoryId());
            stmt.setString(2, product.getProductName());
            stmt.setString(3, product.getBrand());
            stmt.setString(4, product.getDescription());
            stmt.setDouble(5, product.getPrice());
            stmt.setInt(6, product.getStockQuantity());
            stmt.setString(7, product.getImageUrl());
            stmt.setBoolean(8, product.isActive());
            stmt.setInt(9, product.getProductId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error updating product: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deducts stock upon order completion. Returns false if insufficient stock.
     */
    public boolean deductStock(Connection conn, int productId, int quantityToDeduct) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantityToDeduct);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantityToDeduct);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a product by ID.
     */
    public boolean deleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Error deleting product: " + e.getMessage());
            return false;
        }
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product(
                rs.getInt("product_id"),
                rs.getInt("category_id"),
                rs.getString("product_name"),
                rs.getString("brand"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getInt("stock_quantity"),
                rs.getString("image_url"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
        try {
            p.setCategoryName(rs.getString("category_name"));
        } catch (SQLException ignored) {
        }
        return p;
    }
}
