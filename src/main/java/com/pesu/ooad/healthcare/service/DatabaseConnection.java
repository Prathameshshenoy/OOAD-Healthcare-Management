package com.pesu.ooad.healthcare.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection — PURE GoF Singleton (no Spring annotations).
 *
 * UML Class Diagram requirements:
 *   - private static instance
 *   - private constructor
 *   - public static synchronized getInstance()
 *   - connect()
 *   - disconnect()
 */
public class DatabaseConnection {

    // ------------------------------------------------------------------ //
    //  GoF Singleton — private static instance
    // ------------------------------------------------------------------ //
    private static DatabaseConnection instance;

    // JDBC coordinates (match application.properties)
    private static final String URL      = "jdbc:h2:file:./data/healthcare_db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";

    private Connection connection;

    // ------------------------------------------------------------------ //
    //  Private constructor — prevents external instantiation
    // ------------------------------------------------------------------ //
    private DatabaseConnection() {
        // intentionally empty
    }

    // ------------------------------------------------------------------ //
    //  getInstance() — thread-safe, lazy initialisation
    // ------------------------------------------------------------------ //
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // ------------------------------------------------------------------ //
    //  connect() — open JDBC connection
    // ------------------------------------------------------------------ //
    public void connect() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("[DatabaseConnection] Connected to: " + URL);
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] connect() failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  disconnect() — close JDBC connection
    // ------------------------------------------------------------------ //
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DatabaseConnection] Disconnected.");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] disconnect() failed: " + e.getMessage());
        }
    }

    /** Returns true when a JDBC connection is currently open. */
    public boolean isConnected() {
        try { return connection != null && !connection.isClosed(); }
        catch (SQLException e) { return false; }
    }
}
