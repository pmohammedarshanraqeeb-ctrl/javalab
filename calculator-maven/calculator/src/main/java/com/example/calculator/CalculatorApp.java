package com.example.calculator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;

/**
 * Simple Swing calculator (+, -, *, /) that saves every calculation
 * to a MySQL table and shows the saved history in a table.
 */
public class CalculatorApp extends JFrame {

    // ===== Database settings: change USER and PASSWORD to match your MySQL setup =====
    private static final String DB_URL  =
            "jdbc:mysql://localhost:3306/calculator_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "maddy@1981AR";

    // ===== GUI components =====
    private final JTextField operand1Field = new JTextField(12);
    private final JTextField operand2Field = new JTextField(12);
    private final JTextField resultField   = new JTextField(12);

    // Table model for the history grid (cells are read-only)
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"Operand 1", "Operator", "Operand 2", "Result", "Time"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public CalculatorApp() {
        super("Calculator with History");
        buildGui();
        loadHistory(); // show previously saved calculations on startup
    }

    // ---------------------------------------------------------------
    // GUI construction
    // ---------------------------------------------------------------
    private void buildGui() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input fields: two operands and a read-only result
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.add(new JLabel("Operand 1:"));
        inputPanel.add(operand1Field);
        inputPanel.add(new JLabel("Operand 2:"));
        inputPanel.add(operand2Field);
        inputPanel.add(new JLabel("Result:"));
        resultField.setEditable(false);
        inputPanel.add(resultField);

        // One button per operator; each button's listener calls calculate() with its symbol
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        for (String op : new String[]{"+", "-", "*", "/"}) {
            JButton button = new JButton(op);
            button.addActionListener(e -> calculate(op));
            buttonPanel.add(button);
        }

        JPanel topPanel = new JPanel(new BorderLayout(5, 10));
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // History table inside a scroll pane
        JScrollPane historyScroll = new JScrollPane(new JTable(historyModel));
        historyScroll.setBorder(BorderFactory.createTitledBorder("History"));
        historyScroll.setPreferredSize(new Dimension(560, 220));

        add(topPanel, BorderLayout.NORTH);
        add(historyScroll, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null); // center on screen
    }

    // ---------------------------------------------------------------
    // Calculation logic (runs when an operator button is clicked)
    // ---------------------------------------------------------------
    private void calculate(String op) {
        // 1. Read and validate the two operands
        BigDecimal a, b;
        try {
            a = new BigDecimal(operand1Field.getText().trim());
            b = new BigDecimal(operand2Field.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Please enter valid numbers in both fields.");
            return;
        }

        // 2. Perform the chosen operation
        BigDecimal result;
        switch (op) {
            case "+": result = a.add(b);      break;
            case "-": result = a.subtract(b); break;
            case "*": result = a.multiply(b); break;
            case "/":
                if (b.compareTo(BigDecimal.ZERO) == 0) {
                    showError("Cannot divide by zero.");
                    return;
                }
                result = a.divide(b, 10, RoundingMode.HALF_UP);
                break;
            default:
                return;
        }

        // 3. Round to 10 decimals (matches the DB column) and show the result
        result = result.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
        resultField.setText(result.toPlainString());

        // 4. Save to MySQL, then refresh the history table
        try {
            saveToDatabase(a, op, b, result);
            loadHistory();
        } catch (SQLException ex) {
            showError("Could not save to the database:\n" + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // JDBC: insert one calculation into the history table
    // ---------------------------------------------------------------
    private void saveToDatabase(BigDecimal a, String op, BigDecimal b, BigDecimal result)
            throws SQLException {
        String sql = "INSERT INTO calculation_history "
                   + "(operand1, operator_symbol, operand2, result_value) VALUES (?, ?, ?, ?)";

        // try-with-resources closes the connection and statement automatically
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, a);
            ps.setString(2, op);
            ps.setBigDecimal(3, b);
            ps.setBigDecimal(4, result);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // JDBC: read all saved calculations (newest first) into the table
    // ---------------------------------------------------------------
    private void loadHistory() {
        String sql = "SELECT operand1, operator_symbol, operand2, result_value, created_at "
                   + "FROM calculation_history ORDER BY id DESC";

        historyModel.setRowCount(0); // clear old rows

        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                historyModel.addRow(new Object[]{
                        clean(rs.getBigDecimal("operand1")),
                        rs.getString("operator_symbol"),
                        clean(rs.getBigDecimal("operand2")),
                        clean(rs.getBigDecimal("result_value")),
                        rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException ex) {
            showError("Could not load history from the database:\n" + ex.getMessage());
        }
    }

    // Formats a number without trailing zeros (e.g. 5.0000000000 -> 5)
    private String clean(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---------------------------------------------------------------
    // Entry point: build the GUI on the Swing event thread
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculatorApp().setVisible(true));
    }
}
