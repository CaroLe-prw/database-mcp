package com.carole.database.mcp.service;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import com.carole.database.mcp.config.DatabaseConfig;
import com.carole.database.mcp.config.DatabaseOperationConfig;
import com.carole.database.mcp.factory.DataSourceFactory;
import com.carole.database.mcp.pojo.*;
import com.carole.database.mcp.strategy.DatabaseStrategy;
import com.carole.database.mcp.util.*;

import jakarta.annotation.Resource;

/**
 * Database service providing MCP (Model Context Protocol) tools for database operations
 * 
 * This service exposes database functionality as MCP tools that can be used by AI models to perform database queries,
 * table listing, and structure inspection operations. It supports multiple database types through a strategy pattern
 * implementation.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description MCP server implementation that provides intelligent database querying tools for AI agents
 */
@Service
public class DatabaseService {

    @Resource
    private DataSourceFactory dataSourceFactory;

    @Resource
    private DatabaseConfig defaultDatabaseConfig;

    @Resource
    private DatabaseOperationConfig operationConfig;

    @Tool(description = "List all tables in the database with their metadata including table names and remarks")
    public String listTables() {
        try {
            DataSource dataSource = dataSourceFactory.getDataSource(defaultDatabaseConfig);

            // Get the corresponding database strategy
            String databaseType = defaultDatabaseConfig.getDatabaseType();
            for (DatabaseStrategy strategy : dataSourceFactory.getStrategies().values()) {
                if (strategy.getDatabaseType().equals(databaseType)) {
                    return TableFormatter.formatTableList(strategy.listTables(dataSource));
                }
            }
            return "Unsupported database type: " + databaseType;
        } catch (Exception e) {
            return "Failed to retrieve table list: " + e.getMessage();
        }
    }

    @Tool(
        description = "Describe table structure including columns, data types, constraints, indexes, and metadata information")
    public String describeTable(@ToolParam String tableName) {
        try {
            DataSource dataSource = dataSourceFactory.getDataSource(defaultDatabaseConfig);

            // Get the corresponding database strategy
            String databaseType = defaultDatabaseConfig.getDatabaseType();
            for (DatabaseStrategy strategy : dataSourceFactory.getStrategies().values()) {
                if (strategy.getDatabaseType().equals(databaseType)) {
                    return TableFormatter.formatTableStructure(strategy.describeTable(dataSource, tableName));
                }
            }
            return "Unsupported database type: " + databaseType;
        } catch (Exception e) {
            return "Failed to retrieve table structure: " + e.getMessage();
        }
    }

