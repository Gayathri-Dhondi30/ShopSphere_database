-- ============================================================
-- ShopSphere E-Commerce Application - Database Schema
-- Database: MySQL 8.0+
-- File: shopsphere.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS shopsphere
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE shopsphere;

-- ------------------------------------------------------------
-- Drop tables in reverse order of foreign key dependencies
-- ------------------------------------------------------------
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS wishlists;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS users;

-- ============================================================
-- Table: users
-- Represents registered customer accounts
-- ============================================================
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'India',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- Table: admins
-- Represents store administrators and managers
-- ============================================================
CREATE TABLE admins (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('SUPER_ADMIN', 'MANAGER') NOT NULL DEFAULT 'MANAGER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
) ENGINE=InnoDB;

-- ============================================================
-- Table: categories
-- Product categorization hierarchy
-- ============================================================
CREATE TABLE categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- Table: products
-- Catalog items with brand, price, and inventory tracking
-- ============================================================
CREATE TABLE products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_product_price CHECK (price >= 0),
    CONSTRAINT chk_product_stock CHECK (stock_quantity >= 0),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories(category_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- Table: carts
-- Active shopping cart for each user (1-to-1 relationship)
-- ============================================================
CREATE TABLE carts (
    cart_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- Table: cart_items
-- Items stored inside a user's shopping cart
-- ============================================================
CREATE TABLE cart_items (
    cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
    cart_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cart_quantity CHECK (quantity > 0),
    CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id)
        REFERENCES carts(cart_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- Table: wishlists
-- Saved-for-later items per customer
-- ============================================================
CREATE TABLE wishlists (
    wishlist_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_wishlist_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_wishlists_product FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- Table: orders
-- Customer order master record with shipping & payment info
-- ============================================================
CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10, 2) NOT NULL,
    order_status ENUM('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    payment_method ENUM('COD', 'CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'NET_BANKING') NOT NULL DEFAULT 'COD',
    payment_status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    shipping_address VARCHAR(255) NOT NULL,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_state VARCHAR(100) NOT NULL,
    shipping_postal_code VARCHAR(20) NOT NULL,
    shipping_country VARCHAR(100) NOT NULL DEFAULT 'India',
    tracking_number VARCHAR(100),
    notes TEXT,
    CONSTRAINT chk_order_total CHECK (total_amount >= 0),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- Table: order_items
-- Individual line items in an order (captures historical unit price)
-- ============================================================
CREATE TABLE order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    CONSTRAINT chk_order_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_order_item_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_order_item_subtotal CHECK (subtotal >= 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products(product_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- Indexes for Performance
-- ============================================================
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(order_status);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);

-- ============================================================
-- Sample Data Insertion
-- Password Hashes generated via SHA-256 for demo test passwords:
-- 'Admin@123' -> e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7
-- 'Customer@123' -> 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8
-- ============================================================

-- 1. Admins
INSERT INTO admins (username, email, password_hash, full_name, role, is_active)
VALUES
('admin', 'admin@shopsphere.com', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', 'System Administrator', 'SUPER_ADMIN', TRUE),
('manager1', 'manager@shopsphere.com', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', 'Store Operations Manager', 'MANAGER', TRUE);

-- 2. Categories
INSERT INTO categories (category_id, category_name, description, image_url, is_active)
VALUES
(1, 'Electronics', 'Smartphones, laptops, audio gear, and electronic gadgets', 'https://images.unsplash.com/photo-1519389950473-47ba0277781c', TRUE),
(2, 'Fashion', 'Men and Women apparel, footwear, and accessories', 'https://images.unsplash.com/photo-1445205170230-053b83016050', TRUE),
(3, 'Home & Kitchen', 'Kitchen appliances, cookware, and modern home decor', 'https://images.unsplash.com/photo-1556911220-e15b29be8c8f', TRUE),
(4, 'Sports & Fitness', 'Fitness gear, gym equipment, sportswear, and outdoors', 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd', TRUE),
(5, 'Books', 'Fiction, non-fiction, academic, and competitive exam books', 'https://images.unsplash.com/photo-1512820790803-83ca734da794', TRUE);

-- 3. Products
INSERT INTO products (product_id, category_id, product_name, brand, description, price, stock_quantity, image_url, is_active)
VALUES
-- Electronics
(1, 1, 'iPhone 15 Pro (128GB, Natural Titanium)', 'Apple', 'A17 Pro chip, titanium design with textured matte glass back, 48MP main camera.', 129900.00, 25, 'https://images.unsplash.com/photo-1695048133142-1a20484d2569', TRUE),
(2, 1, 'Galaxy S24 Ultra (256GB, Titanium Gray)', 'Samsung', 'Snapdragon 8 Gen 3, Quad Telephoto camera with Galaxy AI, S Pen included.', 119999.00, 30, 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c', TRUE),
(3, 1, 'WH-1000XM5 Wireless Noise Cancelling Headphones', 'Sony', 'Industry leading noise cancellation, 30-hour battery life, crystal clear hands-free calling.', 29990.00, 45, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e', TRUE),
(4, 1, 'MacBook Air M2 (13.6-inch, 8GB RAM, 256GB SSD)', 'Apple', 'Strikingly thin design, 13.6-inch Liquid Retina display, up to 18 hours battery life.', 94900.00, 18, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8', TRUE),

-- Fashion
(5, 2, 'Air Max 270 Running Shoes', 'Nike', 'Lifestyle athletic shoe featuring Nike largest heel Air unit yet for super soft bounce.', 11495.00, 50, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff', TRUE),
(6, 2, 'Ultraboost Light Running Shoes', 'Adidas', 'Epic energy in every stride with 30% lighter BOOST material and Continental Rubber outsole.', 14999.00, 40, 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2', TRUE),
(7, 2, 'Men Regular Fit Denim Jacket', 'Levi\'s', 'Iconic trucker jacket in rigid 100% cotton denim with button flap chest pockets.', 4999.00, 60, 'https://images.unsplash.com/photo-1576995853123-5a10305d93c0', TRUE),
(8, 2, 'Classic Leather Chronograph Watch', 'Fossil', 'Stainless steel case with brown genuine leather strap and mineral crystal lens.', 8995.00, 35, 'https://images.unsplash.com/photo-1524805444758-089113d48a6d', TRUE),

-- Home & Kitchen
(9, 3, 'Smart Aroma Essential Oil Diffuser (500ml)', 'Philips', 'Ultrasonic cool mist humidifier with ambient LED lighting and auto shut-off.', 2499.00, 75, 'https://images.unsplash.com/photo-1608571423902-eed4a5ad8108', TRUE),
(10, 3, 'Cast Iron Dutch Oven Casserole Pot (5.5 Qt)', 'Prestige', 'Heavy-duty enameled cast iron for superior heat retention and uniform cooking.', 3899.00, 20, 'https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7', TRUE),
(11, 3, 'Digital Air Fryer with Rapid Air Tech (4.2L)', 'Philips', 'Fry with up to 90% less fat. 7 preset touch screen cooking menus for quick snacks.', 7999.00, 28, 'https://images.unsplash.com/photo-1594385208974-2e75f8d7bb48', TRUE),

-- Sports & Fitness
(12, 4, 'Adjustable Dumbbell Set (20 KG with Case)', 'Kore', 'Rubberized weight plates with heavy duty chrome threaded bars and spinlock collars.', 3499.00, 40, 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2', TRUE),
(13, 4, 'High Density Yoga & Exercise Mat (6mm)', 'Boldfit', 'Anti-tear eco-friendly TPE material with non-slip dual texture and carry strap.', 1299.00, 100, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f', TRUE),

-- Books
(14, 5, 'Atomic Habits: An Easy & Proven Way to Build Good Habits', 'James Clear', 'The #1 New York Times bestseller on tiny changes, remarkable results.', 599.00, 120, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f', TRUE),
(15, 5, 'The Psychology of Money', 'Morgan Housel', 'Timeless lessons on wealth, greed, and happiness doing well with money.', 450.00, 95, 'https://images.unsplash.com/photo-1592496431122-2349e0fbc666', TRUE);

-- 4. Sample Users
INSERT INTO users (user_id, full_name, email, password_hash, phone, address, city, state, postal_code, country)
VALUES
(1, 'Rahul Sharma', 'rahul.sharma@example.com', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', '+91 9876543210', 'Flat 402, Green Glen Layout, Bellandur', 'Bengaluru', 'Karnataka', '560103', 'India'),
(2, 'Priya Patel', 'priya.patel@example.com', '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', '+91 9812345678', 'B-12, Shanti Kunj, Satellite Road', 'Ahmedabad', 'Gujarat', '380015', 'India');

-- 5. Carts for Users
INSERT INTO carts (cart_id, user_id)
VALUES
(1, 1),
(2, 2);

-- 6. Cart Items (Rahul has Sony headphones and Atomic Habits in his cart)
INSERT INTO cart_items (cart_item_id, cart_id, product_id, quantity)
VALUES
(1, 1, 3, 1),
(2, 1, 14, 2);

-- 7. Wishlists (Priya saved MacBook Air and Fossil watch)
INSERT INTO wishlists (wishlist_id, user_id, product_id)
VALUES
(1, 2, 4),
(2, 2, 8);

-- 8. Sample Placed Orders
-- Order 1: Rahul placed an order paid via COD
INSERT INTO orders (order_id, user_id, order_date, total_amount, order_status, payment_method, payment_status, shipping_address, shipping_city, shipping_state, shipping_postal_code, tracking_number, notes)
VALUES
(1001, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), 11495.00, 'DELIVERED', 'COD', 'PAID', 'Flat 402, Green Glen Layout, Bellandur', 'Bengaluru', 'Karnataka', '560103', 'TRK-IN-981273', 'Please call before delivery');

INSERT INTO order_items (order_item_id, order_id, product_id, quantity, unit_price, subtotal)
VALUES
(1, 1001, 5, 1, 11495.00, 11495.00);

-- Order 2: Priya placed an order paid via UPI
INSERT INTO orders (order_id, user_id, order_date, total_amount, order_status, payment_method, payment_status, shipping_address, shipping_city, shipping_state, shipping_postal_code, tracking_number, notes)
VALUES
(1002, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), 9298.00, 'PROCESSING', 'UPI', 'PAID', 'B-12, Shanti Kunj, Satellite Road', 'Ahmedabad', 'Gujarat', '380015', 'TRK-IN-981550', 'Leave at security desk if unavailable');

INSERT INTO order_items (order_item_id, order_id, product_id, quantity, unit_price, subtotal)
VALUES
(2, 1002, 11, 1, 7999.00, 7999.00),
(3, 1002, 13, 1, 1299.00, 1299.00);

-- ------------------------------------------------------------
-- End of Script
-- ------------------------------------------------------------
