package com.shopsphere;

import com.shopsphere.dao.*;
import com.shopsphere.entity.*;
import com.shopsphere.util.DBConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive Console Application for ShopSphere E-Commerce System.
 * 
 * Allows users and administrators to register and authenticate using their
 * own custom email and password (no fixed or hardcoded credentials required).
 */
public class ShopSphereApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final UserDAO userDAO = new UserDAO();
    private static final AdminDAO adminDAO = new AdminDAO();
    private static final ProductDAO productDAO = new ProductDAO();
    private static final CategoryDAO categoryDAO = new CategoryDAO();
    private static final CartDAO cartDAO = new CartDAO();
    private static final WishlistDAO wishlistDAO = new WishlistDAO();
    private static final OrderDAO orderDAO = new OrderDAO();

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("          WELCOME TO SHOPSPHERE E-COMMERCE PLATFORM            ");
        System.out.println("===============================================================");

        if (!DBConnection.testConnection()) {
            System.err.println("[ERROR] Unable to connect to MySQL database. Please verify db.properties.");
            return;
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleUserRegistration();
                    break;
                case "2":
                    handleUserLogin();
                    break;
                case "3":
                    handleAdminRegistration();
                    break;
                case "4":
                    handleAdminLogin();
                    break;
                case "5":
                    handleBrowseCatalogGuest();
                    break;
                case "6":
                    System.out.println("\nThank you for using ShopSphere! Goodbye.");
                    running = false;
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter a number between 1 and 6.");
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("                        MAIN MENU                              ");
        System.out.println("---------------------------------------------------------------");
        System.out.println(" 1. Register New Customer Account (Your chosen Email & Password)");
        System.out.println(" 2. Customer Login                (Enter your Email & Password)");
        System.out.println(" 3. Register New Administrator    (Your chosen Username/Email & Password)");
        System.out.println(" 4. Administrator Login           (Enter Admin Email/Username & Password)");
        System.out.println(" 5. Browse Products as Guest");
        System.out.println(" 6. Exit");
        System.out.print("Enter your choice (1-6): ");
    }

    // ============================================================
    // USER REGISTRATION & LOGIN (DYNAMIC USER-DEFINED CREDENTIALS)
    // ============================================================

    private static void handleUserRegistration() {
        System.out.println("\n--- [CUSTOMER REGISTRATION] ---");
        System.out.println("Set your own personal credentials (no fixed values):");

        System.out.print("Enter Full Name: ");
        String fullName = scanner.nextLine().trim();
        while (fullName.isEmpty()) {
            System.out.print("Full name cannot be empty. Enter Full Name: ");
            fullName = scanner.nextLine().trim();
        }

        System.out.print("Enter Your Email Address: ");
        String email = scanner.nextLine().trim().toLowerCase();
        while (!email.contains("@") || !email.contains(".")) {
            System.out.print("Invalid email format. Enter a valid Email Address: ");
            email = scanner.nextLine().trim().toLowerCase();
        }

        if (userDAO.findByEmail(email) != null) {
            System.out.println("[!] This email is already registered. Please choose 'Customer Login' or use another email.");
            return;
        }

        System.out.print("Enter Your Password: ");
        String password = scanner.nextLine();
        while (password.trim().isEmpty() || password.length() < 4) {
            System.out.print("Password must be at least 4 characters. Enter Password: ");
            password = scanner.nextLine();
        }

        System.out.print("Enter Phone Number: ");
        String phone = scanner.nextLine().trim();

        System.out.print("Enter Street Address: ");
        String address = scanner.nextLine().trim();

        System.out.print("Enter City: ");
        String city = scanner.nextLine().trim();

        System.out.print("Enter State: ");
        String state = scanner.nextLine().trim();

        System.out.print("Enter Postal Code (PIN): ");
        String postalCode = scanner.nextLine().trim();

        System.out.print("Enter Country [Default: India]: ");
        String country = scanner.nextLine().trim();
        if (country.isEmpty()) country = "India";

        User newUser = new User(fullName, email, null, phone, address, city, state, postalCode, country);
        int userId = userDAO.registerUser(newUser, password);

        if (userId > 0) {
            System.out.println("\n[SUCCESS] Customer account registered successfully!");
            System.out.println(" -> User ID: " + userId);
            System.out.println(" -> Registered Email: " + email);
            System.out.println(" -> Password securely SHA-256 encrypted in MySQL database.");
            System.out.println(" -> Initialized personal shopping cart for your account.");
            System.out.println("You can now login using Option 2.");
        } else {
            System.out.println("[ERROR] Failed to register account. Please try again.");
        }
    }

    private static void handleUserLogin() {
        System.out.println("\n--- [CUSTOMER LOGIN] ---");
        System.out.print("Enter Your Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Enter Your Password: ");
        String password = scanner.nextLine();

        User user = userDAO.authenticate(email, password);
        if (user != null) {
            System.out.println("\n[SUCCESS] Welcome back, " + user.getFullName() + "!");
            customerSession(user);
        } else {
            System.out.println("\n[!] Invalid email or password. Please try again or register a new account.");
        }
    }

    private static void customerSession(User user) {
        boolean active = true;
        while (active) {
            System.out.println("\n===============================================================");
            System.out.println(" CUSTOMER DASHBOARD: Logged in as " + user.getFullName() + " (" + user.getEmail() + ")");
            System.out.println("===============================================================");
            System.out.println(" 1. View My Profile");
            System.out.println(" 2. Browse Product Catalog");
            System.out.println(" 3. Search Products by Keyword");
            System.out.println(" 4. View / Manage Shopping Cart");
            System.out.println(" 5. Add Product to Cart");
            System.out.println(" 6. View / Manage Wishlist");
            System.out.println(" 7. Add Product to Wishlist");
            System.out.println(" 8. Checkout & Place Order");
            System.out.println(" 9. View My Order History");
            System.out.println(" 10. Logout");
            System.out.print("Choose an action (1-10): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    viewProfile(user);
                    break;
                case "2":
                    displayProducts(productDAO.findAllActive());
                    break;
                case "3":
                    searchProducts();
                    break;
                case "4":
                    viewCart(user);
                    break;
                case "5":
                    addToCart(user);
                    break;
                case "6":
                    viewWishlist(user);
                    break;
                case "7":
                    addToWishlist(user);
                    break;
                case "8":
                    checkoutOrder(user);
                    break;
                case "9":
                    viewOrderHistory(user);
                    break;
                case "10":
                    System.out.println("Logged out successfully.");
                    active = false;
                    break;
                default:
                    System.out.println("[!] Invalid choice.");
            }
        }
    }

    // ============================================================
    // ADMIN REGISTRATION & LOGIN (DYNAMIC CREDENTIALS)
    // ============================================================

    private static void handleAdminRegistration() {
        System.out.println("\n--- [ADMINISTRATOR REGISTRATION] ---");
        System.out.println("Create an administrator account with your desired credentials:");

        System.out.print("Enter Admin Username: ");
        String username = scanner.nextLine().trim();
        while (username.isEmpty()) {
            System.out.print("Username cannot be empty: ");
            username = scanner.nextLine().trim();
        }

        System.out.print("Enter Admin Email: ");
        String email = scanner.nextLine().trim().toLowerCase();

        System.out.print("Enter Admin Password: ");
        String password = scanner.nextLine();

        System.out.print("Enter Full Name: ");
        String fullName = scanner.nextLine().trim();

        System.out.print("Enter Role [SUPER_ADMIN / MANAGER, Default: MANAGER]: ");
        String role = scanner.nextLine().trim().toUpperCase();
        if (!"SUPER_ADMIN".equals(role) && !"MANAGER".equals(role)) {
            role = "MANAGER";
        }

        Admin newAdmin = new Admin(username, email, null, fullName, role);
        int adminId = adminDAO.createAdmin(newAdmin, password);

        if (adminId > 0) {
            System.out.println("\n[SUCCESS] Admin account created successfully!");
            System.out.println(" -> Admin ID: " + adminId);
            System.out.println(" -> Username: " + username);
            System.out.println(" -> Role: " + role);
            System.out.println("You can now login as Admin using Option 4.");
        } else {
            System.out.println("[ERROR] Failed to create admin account. Username or email may already exist.");
        }
    }

    private static void handleAdminLogin() {
        System.out.println("\n--- [ADMINISTRATOR LOGIN] ---");
        System.out.print("Enter Admin Username or Email: ");
        String usernameOrEmail = scanner.nextLine().trim();

        System.out.print("Enter Admin Password: ");
        String password = scanner.nextLine();

        Admin admin = adminDAO.authenticate(usernameOrEmail, password);
        if (admin != null) {
            System.out.println("\n[SUCCESS] Admin Authentication successful! Welcome, " + admin.getFullName() + " (" + admin.getRole() + ")");
            adminSession(admin);
        } else {
            System.out.println("\n[!] Invalid admin credentials. Access denied.");
        }
    }

    private static void adminSession(Admin admin) {
        boolean active = true;
        while (active) {
            System.out.println("\n===============================================================");
            System.out.println(" ADMIN CONSOLE: " + admin.getFullName() + " | Role: " + admin.getRole());
            System.out.println("===============================================================");
            System.out.println(" 1. View Dashboard & Live Business Metrics");
            System.out.println(" 2. View All Products");
            System.out.println(" 3. Add New Product to Catalog");
            System.out.println(" 4. Update Product Stock / Price");
            System.out.println(" 5. View All System Orders");
            System.out.println(" 6. Update Order Status (Processing / Shipped / Delivered)");
            System.out.println(" 7. Logout");
            System.out.print("Choose an action (1-7): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    Map<String, Object> stats = adminDAO.getDashboardStats();
                    System.out.println("\n--- REAL-TIME DASHBOARD STATS ---");
                    System.out.println(" • Total Registered Customers: " + stats.get("totalUsers"));
                    System.out.println(" • Total Active Products:     " + stats.get("totalProducts"));
                    System.out.println(" • Total Orders Placed:        " + stats.get("totalOrders"));
                    System.out.printf(" • Total Revenue:              ₹%.2f\n", stats.get("totalRevenue"));
                    System.out.println(" • Low Stock Alert (<10 items):" + stats.get("lowStockProducts"));
                    break;
                case "2":
                    displayProducts(productDAO.findAllActive());
                    break;
                case "3":
                    addNewProductAdmin();
                    break;
                case "4":
                    updateProductStockAdmin();
                    break;
                case "5":
                    viewAllOrdersAdmin();
                    break;
                case "6":
                    updateOrderStatusAdmin();
                    break;
                case "7":
                    System.out.println("Admin logged out successfully.");
                    active = false;
                    break;
                default:
                    System.out.println("[!] Invalid choice.");
            }
        }
    }

    // ============================================================
    // CUSTOMER FEATURES (CART, WISHLIST, CHECKOUT)
    // ============================================================

    private static void viewProfile(User user) {
        System.out.println("\n--- [USER PROFILE] ---");
        System.out.println("User ID:      " + user.getUserId());
        System.out.println("Full Name:    " + user.getFullName());
        System.out.println("Email:        " + user.getEmail());
        System.out.println("Phone:        " + user.getPhone());
        System.out.println("Address:      " + user.getAddress() + ", " + user.getCity() + ", " + user.getState() + " - " + user.getPostalCode());
        System.out.println("Country:      " + user.getCountry());
        System.out.println("Member Since: " + user.getCreatedAt());
    }

    private static void displayProducts(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("No products found.");
            return;
        }
        System.out.println("\n---------------------------------------------------------------------------------------------");
        System.out.printf("%-4s | %-32s | %-14s | %-10s | %-6s\n", "ID", "Product Name", "Brand", "Price (₹)", "Stock");
        System.out.println("---------------------------------------------------------------------------------------------");
        for (Product p : products) {
            String name = p.getProductName().length() > 30 ? p.getProductName().substring(0, 29) + "…" : p.getProductName();
            System.out.printf("%-4d | %-32s | %-14s | ₹%-9.2f | %-6d\n",
                    p.getProductId(), name, p.getBrand(), p.getPrice(), p.getStockQuantity());
        }
        System.out.println("---------------------------------------------------------------------------------------------");
    }

    private static void searchProducts() {
        System.out.print("\nEnter keyword to search (e.g., Apple, Shoes, Sony): ");
        String keyword = scanner.nextLine().trim();
        List<Product> results = productDAO.searchProducts(keyword);
        System.out.println("Search results for '" + keyword + "' (" + results.size() + " matches):");
        displayProducts(results);
    }

    private static void addToCart(User user) {
        System.out.print("\nEnter Product ID to add to cart: ");
        int productId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter Quantity: ");
        int qty = Integer.parseInt(scanner.nextLine().trim());

        if (qty <= 0) {
            System.out.println("[!] Quantity must be greater than zero.");
            return;
        }

        boolean ok = cartDAO.addItemToCart(user.getUserId(), productId, qty);
        if (ok) {
            System.out.println("[SUCCESS] Added item to your shopping cart!");
        } else {
            System.out.println("[!] Failed to add item. Check product ID and stock.");
        }
    }

    private static void viewCart(User user) {
        Cart cart = cartDAO.getOrCreateCart(user.getUserId());
        if (cart == null || cart.getItems().isEmpty()) {
            System.out.println("\nYour shopping cart is currently empty.");
            return;
        }

        System.out.println("\n--- [YOUR SHOPPING CART] (Cart ID: " + cart.getCartId() + ") ---");
        for (CartItem item : cart.getItems()) {
            System.out.printf(" • Product ID %d: %-30s | Qty: %d | Unit: ₹%.2f | Subtotal: ₹%.2f\n",
                    item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
        }
        System.out.printf(" TOTAL CART VALUE: ₹%.2f (%d items)\n", cart.getTotalAmount(), cart.getTotalItemCount());
    }

    private static void addToWishlist(User user) {
        System.out.print("\nEnter Product ID to add to Wishlist: ");
        int productId = Integer.parseInt(scanner.nextLine().trim());
        boolean ok = wishlistDAO.addToWishlist(user.getUserId(), productId);
        if (ok) {
            System.out.println("[SUCCESS] Saved to your Wishlist!");
        } else {
            System.out.println("[!] Item already in wishlist or invalid product ID.");
        }
    }

    private static void viewWishlist(User user) {
        List<Wishlist> list = wishlistDAO.getWishlistByUserId(user.getUserId());
        if (list.isEmpty()) {
            System.out.println("\nYour wishlist is currently empty.");
            return;
        }
        System.out.println("\n--- [YOUR WISHLIST] ---");
        for (Wishlist w : list) {
            System.out.printf(" • ID %d: %-30s | ₹%.2f | Stock: %d\n",
                    w.getProductId(), w.getProductName(), w.getPrice(), w.getStockQuantity());
        }
    }

    private static void checkoutOrder(User user) {
        Cart cart = cartDAO.getOrCreateCart(user.getUserId());
        if (cart == null || cart.getItems().isEmpty()) {
            System.out.println("\n[!] Your cart is empty. Add products to cart before checkout.");
            return;
        }

        System.out.printf("\n--- CHECKOUT (Total Payable: ₹%.2f) ---\n", cart.getTotalAmount());
        System.out.println("Select Payment Method:");
        System.out.println(" 1. Cash on Delivery (COD)");
        System.out.println(" 2. UPI (Google Pay, PhonePe, Paytm)");
        System.out.println(" 3. Credit / Debit Card");
        System.out.print("Choice (1-3): ");
        String payChoice = scanner.nextLine().trim();

        String paymentMethod = "COD";
        String paymentStatus = "PENDING";
        if ("2".equals(payChoice)) {
            paymentMethod = "UPI";
            paymentStatus = "PAID";
        } else if ("3".equals(payChoice)) {
            paymentMethod = "CARD";
            paymentStatus = "PAID";
        }

        System.out.print("Confirm shipping address [" + user.getAddress() + ", " + user.getCity() + "] (Press Enter to keep, or type new): ");
        String customAddr = scanner.nextLine().trim();
        String address = customAddr.isEmpty() ? user.getAddress() : customAddr;

        Orders order = new Orders();
        order.setUserId(user.getUserId());
        order.setTotalAmount(cart.getTotalAmount());
        order.setOrderStatus("CONFIRMED");
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(paymentStatus);
        order.setShippingAddress(address);
        order.setShippingCity(user.getCity());
        order.setShippingState(user.getState());
        order.setShippingPostalCode(user.getPostalCode());
        order.setShippingCountry(user.getCountry());

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setProductId(ci.getProductId());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getUnitPrice());
            oi.setSubtotal(ci.getSubtotal());
            orderItems.add(oi);
        }

        int orderId = orderDAO.placeOrder(order, orderItems, cart.getCartId());
        if (orderId > 0) {
            System.out.println("\n===============================================================");
            System.out.println(" [SUCCESS] ORDER PLACED SUCCESSFULLY via ACID TRANSACTION!");
            System.out.println("===============================================================");
            System.out.println(" Order ID:        " + orderId);
            System.out.printf(" Total Amount:    ₹%.2f\n", cart.getTotalAmount());
            System.out.println(" Payment Method:  " + paymentMethod + " (" + paymentStatus + ")");
            System.out.println(" Shipping To:     " + address + ", " + user.getCity());
            System.out.println(" Product stock automatically decremented in MySQL.");
            System.out.println(" Shopping cart has been cleared.");
        } else {
            System.out.println("[ERROR] Order placement failed. Insufficient stock or transaction aborted.");
        }
    }

    private static void viewOrderHistory(User user) {
        List<Orders> orders = orderDAO.findByUserId(user.getUserId());
        if (orders.isEmpty()) {
            System.out.println("\nYou have not placed any orders yet.");
            return;
        }

        System.out.println("\n--- [YOUR ORDER HISTORY] ---");
        for (Orders o : orders) {
            System.out.printf(" Order #%d | Date: %s | Status: %s | Total: ₹%.2f | Payment: %s (%s)\n",
                    o.getOrderId(), o.getOrderDate(), o.getOrderStatus(), o.getTotalAmount(), o.getPaymentMethod(), o.getPaymentStatus());
        }
    }

    private static void handleBrowseCatalogGuest() {
        System.out.println("\n--- [ALL CATEGORIES & PRODUCTS] ---");
        List<Category> categories = categoryDAO.findAllActive();
        for (Category c : categories) {
            System.out.println("► Category: " + c.getCategoryName() + " - " + c.getDescription());
        }
        displayProducts(productDAO.findAllActive());
    }

    // ============================================================
    // ADMIN ACTIONS
    // ============================================================

    private static void addNewProductAdmin() {
        System.out.println("\n--- [ADD NEW PRODUCT] ---");
        System.out.print("Select Category ID (1=Electronics, 2=Fashion, 3=Home, 4=Sports): ");
        int catId = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Product Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Brand: ");
        String brand = scanner.nextLine().trim();

        System.out.print("Description: ");
        String desc = scanner.nextLine().trim();

        System.out.print("Price (₹): ");
        double price = Double.parseDouble(scanner.nextLine().trim());

        System.out.print("Stock Quantity: ");
        int stock = Integer.parseInt(scanner.nextLine().trim());

        Product p = new Product(catId, name, brand, desc, price, stock, "https://example.com/product.jpg");
        int newId = productDAO.addProduct(p);
        if (newId > 0) {
            System.out.println("[SUCCESS] Product added with ID: " + newId);
        } else {
            System.out.println("[ERROR] Failed to add product.");
        }
    }

    private static void updateProductStockAdmin() {
        System.out.print("\nEnter Product ID to update: ");
        int prodId = Integer.parseInt(scanner.nextLine().trim());
        Product p = productDAO.findById(prodId);
        if (p == null) {
            System.out.println("[!] Product not found.");
            return;
        }
        System.out.printf("Current: %s | Price: ₹%.2f | Stock: %d\n", p.getProductName(), p.getPrice(), p.getStockQuantity());
        System.out.print("New Price (or press Enter to keep current): ");
        String pr = scanner.nextLine().trim();
        if (!pr.isEmpty()) p.setPrice(Double.parseDouble(pr));

        System.out.print("New Stock (or press Enter to keep current): ");
        String st = scanner.nextLine().trim();
        if (!st.isEmpty()) p.setStockQuantity(Integer.parseInt(st));

        boolean ok = productDAO.updateProduct(p);
        if (ok) {
            System.out.println("[SUCCESS] Product updated successfully!");
        } else {
            System.out.println("[ERROR] Failed to update product.");
        }
    }

    private static void viewAllOrdersAdmin() {
        List<Orders> orders = orderDAO.findAllOrders();
        System.out.println("\n--- [ALL SYSTEM ORDERS (" + orders.size() + " total)] ---");
        for (Orders o : orders) {
            System.out.printf(" Order #%d | Customer ID: %d | Status: %-11s | Total: ₹%-9.2f | Tracking: %s\n",
                    o.getOrderId(), o.getUserId(), o.getOrderStatus(), o.getTotalAmount(), o.getTrackingNumber());
        }
    }

    private static void updateOrderStatusAdmin() {
        System.out.print("\nEnter Order ID to update: ");
        int orderId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Enter New Status (CONFIRMED / PROCESSING / SHIPPED / DELIVERED / CANCELLED): ");
        String status = scanner.nextLine().trim().toUpperCase();

        boolean ok = orderDAO.updateOrderStatus(orderId, status);
        if (ok) {
            System.out.println("[SUCCESS] Order #" + orderId + " updated to " + status);
        } else {
            System.out.println("[ERROR] Failed to update order status.");
        }
    }
}
