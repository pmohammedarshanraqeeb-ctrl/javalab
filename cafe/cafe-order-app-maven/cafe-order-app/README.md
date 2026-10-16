# Cafe Order App (Java Swing + MySQL + Maven)

Click menu items to build an order, then press **Place Order** to save it in MySQL.

## Requirements
- JDK 11 or newer
- MySQL Server (running)
- IntelliJ IDEA, Eclipse, or Maven on the command line

## Setup
1. Create the database, tables and starter menu:
   `mysql -u root -p < sql/cafe_db.sql`
2. Open `src/main/java/com/example/cafe/CafeApp.java` and change `DB_USER` and `DB_PASS`
   to your own MySQL login.
3. Open this folder as a Maven project (IntelliJ: File -> Open -> select `pom.xml` -> Open as Project).
   Maven downloads the MySQL driver automatically.

## Run
- IDE: run `CafeApp.main()`
- Terminal: `mvn compile exec:java`

## Change the menu
Add or edit rows in the `menu_items` table, then restart the app. Each `category` becomes a tab.

    INSERT INTO menu_items (name, category, price) VALUES ('Mocha', 'Coffee', 4.00);

## View saved orders
    SELECT o.id AS order_no, o.created_at, m.name, oi.quantity, oi.unit_price, o.total
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.id
    JOIN menu_items m   ON m.id = oi.menu_item_id
    ORDER BY o.id DESC;
