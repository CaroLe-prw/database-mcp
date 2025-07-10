package com.carole.database.mcp.config;

import lombok.Data;

/**
 * Database configuration class containing connection parameters and pool settings
 * 
 * This class encapsulates all necessary database connection information including host, port, credentials, database
 * name, and Druid connection pool configurations. Used by database strategies to create and configure data sources.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Configuration holder for database connection parameters and Druid pool settings
 */
@Data
public class DatabaseConfig {

    // Basic database connection configuration
    private String host;
    private String port;
    private String username;
    private String password;
    private String database;
    private String databaseType;

    // Druid connection pool core configuration
    private int initialSize = 5;
    private int minIdle = 5;
    private int maxActive = 20;
    private long maxWait = 60000;

    public DatabaseConfig(String host, String port, String username, String password, String database,
        String databaseType) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.databaseType = databaseType;
    }

    // Constructor with connection pool configuration
    public DatabaseConfig(String host, String port, String username, String password, String database,
        String databaseType, int initialSize, int minIdle, int maxActive, long maxWait) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.databaseType = databaseType;
        this.initialSize = initialSize;
        this.minIdle = minIdle;
        this.maxActive = maxActive;
        this.maxWait = maxWait;
    }
}