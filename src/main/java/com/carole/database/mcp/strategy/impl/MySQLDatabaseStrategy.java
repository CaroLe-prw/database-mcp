package com.carole.database.mcp.strategy.impl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;
import com.carole.database.mcp.config.DatabaseConfig;
import com.carole.database.mcp.config.DatabaseOperationConfig;
import com.carole.database.mcp.pojo.*;
import com.carole.database.mcp.strategy.DatabaseStrategy;
import com.carole.database.mcp.util.SqlBuilder;
import com.carole.database.mcp.util.SqlSecurityValidator;
import com.carole.database.mcp.util.TableFormatter;

import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * MySQL database strategy implementation for database operations
 * 
 * This strategy provides MySQL-specific implementations for database connection creation, table listing, structure
 * description, and connection validation. It uses Druid connection pool for efficient connection management and
 * supports MySQL-specific metadata queries and formatting.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description MySQL database strategy implementation with Druid connection pool and metadata operations
 */
@Slf4j
@Component
public class MySQLDatabaseStrategy implements DatabaseStrategy {

    @Resource
    private DatabaseOperationConfig operationConfig;

    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String MYSQL_URL_TEMPLATE =
        "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true";
    private static final String DATABASE_TYPE = "mysql";

    /**
     * Creates a MySQL data source with Druid connection pool configuration
     * 
     * This method creates and configures a Druid data source instance for MySQL database connections. It applies both
     * basic connection parameters and advanced pool settings for optimal performance.
     * 
     * @param config Database configuration containing connection details and pool settings
     * @return Configured DruidDataSource instance ready for MySQL database operations
     */
    @Override
    @SneakyThrows
    public DataSource createDataSource(DatabaseConfig config) {
        DruidDataSource dataSource = new DruidDataSource();

        // Basic configuration
        dataSource.setDriverClassName(MYSQL_DRIVER);
        String url = String.format(MYSQL_URL_TEMPLATE, config.getHost(), config.getPort(), config.getDatabase());
        dataSource.setUrl(url);
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());

        // Use Druid connection pool core configuration from DatabaseConfig
        dataSource.setInitialSize(config.getInitialSize());
        dataSource.setMinIdle(config.getMinIdle());
        dataSource.setMaxActive(config.getMaxActive());
        dataSource.setMaxWait(config.getMaxWait());

