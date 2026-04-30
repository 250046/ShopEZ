CREATE DATABASE IF NOT EXISTS shopez_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shopez_db;

-- ─────────────────────────────────────────────
-- USERS (covers Customer, Seller, Admin in one table)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    user_id         INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            ENUM('CUSTOMER','SELLER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    -- Customer-specific
    shipping_address VARCHAR(300),
    wallet_balance  DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    -- Seller-specific
    store_name      VARCHAR(100),
    is_approved     TINYINT(1)      NOT NULL DEFAULT 0,
    is_banned       TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────
-- PRODUCTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    product_id      INT AUTO_INCREMENT PRIMARY KEY,
    seller_id       INT             NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2)   NOT NULL,
    stock_qty       INT             NOT NULL DEFAULT 0,
    category        VARCHAR(100),
    product_type    ENUM('PHYSICAL','DIGITAL') NOT NULL DEFAULT 'PHYSICAL',
    -- Physical product fields
    weight_kg       DECIMAL(6,2),
    dimensions      VARCHAR(100),
    -- Digital product fields
    download_url    VARCHAR(500),
    license_key     VARCHAR(100),
    -- Image
    image_path      VARCHAR(500),
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────
-- CARTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS carts (
    cart_id         INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT             NOT NULL UNIQUE,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id    INT AUTO_INCREMENT PRIMARY KEY,
    cart_id         INT             NOT NULL,
    product_id      INT             NOT NULL,
    quantity        INT             NOT NULL DEFAULT 1,
    unit_price      DECIMAL(10,2)   NOT NULL,
    CONSTRAINT fk_cartitem_cart    FOREIGN KEY (cart_id)    REFERENCES carts(cart_id)    ON DELETE CASCADE,
    CONSTRAINT fk_cartitem_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    UNIQUE KEY uq_cart_product (cart_id, product_id)
);

-- ─────────────────────────────────────────────
-- ORDERS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    order_id        INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT             NOT NULL,
    total_amount    DECIMAL(10,2)   NOT NULL,
    status          ENUM('PENDING','PROCESSING','SHIPPED','DELIVERED','CANCELLED')
                                    NOT NULL DEFAULT 'PENDING',
    shipping_address VARCHAR(300),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id   INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT             NOT NULL,
    product_id      INT             NOT NULL,
    quantity        INT             NOT NULL,
    price_at_purchase DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_orderitem_order   FOREIGN KEY (order_id)   REFERENCES orders(order_id)   ON DELETE CASCADE,
    CONSTRAINT fk_orderitem_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT
);

-- ─────────────────────────────────────────────
-- PAYMENTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT             NOT NULL UNIQUE,
    payment_method  ENUM('CREDIT_CARD','WALLET') NOT NULL,
    amount          DECIMAL(10,2)   NOT NULL,
    status          ENUM('PENDING','COMPLETED','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    -- Credit card fields (nullable)
    masked_card     VARCHAR(20),
    card_holder     VARCHAR(100),
    -- Wallet fields (nullable)
    wallet_snapshot DECIMAL(10,2),
    paid_at         TIMESTAMP,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id)
        REFERENCES orders(order_id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────
-- REVIEWS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    review_id       INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT             NOT NULL,
    product_id      INT             NOT NULL,
    rating          TINYINT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_review (customer_id, product_id),
    CONSTRAINT fk_review_customer FOREIGN KEY (customer_id) REFERENCES users(user_id)    ON DELETE CASCADE,
    CONSTRAINT fk_review_product  FOREIGN KEY (product_id)  REFERENCES products(product_id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────
-- STOCK WATCHERS (Observer pattern persistence)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_watchers (
    watcher_id      INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT             NOT NULL,
    product_id      INT             NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_watcher (customer_id, product_id),
    CONSTRAINT fk_watcher_customer FOREIGN KEY (customer_id) REFERENCES users(user_id)    ON DELETE CASCADE,
    CONSTRAINT fk_watcher_product  FOREIGN KEY (product_id)  REFERENCES products(product_id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────
-- NOTIFICATIONS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    notif_id        INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT             NOT NULL,
    message         VARCHAR(500)    NOT NULL,
    is_read         TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────
-- SEED DATA
-- ─────────────────────────────────────────────

INSERT INTO users (name, email, password_hash, role, is_approved) VALUES
('Admin',        'admin@shopez.com', SHA2('admin123',    256), 'ADMIN',    1),
('Alice Rahman', 'alice@shopez.com', SHA2('seller123',   256), 'SELLER',   1),
('Bob Karimov',  'bob@shopez.com',   SHA2('seller123',   256), 'SELLER',   1),
('John Smith',   'john@shopez.com',  SHA2('customer123', 256), 'CUSTOMER', 1),
('Sara Lee',     'sara@shopez.com',  SHA2('customer123', 256), 'CUSTOMER', 1),
('Timur Yusupov','timur@shopez.com', SHA2('customer123', 256), 'CUSTOMER', 1);

UPDATE users SET store_name = 'AliceTech',   wallet_balance = 0.00  WHERE email = 'alice@shopez.com';
UPDATE users SET store_name = 'BobBooks',    wallet_balance = 0.00  WHERE email = 'bob@shopez.com';
UPDATE users SET shipping_address = '123 Main St, Tashkent', wallet_balance = 150.00 WHERE email = 'john@shopez.com';
UPDATE users SET shipping_address = '45 Oak Ave, Tashkent',  wallet_balance = 75.00  WHERE email = 'sara@shopez.com';
UPDATE users SET shipping_address = '7 Tech Blvd, Tashkent', wallet_balance = 200.00 WHERE email = 'timur@shopez.com';

-- Products (seller_id 2 = Alice, seller_id 3 = Bob)
INSERT INTO products (seller_id, name, description, price, stock_qty, category, product_type, weight_kg, dimensions) VALUES
(2, 'Mechanical Keyboard', 'TKL layout, Cherry MX Blue switches, RGB backlit', 89.99, 15, 'Electronics', 'PHYSICAL', 0.85, '36x13x4 cm'),
(2, 'USB-C Hub 7-in-1',    '4K HDMI, 3x USB-A, SD card, PD charging',          34.99, 30, 'Electronics', 'PHYSICAL', 0.12, '11x4x1.5 cm'),
(3, 'Wireless Mouse',      'Ergonomic, 2.4GHz, 1600 DPI adjustable',            45.00, 20, 'Electronics', 'PHYSICAL', 0.10, '12x6x4 cm');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, product_type, download_url, license_key) VALUES
(2, 'Java OOP Masterclass', 'Complete 12-hour video course covering all OOP concepts in Java 17', 24.99, 999, 'Education', 'DIGITAL', 'https://courses.shopez.com/java-oop', NULL),
(3, 'Clean Code (eBook)',   'Robert C. Martin guide to writing readable, maintainable code',      14.99, 999, 'Education', 'DIGITAL', 'https://books.shopez.com/clean-code',  NULL);

-- ─────────────────────────────────────────────
-- INDEXES
-- ─────────────────────────────────────────────
CREATE INDEX idx_products_seller   ON products(seller_id);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_orders_customer   ON orders(customer_id);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_notif_user        ON notifications(user_id, is_read);
