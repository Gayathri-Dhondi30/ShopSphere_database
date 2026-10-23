package com.shopsphere.entity;

import java.sql.Timestamp;

/**
 * Entity representing a catalog product in ShopSphere.
 */
public class Product {
    private int productId;
    private int categoryId;
    private String categoryName; // Join helper field
    private String productName;
    private String brand;
    private String description;
    private double price;
    private int stockQuantity;
    private String imageUrl;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Product() {
        this.isActive = true;
    }

    public Product(int categoryId, String productName, String brand, String description,
                   double price, int stockQuantity, String imageUrl) {
        this.categoryId = categoryId;
        this.productName = productName;
        this.brand = brand;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
        this.isActive = true;
    }

    public Product(int productId, int categoryId, String productName, String brand,
                   String description, double price, int stockQuantity, String imageUrl,
                   boolean isActive, Timestamp createdAt, Timestamp updatedAt) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.productName = productName;
        this.brand = brand;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", brand='" + brand + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                ", isActive=" + isActive +
                '}';
    }
}