        // Fixed configuration
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
        dataSource.setFilters("stat,wall");
        return dataSource;
    }

    /**
     * Gets the database type identifier for this strategy
     * 
     * Returns the database type string that identifies this strategy as a MySQL implementation. This identifier is used
     * by the factory to select the appropriate strategy.
     * 
     * @return Database type identifier "mysql"
     */
    @Override
    public String getDatabaseType() {
        return DATABASE_TYPE;
    }

    /**
     * Validates MySQL database connection using isValid() method
     * 
     * This method tests the database connection by obtaining a connection from the data source and using the JDBC
     * isValid() method with a 3-second timeout for quick validation.
     * 
     * @param dataSource The data source to validate
     * @return true if connection is valid and accessible, false otherwise
     */
    @Override
    public boolean validateConnection(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Lists all tables in the MySQL database with their metadata
     * 
     * This method retrieves table information from the MySQL database using JDBC metadata API. It provides table names
     * and remarks/comments for each table in the current database catalog.
     * 
     * @param dataSource The data source to query for table information
     * @return List of TableMetadata objects containing table names and descriptions
     */
    @Override
    public List<TableMetadata> listTables(DataSource dataSource) {
        Connection connection = null;
        try {
            // Use Spring's DataSourceUtils for proper connection management
            connection = DataSourceUtils.getConnection(dataSource);

            DatabaseMetaData metaData = connection.getMetaData();
            String databaseName = connection.getCatalog();

            try (ResultSet resultSet = metaData.getTables(databaseName, null, null, new String[] {"TABLE"})) {
                List<TableMetadata> metadataList = new ArrayList<>();
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    String remarks = resultSet.getString("REMARKS");
                    if (remarks == null || remarks.trim().isEmpty()) {
                        remarks = "No remarks";
                    }
                    metadataList.add(new TableMetadata(tableName, remarks));
                }

                return metadataList;
            }
        } catch (SQLException e) {
            return new ArrayList<>();
        } finally {
            // Use DataSourceUtils for proper connection release
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    /**
     * Describes the structure of a MySQL table including columns, data types, constraints, and indexes
     * 
     * This method provides comprehensive table structure information by querying MySQL metadata. It retrieves column
     * details, primary keys, foreign keys, indexes, and auto-increment information to provide a complete picture of the
     * table schema.
     * 
     * @param dataSource The data source to query for table structure information
     * @param tableName The name of the table to describe
     * @return List of TableInfo objects containing detailed column information and constraints
     */
    @Override
    public List<TableInfo> describeTable(DataSource dataSource, String tableName) {
        Connection connection = null;
        try {
            // Use DataSourceUtils for proper connection management
            connection = DataSourceUtils.getConnection(dataSource);

            DatabaseMetaData metaData = connection.getMetaData();
            String databaseName = connection.getCatalog();

            List<TableInfo> tableInfos = new ArrayList<>();

            // Collect column information
            try (ResultSet columnsRs = metaData.getColumns(databaseName, null, tableName, null)) {
                while (columnsRs.next()) {
                    TableInfo tableInfo = new TableInfo();
                    tableInfo.setName(columnsRs.getString("COLUMN_NAME"));
                    tableInfo.setDataType(columnsRs.getString("TYPE_NAME"));
                    tableInfo.setColumnSize(columnsRs.getInt("COLUMN_SIZE"));
                    tableInfo.setDecimalDigits(columnsRs.getInt("DECIMAL_DIGITS"));
                    tableInfo.setNullable("YES".equals(columnsRs.getString("IS_NULLABLE")));
                    tableInfo.setDefaultValue(columnsRs.getString("COLUMN_DEF"));
                    tableInfo.setColumnRemarks(columnsRs.getString("REMARKS"));
                    tableInfo.setOrdinalPosition(columnsRs.getInt("ORDINAL_POSITION"));
                    tableInfo.setAutoIncrement("YES".equals(columnsRs.getString("IS_AUTOINCREMENT")));
                    tableInfo.setGenerated("YES".equals(columnsRs.getString("IS_GENERATEDCOLUMN")));
                    tableInfos.add(tableInfo);
                }
            }

            // Collect primary key information
            try (ResultSet pkRs = metaData.getPrimaryKeys(databaseName, null, tableName)) {
                List<String> pkColumns = new ArrayList<>();
                while (pkRs.next()) {
                    pkColumns.add(pkRs.getString("COLUMN_NAME"));
                }

                // Mark primary key columns
                for (TableInfo tableInfo : tableInfos) {
                    if (pkColumns.contains(tableInfo.getName())) {
                        tableInfo.setPrimaryKey(true);
                    }
                }
            }

            // Collect index information
            try (ResultSet indexRs = metaData.getIndexInfo(databaseName, null, tableName, false, false)) {
                while (indexRs.next()) {
                    String columnName = indexRs.getString("COLUMN_NAME");
                    String indexName = indexRs.getString("INDEX_NAME");
                    boolean nonUnique = indexRs.getBoolean("NON_UNIQUE");

                    // Skip primary key index as it's already shown in KEY column
                    if ("PRIMARY".equals(indexName)) {
                        continue;
                    }

                    // Add index information to corresponding columns
                    for (TableInfo tableInfo : tableInfos) {
                        if (tableInfo.getName().equals(columnName)) {
                            String indexType = nonUnique ? "MUL" : "UNI";
                            if (!tableInfo.getIndexes().contains(indexName)) {
                                tableInfo.getIndexes().add(indexName + "(" + indexType + ")");
                            }
                        }
                    }
                }
            }

            return tableInfos;
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            // Use DataSourceUtils for proper connection release
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    /**
     * Gets the complete definition information for a specific MySQL column
     * 
     * This method retrieves comprehensive column information including data type, constraints, and special attributes
     * like ENUM/SET values. It uses MySQL's INFORMATION_SCHEMA to provide detailed column metadata including all
     * possible values for ENUM and SET types.
     * 
     * @param dataSource The data source to query for column information
     * @param tableName The name of the table containing the column
     * @param columnName The name of the column to get definition for
     * @return Complete column definition information including COLUMN_TYPE with ENUM/SET values
     */
    @Override
    public String getColumnDefinition(DataSource dataSource, String tableName, String columnName) {
        // Validate inputs for security
        String tableValidationError = SqlSecurityValidator.getTableNameValidationError(tableName);
        if (tableValidationError != null) {
            return "Invalid table name: " + tableValidationError;
        }

        String columnValidationError = SqlSecurityValidator.getColumnNameValidationError(columnName);
        if (columnValidationError != null) {
            return "Invalid column name: " + columnValidationError;
        }

        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);

            // Use INFORMATION_SCHEMA with COLUMN_TYPE to get complete ENUM/SET definitions
            String sql =
                "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, COLUMN_DEFAULT, EXTRA, COLUMN_COMMENT "
                    + "FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, connection.getCatalog());
                ps.setString(2, tableName);
                ps.setString(3, columnName);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String columnType = rs.getString("COLUMN_TYPE");
                        String nullable = rs.getString("IS_NULLABLE");
                        String key = rs.getString("COLUMN_KEY");
                        String defaultValue = rs.getString("COLUMN_DEFAULT");
                        String extra = rs.getString("EXTRA");
                        String comment = rs.getString("COLUMN_COMMENT");

                        return String.format("Column: %s, Type: %s, Null: %s, Key: %s, Default: %s, Extra: %s%s",
                            columnName, columnType, nullable, key, defaultValue != null ? defaultValue : "NULL",
                            extra != null ? extra : "",
                            comment != null && !comment.trim().isEmpty() ? ", Comment: " + comment : "");
                    } else {
                        return "Column '" + columnName + "' not found in table '" + tableName + "'";
                    }
                }
            }
        } catch (Exception e) {
            return "Failed to get column definition: " + e.getMessage();
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    /**
     * Performs batch insertion of data records into a MySQL table with transaction safety
     * 
     * This method executes batch INSERT operations using prepared statements and transactions for optimal performance
     * and data integrity. It automatically handles parameter binding, transaction management, and provides detailed
     * result reporting with affected row counts.
     * 
     * @param dataSource The data source to use for database operations
     * @param tableName The name of the target table for insertion
     * @param dataList List of data records to insert, where each record is a map of column names to values
     * @return Result information about the insertion operation including success status and affected rows
     */
    @Override
    public String batchInsert(DataSource dataSource, String tableName, List<Map<String, Object>> dataList) {
        // Validate inputs for security
        String tableValidationError = SqlSecurityValidator.getTableNameValidationError(tableName);
        if (tableValidationError != null) {
            return "Invalid table name: " + tableValidationError;
        }

        if (dataList == null || dataList.isEmpty()) {
            return "No data to insert";
        }

        // Use operation config for record count validation
        String operationValidation = operationConfig.validateInsertOperation(dataList.size());
        if (operationValidation != null) {
            return "INSERT operation denied: " + operationValidation;
        }

        Connection connection = null;
        boolean transactionStarted = false;
        try {
            connection = DataSourceUtils.getConnection(dataSource);

            // Start transaction
            boolean originalAutoCommit = connection.getAutoCommit();
            if (originalAutoCommit) {
                connection.setAutoCommit(false);
                transactionStarted = true;
            }

            // Get the first row to determine columns
            Map<String, Object> firstRow = dataList.getFirst();
            Set<String> columns = firstRow.keySet();

            // Validate all column names for security
            for (String column : columns) {
                String columnValidationError = SqlSecurityValidator.getColumnNameValidationError(column);
                if (columnValidationError != null) {
                    return "Invalid column name '" + column + "': " + columnValidationError;
                }
            }

            // Build INSERT SQL with escaped identifiers
            StringBuilder sql = new StringBuilder("INSERT INTO ");
            sql.append(SqlSecurityValidator.escapeIdentifier(tableName)).append(" (");

            // Add escaped column names
            for (String column : columns) {
                sql.append(SqlSecurityValidator.escapeIdentifier(column)).append(",");
            }
            sql.setLength(sql.length() - 1);
            sql.append(") VALUES ");

            for (int i = 0; i < dataList.size(); i++) {
                sql.append("(");
                sql.append("?,".repeat(columns.size()));
                sql.setLength(sql.length() - 1);
                sql.append("),");
            }
            sql.setLength(sql.length() - 1);

            try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                for (Map<String, Object> row : dataList) {
                    for (String column : columns) {
                        Object value = row.get(column);
                        ps.setObject(paramIndex++, value);
                    }
                }

                int affected = ps.executeUpdate();

                if (transactionStarted) {
                    connection.commit();
                }

                return String.format("Successfully inserted %d rows into table '%s'", affected, tableName);
            }
        } catch (Exception e) {
            if (transactionStarted) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    log.error("Failed to rollback transaction: {}", rollbackEx.getMessage());
                }
            }
            return "Failed to insert data: " + e.getMessage();
        } finally {
            if (connection != null) {
                if (transactionStarted) {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException ex) {
                        log.error("Failed to restore auto-commit: {}", ex.getMessage());
                    }
                }
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    /**
     * Execute SELECT query and return formatted results
     * 
     * This method executes a validated SELECT query and returns the results in a formatted table structure. Results are
     * automatically limited to prevent excessive data retrieval and formatted for easy reading.
     * 
     * @param dataSource The data source to use for query execution
     * @param sqlQuery The SELECT query to execute (already validated and limited)
     * @return Formatted query results as ASCII table, or error message if execution fails
     */
    @Override
    public String executeSelectQuery(DataSource dataSource, String sqlQuery) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = DataSourceUtils.getConnection(dataSource);
            stmt = connection.prepareStatement(sqlQuery);
            rs = stmt.executeQuery();

            // Get result set metadata
            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();

            // Build column headers
            List<String> headers = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                headers.add(metadata.getColumnLabel(i));
            }

            // Collect data rows
            List<List<String>> rows = new ArrayList<>();
            int rowCount = 0;

            while (rs.next() && rowCount < 10) {
                List<String> row = new ArrayList<>();

                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    String stringValue = value != null ? value.toString() : "NULL";

                    // Limit cell content length for display
                    if (stringValue.length() > 150) {
                        stringValue = stringValue.substring(0, 47) + "...";
                    }

                    row.add(stringValue);
                }

                rows.add(row);
                rowCount++;
            }

            return TableFormatter.formatQueryResults(headers, rows, rowCount);
        } catch (SQLException e) {
            log.error("Failed to execute query: {}, Error: {}", sqlQuery, e.getMessage());
            return "Failed to execute query: " + e.getMessage();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.warn("Failed to close ResultSet: {}", e.getMessage());
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    log.warn("Failed to close PreparedStatement: {}", e.getMessage());
                }
            }
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    /**
     * Execute batch update operations using structured update request
     * 
     * This method implements structured and safe database updates with comprehensive validation, transaction support,
     * and detailed result reporting. It supports multiple update rules with different conditions executed within a
     * single transaction.
     * 
     * @param dataSource The data source to use for update operations
     * @param updateRequest Complete update request containing table name, rules, and configuration
     * @return Result information about the update operations including affected rows and details
     */
    @Override
    public String batchUpdateData(DataSource dataSource, UpdateRequest updateRequest) {
        // Validate update request
        String validationError = updateRequest.validate();
        if (validationError != null) {
            return "Update request validation failed: " + validationError;
        }

        String tableName = updateRequest.getTableName();
        List<UpdateRule> updateRules = updateRequest.getUpdateRules();

        // Additional validation using SqlSecurityValidator for table name
        String tableValidationError = SqlSecurityValidator.getTableNameValidationError(tableName);
        if (tableValidationError != null) {
            return "Table validation failed: " + tableValidationError;
        }

        // Check operation limits using operation config
        String operationValidation =
            operationConfig.validateUpdateOperation(updateRequest.getMaxTotalAffectedRecords(), updateRules);
        if (operationValidation != null) {
            return "UPDATE operation denied: " + operationValidation;
        }

        Connection connection = null;
        boolean transactionStarted = false;
        List<Integer> affectedRowsCounts = new ArrayList<>();
        int totalAffectedRows = 0;

        try {
            connection = DataSourceUtils.getConnection(dataSource);

            // Start transaction if requested
            if (updateRequest.isUseTransaction()) {
                connection.setAutoCommit(false);
                transactionStarted = true;
            }

            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append("Batch update operation results:\n");
            resultBuilder.append("Table: ").append(tableName).append("\n");
            resultBuilder.append("Rules executed: ").append(updateRules.size()).append("\n");
            if (updateRequest.isDryRun()) {
                resultBuilder.append("DRY RUN MODE - No actual changes made\n");
            }
            resultBuilder.append("\n");

            // Execute each update rule
            for (int ruleIndex = 0; ruleIndex < updateRules.size(); ruleIndex++) {
                UpdateRule rule = updateRules.get(ruleIndex);

                // Build UPDATE SQL for this rule
                String updateSql = SqlBuilder.buildUpdateSql(tableName, rule);

                if (updateRequest.isDryRun()) {
                    // For dry run, just show what would be executed
                    resultBuilder.append("Rule ").append(ruleIndex + 1).append(" (DRY RUN):\n");
                    resultBuilder.append("SQL: ").append(updateSql).append("\n");
                    resultBuilder.append("Description: ").append(rule.getDescription()).append("\n\n");
                    affectedRowsCounts.add(0);
                    continue;
                }

                // Execute the update
                try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                    // Set parameter values
                    int paramIndex = SqlBuilder.setUpdateParameters(stmt, rule);

                    // Execute update
                    int affectedRows = stmt.executeUpdate();
                    affectedRowsCounts.add(affectedRows);
                    totalAffectedRows += affectedRows;

                    // Check safety limits
                    if (affectedRows > rule.getMaxAffectedRecords()) {
                        throw new SQLException("Rule " + (ruleIndex + 1) + " affected " + affectedRows
                            + " records, exceeding limit of " + rule.getMaxAffectedRecords());
                    }

                    resultBuilder.append("Rule ").append(ruleIndex + 1).append(":\n");
                    resultBuilder.append("SQL: ").append(updateSql).append("\n");
                    resultBuilder.append("Affected rows: ").append(affectedRows).append("\n");
                    resultBuilder.append("Description: ").append(rule.getDescription()).append("\n\n");

                } catch (SQLException e) {
                    log.error("Failed to execute update rule {}: {}", ruleIndex + 1, e.getMessage());
                    throw new SQLException("Rule " + (ruleIndex + 1) + " execution failed: " + e.getMessage(), e);
                }
            }

            // Check total affected rows limit
            if (totalAffectedRows > updateRequest.getMaxTotalAffectedRecords()) {
                throw new SQLException("Total affected rows " + totalAffectedRows + " exceeds maximum limit of "
                    + updateRequest.getMaxTotalAffectedRecords());
            }

            // Commit transaction if started
            if (transactionStarted && !updateRequest.isDryRun()) {
                connection.commit();
                resultBuilder.append("Transaction committed successfully.\n");
            }

            // Add summary
            resultBuilder.append("Summary:\n");
            resultBuilder.append("- Total affected rows: ").append(totalAffectedRows).append("\n");
            resultBuilder.append("- Rules processed: ").append(updateRules.size()).append("\n");
            resultBuilder.append("- Transaction used: ").append(updateRequest.isUseTransaction()).append("\n");

            if (updateRequest.isReturnDetails()) {
                resultBuilder.append("- Per-rule affected rows: ").append(affectedRowsCounts).append("\n");
            }

            return resultBuilder.toString();

        } catch (Exception e) {
            // Rollback transaction on error
            if (transactionStarted) {
                try {
                    connection.rollback();
                    log.info("Transaction rolled back due to error: {}", e.getMessage());
                } catch (SQLException rollbackEx) {
                    log.error("Failed to rollback transaction: {}", rollbackEx.getMessage());
                }
            }

            log.error("Batch update operation failed: {}", e.getMessage());
            return "Batch update operation failed: " + e.getMessage();

        } finally {
            // Restore auto-commit and release connection
            if (connection != null) {
                if (transactionStarted) {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException ex) {
                        log.error("Failed to restore auto-commit: {}", ex.getMessage());
                    }
                }
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    /**
     * Execute batch delete operations using structured delete request
     * 
     * This method implements structured and safe database deletions with comprehensive validation, transaction support,
     * and detailed result reporting. It supports multiple delete rules with different conditions executed within a
     * single transaction.
     * 
     * @param dataSource The data source to use for delete operations
     * @param deleteRequest Complete delete request containing table name, rules, and configuration
     * @return Result information about the delete operations including affected rows and details
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Batch delete implementation with transaction support and comprehensive validation
     */
    @Override
    public String batchDeleteData(DataSource dataSource, DeleteRequest deleteRequest) {
        // Validate delete request
        String validationError = deleteRequest.validate();
        if (validationError != null) {
            return "Delete request validation failed: " + validationError;
        }

        String tableName = deleteRequest.getTableName();
        List<DeleteRule> deleteRules = deleteRequest.getDeleteRules();

        // Additional validation using SqlSecurityValidator for table name
        String tableValidationError = SqlSecurityValidator.getTableNameValidationError(tableName);
        if (tableValidationError != null) {
            return "Table validation failed: " + tableValidationError;
        }

        // Check operation limits using operation config
        String operationValidation =
            operationConfig.validateDeleteOperation(deleteRequest.getMaxTotalAffectedRecords(), deleteRules);
        if (operationValidation != null) {
            return "DELETE operation denied: " + operationValidation;
        }

        Connection connection = null;
        boolean transactionStarted = false;
        List<Integer> affectedRowsCounts = new ArrayList<>();
        int totalAffectedRows = 0;

        try {
            connection = DataSourceUtils.getConnection(dataSource);

            // Start transaction if requested
            if (deleteRequest.isUseTransaction()) {
                connection.setAutoCommit(false);
                transactionStarted = true;
            }

            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append("Batch delete operation results:\n");
            resultBuilder.append("Table: ").append(tableName).append("\n");
            resultBuilder.append("Rules executed: ").append(deleteRules.size()).append("\n");
            if (deleteRequest.isDryRun()) {
                resultBuilder.append("DRY RUN MODE - No actual deletions made\n");
            }
            resultBuilder.append("\n");

            // Execute each delete rule
            for (int ruleIndex = 0; ruleIndex < deleteRules.size(); ruleIndex++) {
                DeleteRule rule = deleteRules.get(ruleIndex);

                // Build DELETE SQL for this rule
                String deleteSql = SqlBuilder.buildDeleteSql(tableName, rule);

                if (deleteRequest.isDryRun()) {
                    // For dry run, just show what would be executed
                    resultBuilder.append("Rule ").append(ruleIndex + 1).append(" (DRY RUN):\n");
                    resultBuilder.append("SQL: ").append(deleteSql).append("\n");
                    resultBuilder.append("Description: ").append(rule.getDescription()).append("\n\n");
                    affectedRowsCounts.add(0);
                    continue;
                }

                // Execute the delete
                try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                    // Set parameter values
                    int paramIndex = SqlBuilder.setDeleteParameters(stmt, rule);

                    // Execute delete
                    int affectedRows = stmt.executeUpdate();
                    affectedRowsCounts.add(affectedRows);
                    totalAffectedRows += affectedRows;

                    // Check safety limits
                    if (affectedRows > rule.getMaxAffectedRecords()) {
                        throw new SQLException("Rule " + (ruleIndex + 1) + " affected " + affectedRows
                            + " records, exceeding limit of " + rule.getMaxAffectedRecords());
                    }

                    resultBuilder.append("Rule ").append(ruleIndex + 1).append(":\n");
                    resultBuilder.append("SQL: ").append(deleteSql).append("\n");
                    resultBuilder.append("Affected rows: ").append(affectedRows).append("\n");
                    resultBuilder.append("Description: ").append(rule.getDescription()).append("\n\n");

                } catch (SQLException e) {
                    log.error("Failed to execute delete rule {}: {}", ruleIndex + 1, e.getMessage());
                    throw new SQLException("Rule " + (ruleIndex + 1) + " execution failed: " + e.getMessage(), e);
                }
            }

            // Check total affected rows limit
            if (totalAffectedRows > deleteRequest.getMaxTotalAffectedRecords()) {
                throw new SQLException("Total affected rows " + totalAffectedRows + " exceeds maximum limit of "
                    + deleteRequest.getMaxTotalAffectedRecords());
            }

            // Commit transaction if started
            if (transactionStarted && !deleteRequest.isDryRun()) {
                connection.commit();
                resultBuilder.append("Transaction committed successfully.\n");
            }

            // For successful operations, return a simple summary
            if (!deleteRequest.isDryRun() && totalAffectedRows > 0) {
                return String.format("Successfully deleted %d rows from table '%s'", totalAffectedRows, tableName);
            }

            // For dry run or zero affected rows, return detailed information
            resultBuilder.append("Summary:\n");
            resultBuilder.append("- Total affected rows: ").append(totalAffectedRows).append("\n");
            resultBuilder.append("- Rules processed: ").append(deleteRules.size()).append("\n");
            resultBuilder.append("- Transaction used: ").append(deleteRequest.isUseTransaction()).append("\n");

            if (deleteRequest.isReturnDetails()) {
                resultBuilder.append("- Per-rule affected rows: ").append(affectedRowsCounts).append("\n");
            }

            return resultBuilder.toString();

        } catch (Exception e) {
            // Rollback transaction on error
            if (transactionStarted) {
                try {
                    connection.rollback();
                    log.info("Transaction rolled back due to error: {}", e.getMessage());
                } catch (SQLException rollbackEx) {
                    log.error("Failed to rollback transaction: {}", rollbackEx.getMessage());
                }
            }

            log.error("Batch delete operation failed: {}", e.getMessage());
            return "Batch delete operation failed: " + e.getMessage();

        } finally {
            // Restore auto-commit and release connection
            if (connection != null) {
                if (transactionStarted) {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException ex) {
                        log.error("Failed to restore auto-commit: {}", ex.getMessage());
                    }
                }
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

}