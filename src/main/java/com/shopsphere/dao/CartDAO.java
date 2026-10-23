package com.shopsphere.dao;

import com.shopsphere.entity.Cart;
import com.shopsphere.entity.CartItem;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Shopping Cart and Cart Items management.
 */
public class CartDAO {

    /**
     * Gets or creates an active cart for a user.
     */
    public Cart getOrCreateCart(int userId) {
        String selectSql = "SELECT * FROM carts WHERE user_id = ?";
        String insertSql = "INSERT INTO carts (user_id) VALUES (?)";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Cart cart = new Cart(
                                rs.getInt("cart_id"),
                                rs.getInt("user_id"),
                                rs.getTimestamp("created_at"),
                                rs.getTimestamp("updated_at")
                        );
                        cart.setItems(getCartItems(cart.getCartId()));
                        return cart;
                    }
                }
            }

            // Not found, create one
            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Cart cart = new Cart(userId);
                        cart.setCartId(rs.getInt(1));
                        return cart;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CartDAO] Error in getOrCreateCart: " + e.getMessage());
        }
        return null;
    }

    /**
     * Adds a product to the user's cart.
     * If product already exists in cart, increments the quantity.
     *
     * @param userId    User ID
     * @param productId Product to add
     * @param quantity  Quantity to add
     * @return true if successful, false otherwise
     */
    public boolean addItemToCart(int userId, int productId, int quantity) {
        if (quantity <= 0) return false;

        Cart cart = getOrCreateCart(userId);
        if (cart == null) return false;

        String checkItemSql = "SELECT cart_item_id, quantity FROM cart_items WHERE cart_id = ? AND product_id = ?";
        String updateItemSql = "UPDATE cart_items SET quantity = quantity + ? WHERE cart_item_id = ?";
        String insertItemSql = "INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement(checkItemSql)) {
                checkStmt.setInt(1, cart.getCartId());
                checkStmt.setInt(2, productId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int cartItemId = rs.getInt("cart_item_id");
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateItemSql)) {
                            updateStmt.setInt(1, quantity);
                            updateStmt.setInt(2, cartItemId);
                            return updateStmt.executeUpdate() > 0;
                        }
                    } else {
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertItemSql)) {
                            insertStmt.setInt(1, cart.getCartId());
                            insertStmt.setInt(2, productId);
                            insertStmt.setInt(3, quantity);
                            return insertStmt.executeUpdate() > 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CartDAO] Error adding item to cart: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all items inside a cart with product details.
     */
    public List<CartItem> getCartItems(int cartId) {
        List<CartItem> items = new ArrayList<>();
        String sql = "SELECT ci.cart_item_id, ci.cart_id, ci.product_id, ci.quantity, ci.added_at, " +
                "p.product_name, p.brand, p.price, p.image_url, p.stock_quantity " +
                "FROM cart_items ci " +
                "JOIN products p ON ci.product_id = p.product_id " +
                "WHERE ci.cart_id = ? " +
                "ORDER BY ci.added_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cartId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CartItem item = new CartItem(
                            rs.getInt("cart_item_id"),
                            rs.getInt("cart_id"),
                            rs.getInt("product_id"),
                            rs.getInt("quantity"),
                            rs.getTimestamp("added_at")
                    );
                    item.setProductName(rs.getString("product_name"));
                    item.setBrand(rs.getString("brand"));
                    item.setUnitPrice(rs.getDouble("price"));
                    item.setImageUrl(rs.getString("image_url"));
                    item.setStockAvailable(rs.getInt("stock_quantity"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CartDAO] Error retrieving cart items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Updates the exact quantity of an existing cart item.
     * If new quantity is 0 or less, deletes the item.
     */
    public boolean updateItemQuantity(int cartItemId, int newQuantity) {
        if (newQuantity <= 0) {
            return removeCartItem(cartItemId);
        }

        String sql = "UPDATE cart_items SET quantity = ? WHERE cart_item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newQuantity);
            stmt.setInt(2, cartItemId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CartDAO] Error updating cart item quantity: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes an item from the cart.
     */
    public boolean removeCartItem(int cartItemId) {
        String sql = "DELETE FROM cart_items WHERE cart_item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cartItemId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CartDAO] Error removing cart item: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clears all items from a user's cart (used after successful order placement).
     */
    public boolean clearCart(int cartId) {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, cartId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[CartDAO] Error clearing cart: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clears cart by connection (within a larger transaction).
     */
    public void clearCart(Connection conn, int cartId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cartId);
            stmt.executeUpdate();
        }
    }
}