    @Tool(
        description = "Insert intelligent test data into database table with advanced generation rules and realistic data patterns. "
            + "Supports complex scenarios like user groups, status variations, sequential IDs, and business workflows.")
    public String insertData(@ToolParam(
        description = "Target database table name (e.g., 'users', 'orders', 'products'). Must be a valid existing table.") String tableName,

        @ToolParam(description = "Total number of records to insert (range: 1-10000). "
            + "When using groups, this parameter is ignored and total is calculated from group recordCounts.") int recordCount,

        @ToolParam(description = "Optional JSON configuration for advanced data generation. Supports 4 modes:\n"
            + "MODE 1 - Simple Fixed Values:\n"
            + "Set same values for all records: {\"status\":1,\"type\":\"active\",\"enabled\":true}\n"
            + "Result: All records get status=1, type='active', enabled=true\n\n"

            + "MODE 2 - Groups (Different record batches):\n"
            + "{\"groups\":[{\"recordCount\":5,\"fixedValues\":{\"status\":1}},{\"recordCount\":3,\"fixedValues\":{\"status\":0}}]}\n"
            + "Result: First 5 records with status=1, next 3 records with status=0\n"
            + "Use for: Creating different user types, order statuses, permission levels\n\n"

            + "MODE 3 - Sequences (Controlled field values):\n"
            + "{\"sequences\":{\"username\":{\"type\":\"CUSTOM_VALUES\",\"customValues\":[\"admin\",\"user1\",\"user2\"]}}}\n"
            + "Sequence types:\n" + "- CUSTOM_VALUES: Use exact values [\"val1\",\"val2\",\"val3\"]\n"
            + "- INCREMENT: Generate numbers {\"type\":\"INCREMENT\",\"startValue\":100,\"step\":10} → 100,110,120...\n"
            + "- PATTERN: Use patterns {\"type\":\"PATTERN\",\"pattern\":\"USER-{000}\"} → USER-001,USER-002...\n\n"

            + "MODE 4 - Combined (Groups + Sequences):\n" + "{\n"
            + "  \"groups\":[{\"recordCount\":3,\"fixedValues\":{\"department\":\"IT\"}},{\"recordCount\":2,\"fixedValues\":{\"department\":\"HR\"}}],\n"
            + "  \"sequences\":{\"employee_id\":{\"type\":\"INCREMENT\",\"startValue\":1000,\"step\":1}}\n" + "}\n"
            + "Result: 3 IT employees (IDs 1000-1002), 2 HR employees (IDs 1003-1004)\n"
            + "Use for: Complex business scenarios with both grouping and sequential control\n\n"

            + "PRACTICAL EXAMPLES:\n"
            + "• User testing: Create admins, regular users, and guests with specific usernames\n"
            + "• Order simulation: Generate orders in different statuses (pending, completed, cancelled)\n"
            + "• Product catalog: Create products with sequential SKUs and different categories\n"
            + "• Employee records: Different departments with sequential employee IDs",
            required = false) String fixedValuesJson) {

        try {
            // Check if INSERT operations are allowed
            String operationValidation = operationConfig.validateInsertOperation(recordCount);
            if (operationValidation != null) {
                return "INSERT operation denied: " + operationValidation;
            }

            // Validate table name
            String tableValidationError = SqlSecurityValidator.getTableNameValidationError(tableName);
            if (tableValidationError != null) {
                return "Invalid table name: " + tableValidationError;
            }

            // Create insert request
            InsertRequest insertRequest = new InsertRequest();
            insertRequest.setTableName(tableName);
            insertRequest.setRecordCount(recordCount);

            // Parse enhanced configuration if provided
            if (fixedValuesJson != null && !fixedValuesJson.trim().isEmpty()) {
                if (!JsonParseUtils.parseEnhancedConfiguration(fixedValuesJson, insertRequest)) {
                    return "Failed to parse configuration JSON";
                }
            }

            // Use actual total record count for validation
            int actualRecordCount = insertRequest.getTotalRecordCount();
            if (SqlSecurityValidator.isInvalidRecordCount(actualRecordCount)) {
                return "Invalid total record count: " + actualRecordCount + " (must be between 1 and 10000)";
            }

            DataSource dataSource = dataSourceFactory.getDataSource(defaultDatabaseConfig);

            // Get the corresponding database strategy
            String databaseType = defaultDatabaseConfig.getDatabaseType();
            for (DatabaseStrategy strategy : dataSourceFactory.getStrategies().values()) {
                if (strategy.getDatabaseType().equals(databaseType)) {
                    // Get table structure
                    List<TableInfo> tableInfos = strategy.describeTable(dataSource, tableName);
                    if (tableInfos.isEmpty()) {
                        return "Table '" + tableName + "' not found or has no columns";
                    }

                    // Generate test data
                    List<Map<String, Object>> testData =
                        DataGenerator.generateTestData(tableInfos, insertRequest, dataSource, strategy);

                    if (testData.isEmpty()) {
                        return "No data generated for insertion";
                    }

                    // Perform batch insert
                    String result = strategy.batchInsert(dataSource, tableName, testData);

                    // Add summary information
                    StringBuilder summary = new StringBuilder(result);
                    summary.append("\n\nGenerated data summary:");
                    summary.append("\n- Table: ").append(tableName);
                    summary.append("\n- Total records: ").append(insertRequest.getTotalRecordCount());

                    if (insertRequest.hasDataGroups()) {
                        summary.append("\n- Groups: ").append(insertRequest.getDataGroups().size());
                        for (int i = 0; i < insertRequest.getDataGroups().size(); i++) {
                            DataGroup group = insertRequest.getDataGroups().get(i);
                            summary.append("\n  Group ").append(i + 1).append(": ").append(group.getRecordCount())
                                .append(" records");
                            if (!group.getFixedValues().isEmpty()) {
                                summary.append(" with ").append(group.getFixedValues());
                            }
                        }
                    }

                    if (insertRequest.hasSequences()) {
                        summary.append("\n- Sequences: ").append(insertRequest.getSequences().keySet());
                    }

                    if (insertRequest.hasConstraints()) {
                        summary.append("\n- Fixed values: ").append(insertRequest.getFixedValues());
                    }

                    return summary.toString();
                }
            }
            return "Unsupported database type: " + databaseType;

        } catch (Exception e) {
            return "Failed to insert test data: " + e.getMessage();
        }
    }

