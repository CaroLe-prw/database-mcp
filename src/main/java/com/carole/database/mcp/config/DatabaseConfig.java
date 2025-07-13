package com.carole.database.mcp.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Database configuration class containing connection parameters and pool settings
 * 
 * Enhanced to support multiple data sources configuration. Can be used as individual data source configuration or as
 * part of a multi-datasource setup. Supports both predefined datasources and dynamic datasource creation.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Configuration holder for database connection parameters and Druid pool settings with multi-datasource
 *              support
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseConfig {

    // Data source identification
    @JsonProperty("name")
    private String dataSourceName;

    @JsonProperty("description")
    private String description;

    // Basic database connection configuration
    @JsonProperty("host")
    private String host;

    @JsonProperty("port")
    private String port;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("database")
    private String database;

    @JsonProperty("databaseType")
    private String databaseType;

    // Druid connection pool core configuration
    @JsonProperty("initialSize")
    private int initialSize = 5;

    @JsonProperty("minIdle")
    private int minIdle = 5;

    @JsonProperty("maxActive")
    private int maxActive = 20;

    @JsonProperty("maxWait")
    private long maxWait = 60000;

    // Constructor for basic database configuration (backward compatibility)
    public DatabaseConfig(String host, String port, String username, String password, String database,
        String databaseType) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.databaseType = databaseType;
    }

    // Constructor with data source name (for predefined datasources)
    public DatabaseConfig(String dataSourceName, String host, String port, String username, String password,
        String database, String databaseType) {
        this.dataSourceName = dataSourceName;
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

    // Full constructor with all configuration options
    public DatabaseConfig(String dataSourceName, String description, String host, String port, String username,
        String password, String database, String databaseType, int initialSize, int minIdle, int maxActive,
        long maxWait) {
        this.dataSourceName = dataSourceName;
        this.description = description;
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

    /**
     * Check if this configuration represents a valid database connection
     * 
     * @return true if all required fields are present
     */
    public boolean isValid() {
        return host != null && !host.trim().isEmpty() && port != null && !port.trim().isEmpty() && username != null
            && !username.trim().isEmpty() && password != null && database != null && !database.trim().isEmpty()
            && databaseType != null && !databaseType.trim().isEmpty();
    }

    /**
     * Get display name for this data source Uses dataSourceName if available, otherwise generates from connection info
     * 
     * @return display name for the data source
     */
    public String getDisplayName() {
        if (dataSourceName != null && !dataSourceName.trim().isEmpty()) {
            return dataSourceName;
        }
        return String.format("%s@%s:%s/%s", username, host, port, database);
    }
}