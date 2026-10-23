package com.shopsphere.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class for establishing and managing MySQL JDBC database connections.
 * Loads configuration dynamically from 'db.properties' with hardcoded fallbacks.
 */
public class DBConnection {

    private static String url;
    private static String username;
    private static String password;
    private static String driver;

    static {
        loadProperties();
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] WARNING: MySQL JDBC Driver class not found: " + driver);
            System.err.println("[DBConnection] Ensure mysql-connector-j jar is present in the classpath.");
        }
    }

    private DBConnection() {
        // Prevent instantiation
    }

    /**
     * Loads database configuration properties from classpath or local file system.
     */
    private static void loadProperties() {
        Properties props = new Properties();
        boolean loaded = false;

        // 1. Try loading from classpath resources
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
                loaded = true;
            }
        } catch (Exception ignored) {
        }

        // 2. Try loading from working directory
        if (!loaded) {
            try (InputStream in = new FileInputStream("db.properties")) {
                props.load(in);
                loaded = true;
            } catch (Exception ignored) {
            }
        }

        // 3. Try loading from src/main/resources/db.properties
        if (!loaded) {
            try (InputStream in = new FileInputStream("src/main/resources/db.properties")) {
                props.load(in);
                loaded = true;
            } catch (Exception ignored) {
            }
        }

        // Configure values from properties with defaults specified in requirements
        driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        url = props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/shopsphere?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
        username = props.getProperty("db.username", "root");
        password = props.getProperty("db.password", "gayathri30");
    }

    /**
     * Obtains a new active connection to the MySQL database.
     *
     * @return java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Failed to connect to MySQL database.");
            System.err.println("  URL: " + url);
            System.err.println("  Username: " + username);
            System.err.println("  Error Code: " + e.getErrorCode() + " | SQLState: " + e.getSQLState());
            System.err.println("  Message: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Validates if the database connection can be established successfully.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Helper to quietly close resources without throwing checked exceptions.
     *
     * @param closeables One or more AutoCloseable instances (Connection, Statement, ResultSet)
     */
    public static void closeQuietly(AutoCloseable... closeables) {
        if (closeables == null) return;
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    // Getters for configuration inspection
    public static String getUrl() {
        return url;
    }

    public static String getUsername() {
        return username;
    }

    public static String getDriver() {
        return driver;
    }
}
