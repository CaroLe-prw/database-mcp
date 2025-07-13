package com.carole.database.mcp.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.carole.database.mcp.config.DataSourceConfigParser;
import com.carole.database.mcp.config.DatabaseConfig;
import com.carole.database.mcp.factory.DataSourceFactory;
import com.carole.database.mcp.strategy.DatabaseStrategy;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralized data source manager for multi-datasource support
 * 
 * This manager provides unified access to multiple data sources, supporting both predefined datasources (configured at
 * startup) and dynamic datasources (created on-demand from JSON configuration). Integrates with existing
 * DataSourceFactory for caching and connection management.
 * 
 * @author CaroLe
 * @Date 2025/7/13
 * @Description Unified manager for multiple database connections with dynamic and predefined datasource support
 */
@Slf4j
@Component
public class DataSourceManager {

    @Resource
    private DataSourceFactory dataSourceFactory;

    @Resource
    private DataSourceConfigParser configParser;

    @Resource
    private DatabaseConfig defaultDatabaseConfig;

    @Resource
    private Map<String, DatabaseConfig> predefinedDataSources;

    // Cache for dynamically created datasource configurations
    private final Map<String, DatabaseConfig> dynamicDataSources = new ConcurrentHashMap<>();

    /**
     * Get data source based on input parameter
     * 
     * This method supports three modes: 1. null/empty -> returns default datasource 2. predefined name -> returns
     * predefined datasource by name 3. JSON string -> parses and creates dynamic datasource
     * 
     * @param dataSourceIdentifier null, predefined name, or JSON configuration
     * @return DataSource instance
     * @throws IllegalArgumentException if datasource cannot be created or found
     */
    public DataSource getDataSource(String dataSourceIdentifier) {
        try {
            DatabaseConfig config = resolveDataSourceConfig(dataSourceIdentifier);
            if (config == null) {
                throw new IllegalArgumentException(
                    "Cannot resolve datasource configuration for: " + dataSourceIdentifier);
            }
            return dataSourceFactory.getDataSource(config);
        } catch (Exception e) {
            log.error("Failed to get datasource for identifier: {}", dataSourceIdentifier, e);
            throw new IllegalArgumentException("Failed to get datasource: " + e.getMessage(), e);
        }
    }

    /**
     * Get database configuration based on input parameter
     * 
     * @param dataSourceIdentifier null, predefined name, or JSON configuration
     * @return DatabaseConfig instance
     * @throws IllegalArgumentException if configuration cannot be resolved
     */
    public DatabaseConfig getDataSourceConfig(String dataSourceIdentifier) {
        DatabaseConfig config = resolveDataSourceConfig(dataSourceIdentifier);
        if (config == null) {
            throw new IllegalArgumentException("Cannot resolve datasource configuration for: " + dataSourceIdentifier);
        }
        return config;
    }

    /**
     * Resolve data source configuration from various input formats
     * 
     * @param dataSourceIdentifier Input identifier (null, name, or JSON)
     * @return DatabaseConfig instance or null if not found
     */
    private DatabaseConfig resolveDataSourceConfig(String dataSourceIdentifier) {
        // Case 1: null or empty -> use default datasource
        if (dataSourceIdentifier == null || dataSourceIdentifier.trim().isEmpty()) {
            log.debug("Using default datasource");
            return defaultDatabaseConfig;
        }

        String identifier = dataSourceIdentifier.trim();

        // Case 2: Check if it's a predefined datasource name
        if (predefinedDataSources.containsKey(identifier)) {
            log.debug("Using predefined datasource: {}", identifier);
            return predefinedDataSources.get(identifier);
        }

        // Case 3: Check if it's JSON format for dynamic datasource
        if (configParser.isJsonFormat(identifier)) {
            return resolveDynamicDataSource(identifier);
        }

        // Case 4: Check if it's a short name that might match predefined datasource (case-insensitive)
        for (Map.Entry<String, DatabaseConfig> entry : predefinedDataSources.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(identifier)) {
                log.debug("Using predefined datasource (case-insensitive match): {}", entry.getKey());
                return entry.getValue();
            }
        }

