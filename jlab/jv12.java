package jlab;
import java.sql.*;
public class jv12 {
    public static void main(String[] args) {

        try {
            // Load JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish Connection
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/studentdb",
                    "root",
                    "maddy@1981");

            Statement stmt = con.createStatement();

            // Insert Record
            String insertQuery = 
                    "INSERT INTO student VALUES(101,'Merry','CSE')";

            stmt.executeUpdate(insertQuery);

            System.out.println("Record Inserted Successfully.");

            // Retrieve Records
            ResultSet rs = stmt.executeQuery("SELECT * FROM student");

            System.out.println("\nStudent Details");

            while(rs.next()) {
                System.out.println(
                        rs.getInt("id") + " "
                        + rs.getString("name") + " "
                        + rs.getString("department"));
            }

            con.close();

        }
        catch(Exception e) {
            System.out.println(e);
        }
    }
}

