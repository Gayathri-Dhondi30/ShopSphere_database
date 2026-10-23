package com.shopsphere.entity;

import java.sql.Timestamp;

/**
 * Entity representing an item saved in a user's wishlist / saved-for-later list.
 */
public class Wishlist {
    private int wishlistId;
    private int userId;
    private int productId;
    private Timestamp addedAt;

    // Joined Product details
    private String productName;
    private String brand;
    private double price;
    private String imageUrl;
    private int stockQuantity;

    public Wishlist() {
    }

    public Wishlist(int userId, int productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public Wishlist(int wishlistId, int userId, int productId, Timestamp addedAt) {
        this.wishlistId = wishlistId;
        this.userId = userId;
        this.productId = productId;
        this.addedAt = addedAt;
    }

    public int getWishlistId() {
        return wishlistId;
    }

    public void setWishlistId(int wishlistId) {
        this.wishlistId = wishlistId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public Timestamp getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Timestamp addedAt) {
        this.addedAt = addedAt;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    @Override
    public String toString() {
        return "Wishlist{" +
                "wishlistId=" + wishlistId +
                ", userId=" + userId +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                '}';
    }
}