    @Tool(description = "Execute safe SQL SELECT queries with automatic result formatting and 10-record limit. "
        + "Only SELECT statements are allowed for data security.")
    public String
        executeQuery(@ToolParam(description = "SQL SELECT query to execute. Only SELECT statements are permitted. "
            + "Examples: 'SELECT * FROM users', 'SELECT id, name FROM products WHERE status = 1', "
            + "'SELECT COUNT(*) FROM orders'. Results are automatically limited to 10 records for performance.") String sqlQuery) {

        try {
            // Validate SQL query for security
            String securityError = SqlSecurityValidator.validateSelectQuery(sqlQuery);
            if (securityError != null) {
                return "SQL security validation failed: " + securityError;
            }

            // Add automatic LIMIT to query
            String limitedQuery = QueryUtils.addLimitToQuery(QueryUtils.normalizeQuery(sqlQuery));

            // Get data source
            DataSource dataSource = dataSourceFactory.getDataSource(defaultDatabaseConfig);

            // Execute query using database strategy
            String databaseType = defaultDatabaseConfig.getDatabaseType();
            for (DatabaseStrategy strategy : dataSourceFactory.getStrategies().values()) {
                if (strategy.getDatabaseType().equals(databaseType)) {
                    return strategy.executeSelectQuery(dataSource, limitedQuery);
                }
            }

            return "Unsupported database type: " + databaseType;

        } catch (Exception e) {
            return "Failed to execute query: " + e.getMessage();
        }
    }

