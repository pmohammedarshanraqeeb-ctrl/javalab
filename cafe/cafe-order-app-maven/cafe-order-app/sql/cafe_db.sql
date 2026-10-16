-- Database for the Cafe Order App
CREATE DATABASE IF NOT EXISTS cafe_db;
USE cafe_db;

-- The menu shown as buttons in the app
CREATE TABLE IF NOT EXISTS menu_items (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(60)  NOT NULL UNIQUE,
    category VARCHAR(30)  NOT NULL,           -- each category becomes a tab
    price    DECIMAL(8,2) NOT NULL
);

-- One row per placed order
CREATE TABLE IF NOT EXISTS orders (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    total      DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One row per item line inside an order
CREATE TABLE IF NOT EXISTS order_items (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    order_id     INT          NOT NULL,
    menu_item_id INT          NOT NULL,
    quantity     INT          NOT NULL,
    unit_price   DECIMAL(8,2) NOT NULL,       -- price at the time of the order
    FOREIGN KEY (order_id)     REFERENCES orders(id),
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

-- Starter menu (INSERT IGNORE means re-running this script will not create duplicates)
INSERT IGNORE INTO menu_items (name, category, price) VALUES
    ('Espresso',        'Coffee',      2.50),
    ('Cappuccino',      'Coffee',      3.50),
    ('Latte',           'Coffee',      3.75),
    ('Iced Coffee',     'Coffee',      3.50),
    ('Black Tea',       'Tea',         2.00),
    ('Green Tea',       'Tea',         2.25),
    ('Lemonade',        'Cold Drinks', 2.75),
    ('Orange Juice',    'Cold Drinks', 3.00),
    ('Croissant',       'Snacks',      2.75),
    ('Blueberry Muffin','Snacks',      2.50),
    ('Sandwich',        'Snacks',      4.50),
    ('Brownie',         'Snacks',      3.00);

-- To see all saved orders, run:
-- SELECT o.id AS order_no, o.created_at, m.name, oi.quantity, oi.unit_price, o.total
-- FROM orders o
-- JOIN order_items oi ON oi.order_id = o.id
-- JOIN menu_items m   ON m.id = oi.menu_item_id
-- ORDER BY o.id DESC;
