package com.shopsphere.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user's shopping cart.
 */
public class Cart {
    private int cartId;
    private int userId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<CartItem> items;

    public Cart() {
        this.items = new ArrayList<>();
    }

    public Cart(int userId) {
        this.userId = userId;
        this.items = new ArrayList<>();
    }

    public Cart(int cartId, int userId, Timestamp createdAt, Timestamp updatedAt) {
        this.cartId = cartId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = new ArrayList<>();
    }

    public int getCartId() {
        return cartId;
    }

    public void setCartId(int cartId) {
        this.cartId = cartId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void addItem(CartItem item) {
        this.items.add(item);
    }

    public double getTotalAmount() {
        double total = 0.0;
        for (CartItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    public int getTotalItemCount() {
        int count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    @Override
    public String toString() {
        return "Cart{" +
                "cartId=" + cartId +
                ", userId=" + userId +
                ", itemsCount=" + getTotalItemCount() +
                ", totalAmount=" + getTotalAmount() +
                '}';
    }
}
