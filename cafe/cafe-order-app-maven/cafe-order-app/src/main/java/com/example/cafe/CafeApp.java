package com.example.cafe;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cafe ordering app: the menu is loaded from MySQL and shown as buttons.
 * Click items to build an order, then "Place Order" saves it to the database.
 */
public class CafeApp extends JFrame {

    // ===== Settings: change DB_USER and DB_PASS to match your MySQL setup =====
    private static final String DB_URL  =
            "jdbc:mysql://localhost:3306/cafe_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "maddy@1981AR";
    private static final String CURRENCY = "$"; // shown before every price

    /** One menu item, loaded from the menu_items table. */
    private static class Product {
        final int id;
        final String name;
        final BigDecimal price;

        Product(int id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }

    // The current order: product -> quantity (LinkedHashMap keeps the order items were added)
    private final Map<Product, Integer> order = new LinkedHashMap<>();

    // GUI components
    private final JTabbedPane menuTabs = new JTabbedPane();
    private final DefaultTableModel orderModel = new DefaultTableModel(
            new String[]{"Item", "Qty", "Price", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable orderTable = new JTable(orderModel);
    private final JLabel totalLabel = new JLabel();

    public CafeApp() {
        super("Cafe Order System");
        buildGui();
        loadMenu();
    }

    // ---------------------------------------------------------------
    // GUI construction
    // ---------------------------------------------------------------
    private void buildGui() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left side: menu tabs (filled in by loadMenu)
        menuTabs.setPreferredSize(new Dimension(460, 380));

        // Right side: the current order
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 18f));

        JButton removeButton = new JButton("Remove One");
        JButton clearButton  = new JButton("Clear");
        JButton placeButton  = new JButton("Place Order");
        removeButton.addActionListener(e -> removeOne());
        clearButton.addActionListener(e -> {
            order.clear();
            refreshOrder();
        });
        placeButton.addActionListener(e -> placeOrder());

        JPanel smallButtons = new JPanel(new GridLayout(1, 2, 5, 5));
        smallButtons.add(removeButton);
        smallButtons.add(clearButton);

        JPanel bottom = new JPanel(new GridLayout(3, 1, 5, 5));
        bottom.add(totalLabel);
        bottom.add(smallButtons);
        bottom.add(placeButton);

        JPanel orderPanel = new JPanel(new BorderLayout(5, 5));
        orderPanel.setBorder(BorderFactory.createTitledBorder("Current Order"));
        orderPanel.setPreferredSize(new Dimension(340, 380));
        orderPanel.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        orderPanel.add(bottom, BorderLayout.SOUTH);

        add(menuTabs, BorderLayout.CENTER);
        add(orderPanel, BorderLayout.EAST);

        refreshOrder();
        pack();
        setLocationRelativeTo(null); // center on screen
    }

    // ---------------------------------------------------------------
    // JDBC: read the menu and create one button per item
    // ---------------------------------------------------------------
    private void loadMenu() {
        String sql = "SELECT id, name, category, price FROM menu_items ORDER BY id";
        Map<String, JPanel> categoryGrids = new LinkedHashMap<>(); // category -> its button grid

        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("id"), rs.getString("name"), rs.getBigDecimal("price"));
                String category = rs.getString("category");

                // First time we see a category: create a new tab for it
                JPanel grid = categoryGrids.get(category);
                if (grid == null) {
                    grid = new JPanel(new GridLayout(0, 3, 8, 8));
                    categoryGrids.put(category, grid);

                    JPanel wrapper = new JPanel(new BorderLayout());
                    wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                    wrapper.add(grid, BorderLayout.NORTH);
                    menuTabs.addTab(category, wrapper);
                }

                // One button per menu item; clicking it adds the item to the order
                JButton button = new JButton("<html><center>" + product.name
                        + "<br>" + money(product.price) + "</center></html>");
                button.setPreferredSize(new Dimension(130, 70));
                button.addActionListener(e -> addToOrder(product));
                grid.add(button);
            }
        } catch (SQLException ex) {
            showError("Could not load the menu from the database:\n" + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Order handling (in memory until "Place Order" is clicked)
    // ---------------------------------------------------------------
    private void addToOrder(Product product) {
        order.merge(product, 1, Integer::sum); // add 1 to the quantity (or start at 1)
        refreshOrder();
    }

    private void removeOne() {
        int row = orderTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an item in the order first.");
            return;
        }
        // Table rows are in the same order as the map entries
        Product product = new ArrayList<>(order.keySet()).get(row);
        int quantity = order.get(product) - 1;
        if (quantity <= 0) {
            order.remove(product);
        } else {
            order.put(product, quantity);
        }
        refreshOrder();
    }

    // Redraws the order table and the total
    private void refreshOrder() {
        orderModel.setRowCount(0);
        for (Map.Entry<Product, Integer> line : order.entrySet()) {
            BigDecimal subtotal = line.getKey().price.multiply(BigDecimal.valueOf(line.getValue()));
            orderModel.addRow(new Object[]{
                    line.getKey().name, line.getValue(), money(line.getKey().price), money(subtotal)});
        }
        totalLabel.setText("Total: " + money(orderTotal()));
    }

    private BigDecimal orderTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Product, Integer> line : order.entrySet()) {
            total = total.add(line.getKey().price.multiply(BigDecimal.valueOf(line.getValue())));
        }
        return total;
    }

    // ---------------------------------------------------------------
    // JDBC: save the order (one row in orders + one row per item line)
    // ---------------------------------------------------------------
    private void placeOrder() {
        if (order.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The order is empty. Click menu items to add them.");
            return;
        }

        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            con.setAutoCommit(false); // save the order and its items together, or not at all
            try {
                int orderId = insertOrder(con);
                insertOrderItems(con, orderId);
                con.commit();

                JOptionPane.showMessageDialog(this,
                        "Order #" + orderId + " placed!\nTotal: " + money(orderTotal()),
                        "Order placed", JOptionPane.INFORMATION_MESSAGE);
                order.clear();
                refreshOrder();
            } catch (SQLException ex) {
                con.rollback(); // undo a half-saved order
                throw ex;
            }
        } catch (SQLException ex) {
            showError("Could not save the order:\n" + ex.getMessage());
        }
    }

    // Inserts the order header and returns the new order id
    private int insertOrder(Connection con) throws SQLException {
        String sql = "INSERT INTO orders (total) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, orderTotal());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // Inserts one row per item line of the current order
    private void insertOrderItems(Connection con, int orderId) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Map.Entry<Product, Integer> line : order.entrySet()) {
                ps.setInt(1, orderId);
                ps.setInt(2, line.getKey().id);
                ps.setInt(3, line.getValue());
                ps.setBigDecimal(4, line.getKey().price);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private String money(BigDecimal amount) {
        return CURRENCY + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CafeApp().setVisible(true));
    }
}