    @Tool(
        description = "Update database records using structured conditions and values with support for batch operations. "
            + "Supports multiple update rules with different conditions in a single operation with transaction safety.")
    public String updateData(@ToolParam(
        description = "Target database table name (e.g., 'users', 'orders', 'products'). Must be a valid existing table.") String tableName,

        @ToolParam(
            description = "JSON configuration for structured update operations. Supports multiple update rules:\n\n"
                + "SIMPLE UPDATE EXAMPLE:\n" + "{\n" + "  \"updateRules\": [\n" + "    {\n"
                + "      \"conditions\": [{\"field\": \"status\", \"operator\": \"=\", \"value\": 1}],\n"
                + "      \"updateValues\": {\"name\": \"2\", \"username\": \"3\"}\n" + "    }\n" + "  ]\n" + "}\n"
                + "Result: Updates records where status=1, setting name='2' and username='3'\n\n"

                + "MULTIPLE RULES EXAMPLE:\n" + "{\n" + "  \"updateRules\": [\n" + "    {\n"
                + "      \"conditions\": [{\"field\": \"status\", \"operator\": \"=\", \"value\": 1}],\n"
                + "      \"updateValues\": {\"name\": \"2\", \"username\": \"3\"}\n" + "    },\n" + "    {\n"
                + "      \"conditions\": [{\"field\": \"status\", \"operator\": \"=\", \"value\": 2}],\n"
                + "      \"updateValues\": {\"name\": \"3\", \"username\": \"4\"}\n" + "    }\n" + "  ]\n" + "}\n"
                + "Result: Updates status=1 records with name='2',username='3' AND status=2 records with name='3',username='4'\n\n"

                + "BATCH UPDATE EXAMPLE:\n" + "{\n" + "  \"updateRules\": [\n" + "    {\n"
                + "      \"conditions\": [{\"field\": \"status\", \"operator\": \"IN\", \"values\": [1, 2]}],\n"
                + "      \"updateValues\": {\"name\": \"3\"}\n" + "    }\n" + "  ]\n" + "}\n"
                + "Result: Updates all records where status IN (1,2), setting name='3'\n\n"

                + "ADVANCED OPTIONS:\n" + "{\n" + "  \"updateRules\": [...],\n" + "  \"useTransaction\": true,\n"
                + "  \"maxTotalAffectedRecords\": 1000,\n" + "  \"dryRun\": false,\n" + "  \"returnDetails\": true\n"
                + "}\n\n"

                + "SUPPORTED OPERATORS:\n" + "- =, !=, >, <, >=, <= (for single values)\n"
                + "- IN, NOT IN (for multiple values using 'values' array)\n"
                + "- LIKE, NOT LIKE (for pattern matching)\n\n"

                + "SAFETY FEATURES:\n" + "• Transaction support with automatic rollback on errors\n"
                + "• Configurable limits on affected records per rule and total\n"
                + "• SQL injection prevention and input validation\n"
                + "• Dry run mode to preview changes without executing\n"
                + "• Detailed result reporting with affected row counts",
            required = false) String updateConfigJson) {

        try {
            // Validate table name
            String tableValidationError = SqlSecurityValidator.getTableNameValidationError(tableName);
            if (tableValidationError != null) {
                return "Invalid table name: " + tableValidationError;
            }

            // Parse JSON configuration or create simple update request
            UpdateRequest updateRequest;

            if (updateConfigJson != null && !updateConfigJson.trim().isEmpty()) {
                // Parse JSON configuration using JsonParseUtils
                updateRequest = JsonParseUtils.parseUpdateConfiguration(tableName, updateConfigJson);
                if (updateRequest == null) {
                    return "Failed to parse update configuration JSON. Please check the format.";
                }
            } else {
                return "Update configuration JSON is required. Please provide update rules in JSON format.";
            }

            // Check if UPDATE operations are allowed
            String operationValidation = operationConfig.validateUpdateOperation(
                updateRequest.getMaxTotalAffectedRecords(), updateRequest.getUpdateRules());
            if (operationValidation != null) {
                return "UPDATE operation denied: " + operationValidation;
            }

            // Get data source
            DataSource dataSource = dataSourceFactory.getDataSource(defaultDatabaseConfig);

            // Execute update using database strategy
            String databaseType = defaultDatabaseConfig.getDatabaseType();
            for (DatabaseStrategy strategy : dataSourceFactory.getStrategies().values()) {
                if (strategy.getDatabaseType().equals(databaseType)) {
                    return strategy.batchUpdateData(dataSource, updateRequest);
                }
            }

            return "Unsupported database type: " + databaseType;

        } catch (Exception e) {
            return "Failed to execute update operation: " + e.getMessage();
        }
    }

