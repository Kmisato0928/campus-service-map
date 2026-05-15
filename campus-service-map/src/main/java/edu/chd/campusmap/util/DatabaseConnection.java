package edu.chd.campusmap.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            Class.forName(ConfigLoader.get("db.driver"));
            connection = DriverManager.getConnection(
                ConfigLoader.get("db.url"),
                ConfigLoader.get("db.username"),
                ConfigLoader.get("db.password")
            );
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(
                    ConfigLoader.get("db.url"),
                    ConfigLoader.get("db.username"),
                    ConfigLoader.get("db.password")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取数据库连接失败", e);
        }
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
