package com.shopsphere.dao;

import com.shopsphere.entity.OrderItem;
import com.shopsphere.entity.Orders;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Orders and Order Items management.
 * Uses atomic ACID database transactions for checkout and stock deduction.
 */
public class OrderDAO {

    private final ProductDAO productDAO = new ProductDAO();
    private final CartDAO cartDAO = new CartDAO();

    /**
     * Places a customer order atomically within a database transaction.
     * Inserts the order, inserts line items, validates & deducts inventory stock,
     * and clears the user's cart.
     *
     * @param order           Order header entity
     * @param items           List of order items
     * @param cartIdToClear   Optional cart ID to clear after checkout (can be null or 0)
     * @return Generated order_id if successful, or -1 if failed/rolled back
     */
    public int placeOrder(Orders order, List<OrderItem> items, Integer cartIdToClear) {
        if (items == null || items.isEmpty()) {
            System.err.println("[OrderDAO] Cannot place order with empty items.");
            return -1;
        }

        String insertOrderSql = "INSERT INTO orders (user_id, total_amount, order_status, payment_method, payment_status, " +
                "shipping_address, shipping_city, shipping_state, shipping_postal_code, shipping_country, tracking_number, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String insertOrderItemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        int generatedOrderId = -1;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Insert order header
            try (PreparedStatement orderStmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setInt(1, order.getUserId());
                orderStmt.setDouble(2, order.getTotalAmount());
                orderStmt.setString(3, order.getOrderStatus() != null ? order.getOrderStatus() : "PENDING");
                orderStmt.setString(4, order.getPaymentMethod() != null ? order.getPaymentMethod() : "COD");
                orderStmt.setString(5, order.getPaymentStatus() != null ? order.getPaymentStatus() : "PENDING");
                orderStmt.setString(6, order.getShippingAddress());
                orderStmt.setString(7, order.getShippingCity());
                orderStmt.setString(8, order.getShippingState());
                orderStmt.setString(9, order.getShippingPostalCode());
                orderStmt.setString(10, order.getShippingCountry() != null ? order.getShippingCountry() : "India");
                orderStmt.setString(11, order.getTrackingNumber());
                orderStmt.setString(12, order.getNotes());

                int affected = orderStmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return -1;
                }

                try (ResultSet rs = orderStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedOrderId = rs.getInt(1);
                        order.setOrderId(generatedOrderId);
                    }
                }
            }

            // 2. Insert line items and deduct stock
            try (PreparedStatement itemStmt = conn.prepareStatement(insertOrderItemSql)) {
                for (OrderItem item : items) {
                    // Check and deduct stock atomically
                    boolean stockDeducted = productDAO.deductStock(conn, item.getProductId(), item.getQuantity());
                    if (!stockDeducted) {
                        System.err.println("[OrderDAO] Insufficient stock for product ID: " + item.getProductId());
                        conn.rollback();
                        return -1;
                    }

                    itemStmt.setInt(1, generatedOrderId);
                    itemStmt.setInt(2, item.getProductId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getUnitPrice());
                    itemStmt.setDouble(5, item.getSubtotal());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            // 3. Clear cart if specified
            if (cartIdToClear != null && cartIdToClear > 0) {
                cartDAO.clearCart(conn, cartIdToClear);
            }

            conn.commit(); // Transaction successful
            return generatedOrderId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[OrderDAO] Error during rollback: " + ex.getMessage());
                }
            }
            System.err.println("[OrderDAO] Error placing order: " + e.getMessage());
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
     * Retrieves an order by ID along with its line items and customer details.
     */
    public Orders findById(int orderId) {
        String sql = "SELECT o.*, u.full_name AS customer_name, u.email AS customer_email " +
                "FROM orders o " +
                "JOIN users u ON o.user_id = u.user_id " +
                "WHERE o.order_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Orders order = mapResultSetToOrder(rs);
                    order.setOrderItems(getOrderItems(orderId));
                    return order;
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error finding order by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all orders placed by a specific user (order history).
     */
    public List<Orders> findByUserId(int userId) {
        List<Orders> list = new ArrayList<>();
        String sql = "SELECT o.*, u.full_name AS customer_name, u.email AS customer_email " +
                "FROM orders o " +
                "JOIN users u ON o.user_id = u.user_id " +
                "WHERE o.user_id = ? " +
                "ORDER BY o.order_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Orders order = mapResultSetToOrder(rs);
                    order.setOrderItems(getOrderItems(order.getOrderId()));
                    list.add(order);
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error fetching user orders: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all orders in the system (for Admin dashboard).
     */
    public List<Orders> findAllOrders() {
        List<Orders> list = new ArrayList<>();
        String sql = "SELECT o.*, u.full_name AS customer_name, u.email AS customer_email " +
                "FROM orders o " +
                "JOIN users u ON o.user_id = u.user_id " +
                "ORDER BY o.order_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Orders order = mapResultSetToOrder(rs);
                order.setOrderItems(getOrderItems(order.getOrderId()));
                list.add(order);
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error fetching all orders: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves individual line items for an order.
     */
    public List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.*, p.product_name, p.brand, p.image_url " +
                "FROM order_items oi " +
                "JOIN products p ON oi.product_id = p.product_id " +
                "WHERE oi.order_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem(
                            rs.getInt("order_item_id"),
                            rs.getInt("order_id"),
                            rs.getInt("product_id"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price"),
                            rs.getDouble("subtotal")
                    );
                    item.setProductName(rs.getString("product_name"));
                    item.setBrand(rs.getString("brand"));
                    item.setImageUrl(rs.getString("image_url"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error fetching order items: " + e.getMessage());
        }
        return items;
    }

    /**
     * Updates order fulfillment status (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED).
     */
    public boolean updateOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET order_status = ? WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error updating order status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates payment status (PENDING, PAID, FAILED, REFUNDED).
     */
    public boolean updatePaymentStatus(int orderId, String paymentStatus) {
        String sql = "UPDATE orders SET payment_status = ? WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, paymentStatus);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error updating payment status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates order shipping tracking number.
     */
    public boolean updateTrackingNumber(int orderId, String trackingNumber) {
        String sql = "UPDATE orders SET tracking_number = ? WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, trackingNumber);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error updating tracking number: " + e.getMessage());
            return false;
        }
    }

    private Orders mapResultSetToOrder(ResultSet rs) throws SQLException {
        Orders o = new Orders(
                rs.getInt("order_id"),
                rs.getInt("user_id"),
                rs.getTimestamp("order_date"),
                rs.getDouble("total_amount"),
                rs.getString("order_status"),
                rs.getString("payment_method"),
                rs.getString("payment_status"),
                rs.getString("shipping_address"),
                rs.getString("shipping_city"),
                rs.getString("shipping_state"),
                rs.getString("shipping_postal_code"),
                rs.getString("shipping_country"),
                rs.getString("tracking_number"),
                rs.getString("notes")
        );
        try {
            o.setCustomerName(rs.getString("customer_name"));
            o.setCustomerEmail(rs.getString("customer_email"));
        } catch (SQLException ignored) {
        }
        return o;
    }
}
