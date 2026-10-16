-- Creates the database and the table used by CalculatorApp.java
CREATE DATABASE IF NOT EXISTS calculator_db;
USE calculator_db;

CREATE TABLE IF NOT EXISTS calculation_history (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    operand1        DECIMAL(30,10) NOT NULL,                  -- first number
    operator_symbol CHAR(1)        NOT NULL,                  -- one of: + - * /
    operand2        DECIMAL(30,10) NOT NULL,                  -- second number
    result_value    DECIMAL(30,10) NOT NULL,                  -- calculated answer
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