        log.warn("No datasource found for identifier: {}", identifier);
        return null;
    }

    /**
     * Resolve dynamic data source from JSON configuration
     * 
     * @param jsonConfig JSON configuration string
     * @return DatabaseConfig instance or null if parsing fails
     */
    private DatabaseConfig resolveDynamicDataSource(String jsonConfig) {
        try {
            // Check cache first
            String cacheKey = "dynamic_" + jsonConfig.hashCode();
            DatabaseConfig cachedConfig = dynamicDataSources.get(cacheKey);
            if (cachedConfig != null) {
                log.debug("Using cached dynamic datasource configuration");
                return cachedConfig;
            }

            // Parse new dynamic configuration
            DatabaseConfig dynamicConfig = configParser.parseSingleDataSourceConfig(jsonConfig);
            if (dynamicConfig != null && configParser.validateDataSourceConfig(dynamicConfig)) {
                // Generate name for dynamic datasource if not provided
                if (dynamicConfig.getDataSourceName() == null || dynamicConfig.getDataSourceName().trim().isEmpty()) {
                    dynamicConfig.setDataSourceName("dynamic_" + System.currentTimeMillis());
                }

                // Cache the configuration
                dynamicDataSources.put(cacheKey, dynamicConfig);
                log.debug("Created and cached dynamic datasource: {}", dynamicConfig.getDisplayName());
                return dynamicConfig;
            } else {
                log.error("Invalid dynamic datasource configuration: {}", jsonConfig);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to resolve dynamic datasource from JSON: {}", jsonConfig, e);
            return null;
        }
    }

    /**
     * Get datasource information for debugging and logging
     * 
     * @param dataSourceIdentifier Datasource identifier
     * @return Human-readable datasource information
     */
    public String getDataSourceInfo(String dataSourceIdentifier) {
        try {
            DatabaseConfig config = resolveDataSourceConfig(dataSourceIdentifier);
            if (config != null) {
                return String.format("DataSource: %s (%s)", config.getDisplayName(),
                    config.getDataSourceName() != null ? config.getDataSourceName() : "unnamed");
            } else {
                return "DataSource: Not found";
            }
        } catch (Exception e) {
            return "DataSource: Error - " + e.getMessage();
        }
    }

    /**
     * Functional interface for database operations
     */
    @FunctionalInterface
    public interface DatabaseOperation {
        String execute(DataSource dataSource, DatabaseStrategy strategy) throws Exception;
    }

    /**
     * Execute operation with specified datasource configuration
     * 
     * This method provides unified access to database operations across multiple datasources. It handles datasource
     * resolution, strategy selection, and error handling consistently.
     * 
     * @param dataSourceConfig Datasource configuration (null, name, or JSON)
     * @param operation Database operation to execute
     * @param operationName Operation name for error reporting
     * @return Operation result
     */
    public String executeWithDataSource(String dataSourceConfig, DatabaseOperation operation, String operationName) {
        try {
            // Get data source using DataSourceManager
            DataSource dataSource = getDataSource(dataSourceConfig);
            DatabaseConfig config = getDataSourceConfig(dataSourceConfig);

            // Get the corresponding database strategy
            String databaseType = config.getDatabaseType();
            for (DatabaseStrategy strategy : dataSourceFactory.getStrategies().values()) {
                if (strategy.getDatabaseType().equals(databaseType)) {
                    String result = operation.execute(dataSource, strategy);

                    // Add datasource info to result for multi-datasource operations
                    if (dataSourceConfig != null && !dataSourceConfig.trim().isEmpty()) {
                        return result + "\n\n[DataSource: " + config.getDisplayName() + "]";
                    }
                    return result;
                }
            }
            return "Unsupported database type: " + databaseType;
        } catch (Exception e) {
            String errorMsg = "Failed to " + operationName + ": " + e.getMessage();
            if (dataSourceConfig != null && !dataSourceConfig.trim().isEmpty()) {
                errorMsg += " [DataSource: " + getDataSourceInfo(dataSourceConfig) + "]";
            }
            return errorMsg;
        }
    }
}