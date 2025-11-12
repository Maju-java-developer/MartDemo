package raven.modal.demo.mysql;

import lombok.Getter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Getter
public class MySQLConnection {

    private static final String DB_URL = "jdbc:mysql://localhost:110/martDB"; // ✅ also fix your port (see below)
    private static final String USER = "root";
    private static final String PASS = "root";
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private MySQLConnection() {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found.");
            e.printStackTrace();
        }
    }

    private static final class InstanceHolder {
        static final MySQLConnection instance = new MySQLConnection();
    }

    public static MySQLConnection getInstance() {
        return InstanceHolder.instance;
    }

    // ✅ Always create a *new* connection
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            System.err.println("Could not establish new DB connection.");
            e.printStackTrace();
            return null;
        }
    }
}
