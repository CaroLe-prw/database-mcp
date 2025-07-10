package com.carole.database.mcp.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.carole.database.mcp.config.DatabaseConfig;
import com.carole.database.mcp.strategy.DatabaseStrategy;

import lombok.Getter;

/**
 * DataSource factory for managing multiple database connections with caching
 * 
 * This factory manages database strategy registration, data source creation, connection validation, and caching for
 * improved performance. It supports multiple database types through the strategy pattern and maintains connection pools
 * for reuse across operations.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Factory for creating and managing cached database connections with strategy pattern support
 */
@Component
public class DataSourceFactory {

    @Getter
    private final Map<String, DatabaseStrategy> strategies = new HashMap<>();

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    @Autowired
    public DataSourceFactory(List<DatabaseStrategy> databaseStrategies) {
        // Register all database strategies
        for (DatabaseStrategy strategy : databaseStrategies) {
            strategies.put(strategy.getDatabaseType(), strategy);
        }
    }

    /**
     * Create or retrieve a cached data source based on database configuration
     * 
     * @param config Database configuration containing connection parameters
     * @return DataSource instance for database operations
     */
    public DataSource getDataSource(DatabaseConfig config) {
        String cacheKey = buildCacheKey(config);

        // First try to get from cache
        DataSource cachedDataSource = dataSourceCache.get(cacheKey);
        if (cachedDataSource != null) {
            DatabaseStrategy strategy = strategies.get(config.getDatabaseType());
            if (strategy != null && strategy.validateConnection(cachedDataSource)) {
                return cachedDataSource;
            } else {
                // Connection invalid, remove from cache
                dataSourceCache.remove(cacheKey);
            }
        }

        // Create new data source
        DatabaseStrategy strategy = strategies.get(config.getDatabaseType());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported database type: " + config.getDatabaseType());
        }

        DataSource dataSource = strategy.createDataSource(config);

        // Validate connection
        if (!strategy.validateConnection(dataSource)) {
            throw new RuntimeException("Database connection validation failed");
        }

        // Cache the data source
        dataSourceCache.put(cacheKey, dataSource);

        return dataSource;
    }

    /**
     * Build cache key for data source identification
     * 
     * @param config Database configuration
     * @return Cache key string for data source lookup
     */
    private String buildCacheKey(DatabaseConfig config) {
        return String.format("%s_%s_%s_%s_%s", config.getDatabaseType(), config.getHost(), config.getPort(),
            config.getUsername(), config.getDatabase());
    }
}