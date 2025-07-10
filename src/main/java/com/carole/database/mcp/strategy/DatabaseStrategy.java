package com.carole.database.mcp.strategy;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.carole.database.mcp.config.DatabaseConfig;
import com.carole.database.mcp.pojo.DeleteRequest;
import com.carole.database.mcp.pojo.TableInfo;
import com.carole.database.mcp.pojo.TableMetadata;
import com.carole.database.mcp.pojo.UpdateRequest;

/**
 * Database strategy interface supporting multiple database types using strategy pattern
 * 
 * This interface defines the contract for database operations across different database types. Implementations provide
 * database-specific logic for connection management, metadata retrieval, and data manipulation operations while
 * maintaining a consistent API.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Database strategy interface supporting multiple database types using strategy pattern
 */
public interface DatabaseStrategy {

    /**
     * Creates a data source instance with the specified database configuration
     * 
     * @param config Database configuration information including connection details and pool settings
     * @return Configured data source instance ready for database operations
     */
    DataSource createDataSource(DatabaseConfig config);

    /**
     * Gets the database type identifier supported by this strategy
     * 
     * @return Database type identifier (e.g., "mysql", "postgresql", "oracle")
     */
    String getDatabaseType();

    /**
     * Validates the connection to the database using the provided data source
     * 
     * @param dataSource The data source to validate
     * @return true if connection is valid and accessible, false otherwise
     */
    boolean validateConnection(DataSource dataSource);

    /**
     * Lists all tables in the database with their metadata information
     * 
     * @param dataSource The data source to query
     * @return Formatted table information as a string, or error message if operation fails
     */
    List<TableMetadata> listTables(DataSource dataSource);

    /**
     * Describes the structure of a specific table including columns, data types, and constraints
     * 
     * @param dataSource The data source to query
     * @param tableName The name of the table to describe
     * @return List of TableInfo objects containing detailed column information
     */
    List<TableInfo> describeTable(DataSource dataSource, String tableName);

    /**
     * Gets the complete definition information for a specific column (including all possible values for ENUM/SET types)
     * 
     * @param dataSource The data source to query
     * @param tableName The name of the table containing the column
     * @param columnName The name of the column to get definition for
     * @return Complete column definition information including all possible values for ENUM/SET types
     */
    String getColumnDefinition(DataSource dataSource, String tableName, String columnName);

    /**
     * Performs batch insertion of data records into the specified table
     * 
     * @param dataSource The data source to use for insertion
     * @param tableName The name of the target table
     * @param dataList List of data records to insert, where each record is a map of column names to values
     * @return Result information about the insertion operation including success status and affected rows
     */
    String batchInsert(DataSource dataSource, String tableName, List<Map<String, Object>> dataList);

    /**
     * Executes a SELECT query and returns formatted results
     * 
     * @param dataSource The data source to use for query execution
     * @param sqlQuery The SELECT query to execute (should already be validated and limited)
     * @return Formatted query results as a string, or error message if execution fails
     */
    String executeSelectQuery(DataSource dataSource, String sqlQuery);

    /**
     * Performs batch update operations using structured update request
     * 
     * This method executes multiple update rules within a single transaction (if requested), providing structured and
     * safe database updates with comprehensive validation.
     * 
     * @param dataSource The data source to use for update operations
     * @param updateRequest Complete update request containing table name, rules, and configuration
     * @return Result information about the update operations including affected rows and details
     */
    String batchUpdateData(DataSource dataSource, UpdateRequest updateRequest);

    /**
     * Performs batch delete operations using structured delete request
     * 
     * This method executes multiple delete rules within a single transaction (if requested), providing structured and
     * safe database deletions with comprehensive validation and safety limits.
     * 
     * @param dataSource The data source to use for delete operations
     * @param deleteRequest Complete delete request containing table name, rules, and configuration
     * @return Result information about the delete operations including affected rows and details
     */
    String batchDeleteData(DataSource dataSource, DeleteRequest deleteRequest);
}