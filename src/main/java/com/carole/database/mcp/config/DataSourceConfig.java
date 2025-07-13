package com.carole.database.mcp.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced database configuration class for managing multiple data sources
 * 
 * This configuration class supports both single default datasource (backward compatibility) and multiple predefined
 * datasources. Reads configuration from environment variables and supports JSON-based multi-datasource configuration
 * for flexible deployment scenarios.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Enhanced configuration for multiple database connections with Druid connection pool settings
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Resource
    private DataSourceConfigParser configParser;

    // Default database connection configuration (backward compatibility)
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

    // Multi-datasource configuration
    @Value("${DATASOURCE_CONFIG:}")
    private String dataSourceConfig;

    /**
     * Create default database configuration bean for backward compatibility This configuration is used when no specific
     * datasource is requested
     * 
     * @return Default DatabaseConfig instance
     */
    @Bean
    public DatabaseConfig defaultDatabaseConfig() {
        DatabaseConfig config = new DatabaseConfig("default", "Default database connection", host, port, username,
            password, database, databaseType, initialSize, minIdle, maxActive, maxWait);

        log.info("Initialized default database configuration: {}", config.getDisplayName());
        return config;
    }

    /**
     * Create map of predefined database configurations Parses DATASOURCES_CONFIG environment variable for additional
     * datasources
     * 
     * @return Map of datasource name to DatabaseConfig
     */
    @Bean
    public Map<String, DatabaseConfig> predefinedDataSources() {
        Map<String, DatabaseConfig> dataSources = new HashMap<>();

        // Add default datasource to the map
        DatabaseConfig defaultConfig = defaultDatabaseConfig();
        dataSources.put("default", defaultConfig);

        // Parse additional datasources from configuration
        if (dataSourceConfig != null && !dataSourceConfig.trim().isEmpty()) {
            try {
                Map<String, DatabaseConfig> additionalSources =
                    configParser.parseMultiDataSourceConfig(dataSourceConfig);
                dataSources.putAll(additionalSources);

                log.info("Loaded {} additional predefined datasources", additionalSources.size());
                for (String name : additionalSources.keySet()) {
                    log.info("  - {}: {}", name, additionalSources.get(name).getDisplayName());
                }
            } catch (Exception e) {
                log.error("Failed to parse DATASOURCES_CONFIG, using only default datasource", e);
            }
        }
        log.info("Total configured datasources: {}", dataSources.size());
        return dataSources;
    }
}