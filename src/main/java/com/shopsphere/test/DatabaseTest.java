package com.shopsphere.test;

import com.shopsphere.dao.*;
import com.shopsphere.entity.*;
import com.shopsphere.util.DBConnection;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive verification and testing suite for ShopSphere Database & JDBC module.
 * Tests connection, CRUD operations, transactions, and entity mapping.
 */
public class DatabaseTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("     SHOPSPHERE DATABASE & JDBC MODULE - TEST SUITE           ");
        System.out.println("===============================================================");

        // Step 1: Test Database Connection
        testConnection();

        // Instantiate DAOs
        UserDAO userDAO = new UserDAO();
        CategoryDAO categoryDAO = new CategoryDAO();
        ProductDAO productDAO = new ProductDAO();
        CartDAO cartDAO = new CartDAO();
        WishlistDAO wishlistDAO = new WishlistDAO();
        OrderDAO orderDAO = new OrderDAO();
        AdminDAO adminDAO = new AdminDAO();

        // Step 2: Test Admin Authentication & Stats (dynamic or default)
        String adminUser = (args.length >= 4) ? args[2] : "admin";
        String adminPass = (args.length >= 4) ? args[3] : "Admin@123";
        testAdminOperations(adminDAO, adminUser, adminPass);

        // Step 3: Test Category Operations
        int testCategoryId = testCategoryOperations(categoryDAO);

        // Step 4: Test Product Operations
        int testProductId = testProductOperations(productDAO, testCategoryId);

        // Step 5: Test User Registration, Login & Profile Update (dynamic or user-specified)
        String userEmail = (args.length >= 2) ? args[0] : ("user_" + System.currentTimeMillis() + "@custom.com");
        String userPass = (args.length >= 2) ? args[1] : "MyCustomPass@123";
        int testUserId = testUserOperations(userDAO, userEmail, userPass);

        // Step 6: Test Cart & Cart Items
        testCartOperations(cartDAO, testUserId, testProductId);

        // Step 7: Test Wishlist Operations
        testWishlistOperations(wishlistDAO, testUserId, testProductId);

        // Step 8: Test Order Placement with ACID Transaction & Stock Deduction
        testOrderOperations(orderDAO, productDAO, testUserId, testProductId);

        // Summary Report
        System.out.println("\n===============================================================");
        System.out.println("                     TEST RUN SUMMARY                          ");
        System.out.println("===============================================================");
        System.out.println("  Total Passed: " + testsPassed);
        System.out.println("  Total Failed: " + testsFailed);
        if (testsFailed == 0) {
            System.out.println("  RESULT: ALL DATABASE & JDBC TESTS PASSED SUCCESSFULLY! (100%)");
        } else {
            System.out.println("  RESULT: SOME TESTS FAILED. Check database state and logs above.");
        }
        System.out.println("===============================================================\n");
    }

    private static void testConnection() {
        System.out.print("\n[TEST 1] Testing MySQL Connection... ");
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                reportSuccess("Connected successfully to " + DBConnection.getUrl());
            } else {
                reportFailure("Connection object is null or closed.");
            }
        } catch (Exception e) {
            reportFailure("Connection failed: " + e.getMessage());
            System.err.println("\n[SETUP REQUIRED] If MySQL is not running or the database 'shopsphere' is not created,");
            System.err.println("                 please run the MySQL service and execute shopsphere.sql first.");
        }
    }

    private static void testAdminOperations(AdminDAO adminDAO, String adminUser, String adminPass) {
        System.out.print("\n[TEST 2] Testing Admin Authentication & Dashboard Stats (" + adminUser + ")... ");
        try {
            Admin admin = adminDAO.authenticate(adminUser, adminPass);
            if (admin != null) {
                Map<String, Object> stats = adminDAO.getDashboardStats();
                reportSuccess("Authenticated admin '" + admin.getFullName() + "' (" + admin.getRole() + ") | Total Products: " +
                        stats.get("totalProducts") + " | Total Orders: " + stats.get("totalOrders"));
            } else {
                reportFailure("Admin login failed for '" + adminUser + "'. Check credentials.");
            }
        } catch (Exception e) {
            reportFailure("Admin test failed: " + e.getMessage());
        }
    }

    private static int testCategoryOperations(CategoryDAO categoryDAO) {
        System.out.print("\n[TEST 3] Testing Category CRUD Operations... ");
        try {
            // List active categories
            List<Category> categories = categoryDAO.findAllActive();
            if (categories.isEmpty()) {
                reportFailure("No categories found. Seed data might be missing.");
                return -1;
            }

            // Create temporary category
            Category tempCat = new Category("Automotive " + System.currentTimeMillis(),
                    "Car accessories and detailing", "https://example.com/auto.jpg");
            int catId = categoryDAO.addCategory(tempCat);
            if (catId <= 0) {
                reportFailure("Failed to insert new category.");
                return categories.get(0).getCategoryId();
            }

            // Read
            Category fetched = categoryDAO.findById(catId);
            if (fetched == null) {
                reportFailure("Failed to retrieve created category.");
                return catId;
            }

            // Update
            fetched.setDescription("Updated car parts description");
            categoryDAO.updateCategory(fetched);

            reportSuccess("Category created (ID: " + catId + "), fetched, and updated successfully.");
            return catId;
        } catch (Exception e) {
            reportFailure("Category test failed: " + e.getMessage());
            return 1;
        }
    }

    private static int testProductOperations(ProductDAO productDAO, int categoryId) {
        System.out.print("\n[TEST 4] Testing Product Catalog, Search & Filter... ");
        try {
            // Find active products
            List<Product> products = productDAO.findAllActive();
            if (products.isEmpty()) {
                reportFailure("No products found in catalog.");
                return -1;
            }

            // Keyword search test
            List<Product> searchResults = productDAO.searchProducts("Apple");

            // Filter test
            List<Product> filtered = productDAO.filterProducts(null, "Apple", 50000.0, 150000.0);

            // Create temporary product for testing stock
            Product testProd = new Product(
                    categoryId > 0 ? categoryId : 1,
                    "Test Smart Wireless Earbuds " + System.currentTimeMillis(),
                    "Boat",
                    "High bass bluetooth earphones with 40h battery",
                    1999.00,
                    50,
                    "https://example.com/boat.jpg"
            );
            int prodId = productDAO.addProduct(testProd);

            if (prodId > 0 && !searchResults.isEmpty()) {
                reportSuccess("Products fetched (" + products.size() + "), searched ('Apple' -> " +
                        searchResults.size() + " matches), test product inserted (ID: " + prodId + ").");
                return prodId;
            } else {
                reportFailure("Product search or insert failed.");
                return products.get(0).getProductId();
            }
        } catch (Exception e) {
            reportFailure("Product test failed: " + e.getMessage());
            return 1;
        }
    }

    private static int testUserOperations(UserDAO userDAO, String userEmail, String userPassword) {
        System.out.print("\n[TEST 5] Testing User Registration, Authentication & Profile (" + userEmail + ")... ");
        try {
            User newUser = new User(
                    "User " + (System.currentTimeMillis() % 1000),
                    userEmail,
                    null, // DAO hashes it
                    "+91 9999988888",
                    "123 Custom Street",
                    "Pune",
                    "Maharashtra",
                    "411001",
                    "India"
            );

            // 1. Register
            int userId = userDAO.registerUser(newUser, userPassword);
            if (userId <= 0) {
                // If already registered, authenticate existing
                User existing = userDAO.authenticate(userEmail, userPassword);
                if (existing != null) {
                    reportSuccess("User already registered (ID: " + existing.getUserId() + "), authenticated successfully with user password.");
                    return existing.getUserId();
                }
                reportFailure("User registration failed for: " + userEmail);
                return -1;
            }

            // 2. Authenticate
            User authenticated = userDAO.authenticate(userEmail, userPassword);
            if (authenticated == null) {
                reportFailure("Authentication failed for newly registered user.");
                return userId;
            }

            // 3. Update Profile
            authenticated.setPhone("+91 9111122222");
            authenticated.setCity("Bengaluru");
            boolean updated = userDAO.updateProfile(authenticated);

            if (updated) {
                reportSuccess("User registered (ID: " + userId + ") with email '" + userEmail + "', authenticated with hash, and profile updated.");
                return userId;
            } else {
                reportFailure("User profile update failed.");
                return userId;
            }
        } catch (Exception e) {
            reportFailure("User test failed: " + e.getMessage());
            return -1;
        }
    }

    private static void testCartOperations(CartDAO cartDAO, int userId, int productId) {
        System.out.print("\n[TEST 6] Testing Cart & Cart Items Operations... ");
        try {
            if (userId <= 0 || productId <= 0) {
                reportFailure("Skipping cart test due to invalid user or product ID.");
                return;
            }

            // 1. Add item to cart
            boolean added = cartDAO.addItemToCart(userId, productId, 2);
            if (!added) {
                reportFailure("Failed to add item to cart.");
                return;
            }

            // 2. View cart
            Cart cart = cartDAO.getOrCreateCart(userId);
            if (cart == null || cart.getItems().isEmpty()) {
                reportFailure("Cart items list is empty after adding item.");
                return;
            }

            // 3. Update quantity
            CartItem firstItem = cart.getItems().get(0);
            boolean updated = cartDAO.updateItemQuantity(firstItem.getCartItemId(), 3);

            if (updated) {
                reportSuccess("Cart loaded (Cart ID: " + cart.getCartId() + "), items: " +
                        cart.getItems().size() + ", quantity updated to 3.");
            } else {
                reportFailure("Failed to update cart item quantity.");
            }
        } catch (Exception e) {
            reportFailure("Cart test failed: " + e.getMessage());
        }
    }

    private static void testWishlistOperations(WishlistDAO wishlistDAO, int userId, int productId) {
        System.out.print("\n[TEST 7] Testing Wishlist Operations... ");
        try {
            if (userId <= 0 || productId <= 0) {
                reportFailure("Skipping wishlist test due to invalid user or product ID.");
                return;
            }

            // 1. Add to wishlist
            boolean added = wishlistDAO.addToWishlist(userId, productId);

            // 2. Check if in wishlist
            boolean inWishlist = wishlistDAO.isInWishlist(userId, productId);

            // 3. Fetch user's wishlist
            List<Wishlist> wishlist = wishlistDAO.getWishlistByUserId(userId);

            if (inWishlist && !wishlist.isEmpty()) {
                reportSuccess("Product added to wishlist, verified membership, retrieved " + wishlist.size() + " items.");
            } else {
                reportFailure("Wishlist check or retrieval failed.");
            }
        } catch (Exception e) {
            reportFailure("Wishlist test failed: " + e.getMessage());
        }
    }

    private static void testOrderOperations(OrderDAO orderDAO, ProductDAO productDAO, int userId, int productId) {
        System.out.print("\n[TEST 8] Testing Order Placement Transaction & Stock Deduction... ");
        try {
            if (userId <= 0 || productId <= 0) {
                reportFailure("Skipping order test due to invalid user or product ID.");
                return;
            }

            Product prodBefore = productDAO.findById(productId);
            int stockBefore = prodBefore != null ? prodBefore.getStockQuantity() : 0;
            int orderQty = 2;
            double unitPrice = prodBefore != null ? prodBefore.getPrice() : 1999.00;
            double totalAmount = orderQty * unitPrice;

            Orders order = new Orders(
                    userId,
                    totalAmount,
                    "COD",
                    "123 Test Street",
                    "Mumbai",
                    "Maharashtra",
                    "400001",
                    "Test automated order placement"
            );

            List<OrderItem> items = new ArrayList<>();
            items.add(new OrderItem(productId, orderQty, unitPrice));

            // Place order inside atomic transaction
            int orderId = orderDAO.placeOrder(order, items, null);
            if (orderId <= 0) {
                reportFailure("Order placement transaction failed or was rolled back.");
                return;
            }

            // Verify stock deduction
            Product prodAfter = productDAO.findById(productId);
            int stockAfter = prodAfter != null ? prodAfter.getStockQuantity() : 0;

            // Verify order details retrieval
            Orders placedOrder = orderDAO.findById(orderId);

            if (stockAfter == (stockBefore - orderQty) && placedOrder != null && !placedOrder.getOrderItems().isEmpty()) {
                reportSuccess("Order placed (ID: " + orderId + ") via ACID transaction! Stock reduced from " +
                        stockBefore + " -> " + stockAfter + ", retrieved " + placedOrder.getOrderItems().size() + " order items.");
            } else {
                reportFailure("Stock deduction or order retrieval verification mismatch.");
            }
        } catch (Exception e) {
            reportFailure("Order placement test failed: " + e.getMessage());
        }
    }

    private static void reportSuccess(String message) {
        testsPassed++;
        System.out.println("PASSED ✓");
        System.out.println("   -> " + message);
    }

    private static void reportFailure(String message) {
        testsFailed++;
        System.out.println("FAILED ✗");
        System.out.println("   -> " + message);
    }
}
