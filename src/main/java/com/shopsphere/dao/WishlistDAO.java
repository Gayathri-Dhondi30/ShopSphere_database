package com.shopsphere.dao;

import com.shopsphere.entity.Wishlist;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Wishlist / Saved-for-Later operations.
 */
public class WishlistDAO {

    private final CartDAO cartDAO = new CartDAO();

    /**
     * Adds a product to the user's wishlist.
     */
    public boolean addToWishlist(int userId, int productId) {
        String sql = "INSERT IGNORE INTO wishlists (user_id, product_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[WishlistDAO] Error adding to wishlist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes an item from the user's wishlist.
     */
    public boolean removeFromWishlist(int userId, int productId) {
        String sql = "DELETE FROM wishlists WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[WishlistDAO] Error removing from wishlist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a product is already in the user's wishlist.
     */
    public boolean isInWishlist(int userId, int productId) {
        String sql = "SELECT 1 FROM wishlists WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[WishlistDAO] Error checking wishlist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all items in a user's wishlist with full product details.
     */
    public List<Wishlist> getWishlistByUserId(int userId) {
        List<Wishlist> list = new ArrayList<>();
        String sql = "SELECT w.wishlist_id, w.user_id, w.product_id, w.added_at, " +
                "p.product_name, p.brand, p.price, p.image_url, p.stock_quantity " +
                "FROM wishlists w " +
                "JOIN products p ON w.product_id = p.product_id " +
                "WHERE w.user_id = ? " +
                "ORDER BY w.added_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Wishlist w = new Wishlist(
                            rs.getInt("wishlist_id"),
                            rs.getInt("user_id"),
                            rs.getInt("product_id"),
                            rs.getTimestamp("added_at")
                    );
                    w.setProductName(rs.getString("product_name"));
                    w.setBrand(rs.getString("brand"));
                    w.setPrice(rs.getDouble("price"));
                    w.setImageUrl(rs.getString("image_url"));
                    w.setStockQuantity(rs.getInt("stock_quantity"));
                    list.add(w);
                }
            }
        } catch (SQLException e) {
            System.err.println("[WishlistDAO] Error fetching wishlist items: " + e.getMessage());
        }
        return list;
    }

    /**
     * Moves a product from wishlist to the user's shopping cart.
     */
    public boolean moveToCart(int userId, int productId) {
        boolean added = cartDAO.addItemToCart(userId, productId, 1);
        if (added) {
            removeFromWishlist(userId, productId);
            return true;
        }
        return false;
    }
}