    @Tool(description = "Delete database records using structured conditions with support for batch operations. "
        + "Supports multiple delete rules with different conditions in a single operation with transaction safety.")
    public String deleteData(@ToolParam(
        description = "Target database table name (e.g., 'users', 'orders', 'products'). Must be a valid existing table.") String tableName,

        @ToolParam(
            description = "JSON configuration for structured delete operations. Supports multiple delete rules:\\n\\n"
                + "SIMPLE DELETE EXAMPLE:\\n" + "{\\n" + "  \\\"deleteRules\\\": [\\n" + "    {\\n"
                + "      \\\"conditions\\\": [{\\\"field\\\": \\\"status\\\", \\\"operator\\\": \\\"=\\\", \\\"value\\\": 0}]\\n"
                + "    }\\n" + "  ]\\n" + "}\\n" + "Result: Deletes records where status=0\\n\\n"

                + "MULTIPLE RULES EXAMPLE:\\n" + "{\\n" + "  \\\"deleteRules\\\": [\\n" + "    {\\n"
                + "      \\\"conditions\\\": [{\\\"field\\\": \\\"status\\\", \\\"operator\\\": \\\"=\\\", \\\"value\\\": 0}]\\n"
                + "    },\\n" + "    {\\n"
                + "      \\\"conditions\\\": [{\\\"field\\\": \\\"created_at\\\", \\\"operator\\\": \\\"<\\\", \\\"value\\\": \\\"2023-01-01\\\"}]\\n"
                + "    }\\n" + "  ]\\n" + "}\\n"
                + "Result: Deletes status=0 records AND old records before 2023-01-01\\n\\n"

                + "BATCH DELETE EXAMPLE:\\n" + "{\\n" + "  \\\"deleteRules\\\": [\\n" + "    {\\n"
                + "      \\\"conditions\\\": [{\\\"field\\\": \\\"status\\\", \\\"operator\\\": \\\"IN\\\", \\\"values\\\": [0, -1, 2]}]\\n"
                + "    }\\n" + "  ]\\n" + "}\\n" + "Result: Deletes all records where status IN (0, -1, 2)\\n\\n"

                + "ADVANCED OPTIONS:\\n" + "{\\n" + "  \\\"deleteRules\\\": [...],\\n"
                + "  \\\"useTransaction\\\": true,\\n" + "  \\\"maxTotalAffectedRecords\\\": 1000,\\n"
                + "  \\\"dryRun\\\": false,\\n" + "  \\\"returnDetails\\\": true\\n" + "}\\n\\n"

                + "RULE-SPECIFIC OPTIONS:\\n" + "{\\n" + "  \\\"deleteRules\\\": [\\n" + "    {\\n"
                + "      \\\"conditions\\\": [...],\\n" + "      \\\"maxAffectedRecords\\\": 500,\\n"
                + "      \\\"requireConditions\\\": true,\\n"
                + "      \\\"description\\\": \\\"Delete inactive users\\\"\\n" + "    }\\n" + "  ]\\n" + "}\\n\\n"

                + "SUPPORTED OPERATORS:\\n" + "- =, !=, >, <, >=, <= (for single values)\\n"
                + "- IN, NOT IN (for multiple values using 'values' array)\\n"
                + "- LIKE, NOT LIKE (for pattern matching)\\n\\n"

                + "SAFETY FEATURES:\\n" + "• Transaction support with automatic rollback on errors\\n"
                + "• Configurable limits on affected records per rule and total\\n"
                + "• SQL injection prevention and input validation\\n"
                + "• Dry run mode to preview deletions without executing\\n"
                + "• Detailed result reporting with affected row counts\\n"
                + "• Mandatory conditions by default (prevents accidental full table deletes)",
            required = false) String deleteConfigJson) {

        try {
            // Validate table name
            String tableValidationError = SqlSecurityValidator.getTableNameValidationError(tableName);
            if (tableValidationError != null) {
                return "Invalid table name: " + tableValidationError;
            }

            // Parse JSON configuration or create simple delete request
            DeleteRequest deleteRequest;

            if (deleteConfigJson != null && !deleteConfigJson.trim().isEmpty()) {
                // Parse JSON configuration using JsonParseUtils
                deleteRequest = JsonParseUtils.parseDeleteConfiguration(tableName, deleteConfigJson);
                if (deleteRequest == null) {
                    return "Failed to parse delete configuration JSON. Please check the format.";
                }
            } else {
                return "Delete configuration JSON is required. Please provide delete rules in JSON format.";
            }

            // Check if DELETE operations are allowed
            String operationValidation = operationConfig.validateDeleteOperation(
                deleteRequest.getMaxTotalAffectedRecords(), deleteRequest.getDeleteRules());
            if (operationValidation != null) {
                return "DELETE operation denied: " + operationValidation;
            }

            // Get data source
            DataSource dataSource = dataSourceFactory.getDataSource(defaultDatabaseConfig);

            // Execute delete using database strategy
            String databaseType = defaultDatabaseConfig.getDatabaseType();
            for (DatabaseStrategy strategy : dataSourceFactory.getStrategies().values()) {
                if (strategy.getDatabaseType().equals(databaseType)) {
                    return strategy.batchDeleteData(dataSource, deleteRequest);
                }
            }

            return "Unsupported database type: " + databaseType;

        } catch (Exception e) {
            return "Failed to execute delete operation: " + e.getMessage();
        }
    }

}