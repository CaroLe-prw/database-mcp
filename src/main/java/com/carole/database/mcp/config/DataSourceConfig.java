package com.carole.database.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Database configuration class for managing connection parameters and Druid pool settings
 * 
 * This configuration class reads database connection parameters from environment variables and creates a DatabaseConfig
 * bean with Druid connection pool configurations. Supports MySQL database connections with customizable pool settings.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Configuration for database connection parameters and Druid connection pool settings
 */
@Configuration
public class DataSourceConfig {

    // Basic database connection configuration
    @Value("${DATABASE_TYPE:mysql}")
    private String databaseType;

    @Value("${HOST:192.168.31.50}")
    private String host;

    @Value("${PORT:3306}")
    private String port;

    @Value("${USER:root}")
    private String username;

    @Value("${PASSWORD:Aa040832@}")
    private String password;

    @Value("${DATABASE:mall_admin}")
    private String database;

    // Druid connection pool core configuration
    @Value("${DRUID_INITIAL_SIZE:5}")
    private int initialSize;

    @Value("${DRUID_MIN_IDLE:5}")
    private int minIdle;

    @Value("${DRUID_MAX_ACTIVE:20}")
    private int maxActive;

    @Value("${DRUID_MAX_WAIT:60000}")
    private long maxWait;

    @Bean
    public DatabaseConfig databaseConfig() {
        return new DatabaseConfig(host, port, username, password, database, databaseType, initialSize, minIdle,
            maxActive, maxWait);
    }
}