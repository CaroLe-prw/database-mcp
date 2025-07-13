package com.carole.database.mcp.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.carole.database.mcp.util.JsonParseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Data source configuration parser for multi-datasource support
 * 
 * Handles parsing of multiple data source configurations from various sources including environment variables, JSON
 * strings, and configuration files. Supports both predefined datasources and dynamic datasource creation.
 * 
 * @author CaroLe
 * @Date 2025/7/13
 * @Description Parser for multi-datasource configuration with JSON and environment variable support
 */
@Slf4j
@Component
public class DataSourceConfigParser {

    private final ObjectMapper objectMapper;

    public DataSourceConfigParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parse multiple data source configurations from JSON string
     * 
     * Expected JSON format: { "dataSources": [ { "name": "production", "description": "Production database", "host":
     * "prod.db.com", "port": "3306", "username": "prod_user", "password": "prod_pass", "database": "prod_db",
     * "databaseType": "mysql" }, { "name": "testing", "host": "test.db.com", "port": "3306", "username": "test_user",
     * "password": "test_pass", "database": "test_db", "databaseType": "mysql" } ] }
     * 
     * @param jsonConfig JSON configuration string
     * @return Map of data source name to DatabaseConfig
     */
    public Map<String, DatabaseConfig> parseMultiDataSourceConfig(String jsonConfig) {
        Map<String, DatabaseConfig> configMap = new HashMap<>();

        if (jsonConfig == null || jsonConfig.trim().isEmpty()) {
            log.debug("No multi-datasource configuration provided");
            return configMap;
        }

        try {
            Map<String, Object> configRoot =
                objectMapper.readValue(jsonConfig, new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataSourcesList = (List<Map<String, Object>>)configRoot.get("dataSources");

            if (dataSourcesList != null) {
                for (Map<String, Object> dsConfig : dataSourcesList) {
                    try {
                        DatabaseConfig config = objectMapper.convertValue(dsConfig, DatabaseConfig.class);

                        if (config.isValid()) {
                            String name = config.getDataSourceName();
                            if (name == null || name.trim().isEmpty()) {
                                name = "datasource_" + configMap.size();
                                config.setDataSourceName(name);
                            }
                            configMap.put(name, config);
                            log.debug("Loaded datasource configuration: {}", config.getDisplayName());
                        } else {
                            log.warn("Invalid datasource configuration found, skipping: {}", dsConfig);
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse individual datasource configuration: {}", dsConfig, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse multi-datasource configuration from JSON: {}", jsonConfig, e);
        }

        return configMap;
    }

    /**
     * Parse single data source configuration from JSON string
     * 
     * Expected JSON format: { "host": "localhost", "port": "3306", "username": "user", "password": "pass", "database":
     * "test_db", "databaseType": "mysql" }
     * 
     * @param jsonConfig JSON configuration string
     * @return DatabaseConfig instance or null if parsing fails
     */
    public DatabaseConfig parseSingleDataSourceConfig(String jsonConfig) {
        if (jsonConfig == null || jsonConfig.trim().isEmpty()) {
            return null;
        }

        try {
            DatabaseConfig config = objectMapper.readValue(jsonConfig, DatabaseConfig.class);

            if (config.isValid()) {
                log.debug("Parsed dynamic datasource configuration: {}", config.getDisplayName());
                return config;
            } else {
                log.warn("Invalid dynamic datasource configuration: {}", jsonConfig);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to parse single datasource configuration from JSON: {}", jsonConfig, e);
            return null;
        }
    }

    /**
     * Detect if a string is JSON format for dynamic datasource configuration
     * 
     * @param input Input string to check
     * @return true if the string appears to be JSON
     */
    public boolean isJsonFormat(String input) {
        return JsonParseUtils.isJsonFormat(input);
    }

    /**
     * Validate datasource configuration for security and completeness
     * 
     * @param config DatabaseConfig to validate
     * @return true if configuration is valid and secure
     */
    public boolean validateDataSourceConfig(DatabaseConfig config) {
        if (config == null || !config.isValid()) {
            return false;
        }

        // Security validation
        if (containsSqlInjectionPatterns(config.getDatabase()) || containsSqlInjectionPatterns(config.getUsername())
            || containsSqlInjectionPatterns(config.getHost())) {
            log.warn("Potential SQL injection patterns detected in datasource configuration");
            return false;
        }

        // Reasonable limits validation
        if (config.getMaxActive() > 100 || config.getMaxActive() < 1) {
            log.warn("Invalid maxActive value: {}", config.getMaxActive());
            return false;
        }

        if (config.getMaxWait() > 300000 || config.getMaxWait() < 1000) {
            log.warn("Invalid maxWait value: {}", config.getMaxWait());
            return false;
        }

        return true;
    }

    private boolean containsSqlInjectionPatterns(String input) {
        if (input == null) {
            return false;
        }

        String upperInput = input.toUpperCase();
        String[] patterns = {"SELECT ", "INSERT ", "UPDATE ", "DELETE ", "DROP ", "CREATE ", "ALTER ", "EXEC ",
            "UNION ", "SCRIPT", "--", "/*", "*/", ";", "'"};

        for (String pattern : patterns) {
            if (upperInput.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}