package com.shopsphere.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a customer purchase order in ShopSphere.
 */
public class Orders {
    private int orderId;
    private int userId;
    private Timestamp orderDate;
    private double totalAmount;
    private String orderStatus;      // PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    private String paymentMethod;    // COD, CREDIT_CARD, DEBIT_CARD, UPI, NET_BANKING
    private String paymentStatus;    // PENDING, PAID, FAILED, REFUNDED
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;
    private String trackingNumber;
    private String notes;

    // Joined customer summary
    private String customerName;
    private String customerEmail;

    // Line items contained in this order
    private List<OrderItem> orderItems;

    public Orders() {
        this.orderStatus = "PENDING";
        this.paymentMethod = "COD";
        this.paymentStatus = "PENDING";
        this.shippingCountry = "India";
        this.orderItems = new ArrayList<>();
    }

    public Orders(int userId, double totalAmount, String paymentMethod,
                  String shippingAddress, String shippingCity, String shippingState,
                  String shippingPostalCode, String notes) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.orderStatus = "PENDING";
        this.paymentMethod = paymentMethod;
        this.paymentStatus = "COD".equalsIgnoreCase(paymentMethod) ? "PENDING" : "PAID";
        this.shippingAddress = shippingAddress;
        this.shippingCity = shippingCity;
        this.shippingState = shippingState;
        this.shippingPostalCode = shippingPostalCode;
        this.shippingCountry = "India";
        this.notes = notes;
        this.orderItems = new ArrayList<>();
    }

    public Orders(int orderId, int userId, Timestamp orderDate, double totalAmount,
                  String orderStatus, String paymentMethod, String paymentStatus,
                  String shippingAddress, String shippingCity, String shippingState,
                  String shippingPostalCode, String shippingCountry, String trackingNumber,
                  String notes) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.shippingAddress = shippingAddress;
        this.shippingCity = shippingCity;
        this.shippingState = shippingState;
        this.shippingPostalCode = shippingPostalCode;
        this.shippingCountry = shippingCountry;
        this.trackingNumber = trackingNumber;
        this.notes = notes;
        this.orderItems = new ArrayList<>();
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getShippingState() {
        return shippingState;
    }

    public void setShippingState(String shippingState) {
        this.shippingState = shippingState;
    }

    public String getShippingPostalCode() {
        return shippingPostalCode;
    }

    public void setShippingPostalCode(String shippingPostalCode) {
        this.shippingPostalCode = shippingPostalCode;
    }

    public String getShippingCountry() {
        return shippingCountry;
    }

    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems != null ? orderItems : new ArrayList<>();
    }

    public void addOrderItem(OrderItem item) {
        this.orderItems.add(item);
    }

    @Override
    public String toString() {
        return "Orders{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", totalAmount=" + totalAmount +
                ", orderStatus='" + orderStatus + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                '}';
    }
}
