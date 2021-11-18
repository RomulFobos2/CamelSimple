package org.example;

import bitronix.tm.resource.jdbc.PoolingDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ManagerDB {
    PoolingDataSource poolingDataSource;
    private String url;
    private String username;
    private String password;
    private static Connection currentConnection;

    private ManagerDB(PoolingDataSource poolingDataSource, String url, String username, String password) throws Exception {
        this.poolingDataSource = poolingDataSource;
        this.url = url;
        this.username = username;
        this.password = password;
        this.currentConnection = poolingDataSource.getConnection(username, password);
    }

    public PoolingDataSource getPoolingDataSource() {
        return poolingDataSource;
    }

    public void setPoolingDataSource(PoolingDataSource poolingDataSource) {
        this.poolingDataSource = poolingDataSource;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static Connection getCurrentConnection() {
        return currentConnection;
    }

    public static void setCurrentConnection(Connection currentConnection) {
        ManagerDB.currentConnection = currentConnection;
    }

    public static boolean tableExist(String tableName) throws SQLException {
        boolean tExists = false;
        try (ResultSet rs = currentConnection.getMetaData().getTables(null, null, tableName, null)) {
            while (rs.next()) {
                String tName = rs.getString("TABLE_NAME");
                if (tName != null && tName.equals(tableName)) {
                    tExists = true;
                    break;
                }
            }
        }
        return tExists;
    }
}
