package com.carole.database.mcp.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import com.carole.database.mcp.config.DatabaseOperationConfig;
import com.carole.database.mcp.manager.DataSourceManager;
import com.carole.database.mcp.pojo.*;
import com.carole.database.mcp.util.*;

import jakarta.annotation.Resource;

/**
 * Enhanced database service providing MCP (Model Context Protocol) tools for multi-datasource operations
 * 
 * This service exposes database functionality as MCP tools that can be used by AI models to perform database queries,
 * table listing, and structure inspection operations. Enhanced to support multiple data sources with both predefined
 * and dynamic datasource configurations, while maintaining backward compatibility with single datasource mode.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Enhanced MCP server implementation that provides intelligent database querying tools with
 *              multi-datasource support
 */
@Service
public class DatabaseService {

    @Resource
    private DatabaseOperationConfig operationConfig;

    @Resource
    private DataSourceManager dataSourceManager;

    @Tool(description = "List all tables in the database with their metadata including table names and remarks. "
        + "Supports multiple data sources: use predefined datasource name, JSON configuration, or leave empty for default.")
    public String listTables(@ToolParam(description = "Datasource identifier - USE SIMPLE STRING VALUES ONLY:\n"
        + "Valid examples:\n" + "  null (for default datasource)\n" + "  \"pms\" (for PMS database)\n"
        + "  \"admin\" (for Admin database)\n" + "  \"test\" (for test database)\n"
        + "  Complete JSON: {\"host\":\"192.168.31.50\",\"port\":\"3306\",\"username\":\"root\",\"password\":\"Aa040832@\",\"database\":\"mall_pms\",\"databaseType\":\"mysql\"}\n\n"
        + "CRITICAL: NEVER use {\"database\":\"xxx\"} format!\n" + "WRONG: {\"database\":\"admin\"}\n"
        + "CORRECT: \"admin\"", required = false) String dataSourceConfig) {
        return dataSourceManager.executeWithDataSource(dataSourceConfig, (dataSource, strategy) -> {
            return TableFormatter.formatTableList(strategy.listTables(dataSource));
        }, "retrieve table list");
    }

    @Tool(
        description = "Describe table structure including columns, data types, constraints, indexes, and metadata information. "
            + "Supports multiple data sources: use predefined datasource name, JSON configuration, or leave empty for default.")
    public String describeTable(@ToolParam(description = "Name of the table to describe") String tableName,
        @ToolParam(description = "Datasource identifier - USE SIMPLE STRING VALUES ONLY:\n" + "Valid examples:\n"
            + "  null (for default datasource)\n" + "  \"pms\" (for PMS database)\n"
            + "  \"admin\" (for Admin database)\n" + "  \"test\" (for test database)\n"
            + "  Complete JSON: {\"host\":\"192.168.31.50\",\"port\":\"3306\",\"username\":\"root\",\"password\":\"Aa040832@\",\"database\":\"mall_pms\",\"databaseType\":\"mysql\"}\n\n"
            + "CRITICAL: NEVER use {\"database\":\"xxx\"} format!\n" + "WRONG: {\"database\":\"admin\"}\n"
            + "CORRECT: \"admin\"", required = false) String dataSourceConfig) {
        return dataSourceManager.executeWithDataSource(dataSourceConfig, (dataSource, strategy) -> {
            return TableFormatter.formatTableStructure(strategy.describeTable(dataSource, tableName));
        }, "retrieve table structure for '" + tableName + "'");
    }

    @Tool(description = "Insert intelligent test data into database table with multi-datasource support. "
        + "Supports advanced generation rules, realistic data patterns, and complex business scenarios.")
    public String insertData(@ToolParam(
        description = "Target database table name (e.g., 'users', 'orders', 'products'). Must be a valid existing table.") String tableName,

        @ToolParam(description = "Total number of records to insert (range: 1-10000). "
            + "When using groups, this parameter is ignored and total is calculated from group recordCounts.") int recordCount,

        @ToolParam(description = "Optional JSON configuration for advanced data generation. Supports 4 modes:\n"
            + "MODE 1 - Simple Fixed Values: {\"status\":1,\"type\":\"active\"}\n"
            + "MODE 2 - Groups: {\"groups\":[{\"recordCount\":5,\"fixedValues\":{\"status\":1}}]}\n"
            + "MODE 3 - Sequences: {\"sequences\":{\"username\":{\"type\":\"CUSTOM_VALUES\",\"customValues\":[\"admin\",\"user1\"]}}}\n"
            + "MODE 4 - Combined: Groups + Sequences together", required = false) String fixedValuesJson,

        @ToolParam(description = "Optional datasource identifier. Can be:\n"
            + "• null or empty string: Use default datasource\n"
            + "• Predefined datasource name: \"pms\", \"admin\", \"production\", \"test\"\n"
            + "• Complete JSON config: {\"host\":\"192.168.31.50\",\"port\":\"3306\",\"username\":\"root\",\"password\":\"pass\",\"database\":\"mall_pms\",\"databaseType\":\"mysql\"}\n"
            + "IMPORTANT: Do NOT use {\"database\":\"name\"} format - use just \"name\" for predefined datasources",
            required = false) String dataSourceConfig) {

        return dataSourceManager.executeWithDataSource(dataSourceConfig, (dataSource, strategy) -> {
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
        }, "insert test data into '" + tableName + "'");
    }

    @Tool(description = "Execute safe SQL SELECT queries with multi-datasource support. "
        + "Automatic result formatting and 10-record limit. Only SELECT statements are allowed for data security.")
    public String executeQuery(
        @ToolParam(description = "SQL SELECT query to execute. Only SELECT statements are permitted. "
            + "Examples: 'SELECT * FROM users', 'SELECT id, name FROM products WHERE status = 1', "
            + "'SELECT COUNT(*) FROM orders'. Results are automatically limited to 10 records for performance.") String sqlQuery,

        @ToolParam(description = "Optional datasource identifier. Can be:\n"
            + "• null or empty string: Use default datasource\n"
            + "• Predefined datasource name: \"pms\", \"admin\", \"production\", \"test\"\n"
            + "• Complete JSON config: {\"host\":\"192.168.31.50\",\"port\":\"3306\",\"username\":\"root\",\"password\":\"pass\",\"database\":\"mall_pms\",\"databaseType\":\"mysql\"}\n"
            + "IMPORTANT: Do NOT use {\"database\":\"name\"} format - use just \"name\" for predefined datasources",
            required = false) String dataSourceConfig) {

        return dataSourceManager.executeWithDataSource(dataSourceConfig, (dataSource, strategy) -> {
            // Validate SQL query for security
            String securityError = SqlSecurityValidator.validateSelectQuery(sqlQuery);
            if (securityError != null) {
                return "SQL security validation failed: " + securityError;
            }

            // Add automatic LIMIT to query
            String limitedQuery = QueryUtils.addLimitToQuery(QueryUtils.normalizeQuery(sqlQuery));

            return strategy.executeSelectQuery(dataSource, limitedQuery);
        }, "execute query");
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
            required = false) String updateConfigJson,

        @ToolParam(description = "Datasource identifier - USE SIMPLE STRING VALUES ONLY:\n" + "Valid examples:\n"
            + "  null (for default datasource)\n" + "  \"pms\" (for PMS database)\n"
            + "  \"admin\" (for Admin database)\n" + "  \"test\" (for test database)\n"
            + "  Complete JSON: {\"host\":\"192.168.31.50\",\"port\":\"3306\",\"username\":\"root\",\"password\":\"Aa040832@\",\"database\":\"mall_pms\",\"databaseType\":\"mysql\"}\n\n"
            + "CRITICAL: NEVER use {\"database\":\"xxx\"} format!\n" + "WRONG: {\"database\":\"admin\"}\n"
            + "CORRECT: \"admin\"", required = false) String dataSourceConfig) {

        return dataSourceManager.executeWithDataSource(dataSourceConfig, (dataSource, strategy) -> {
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

                // Execute update using database strategy
                return strategy.batchUpdateData(dataSource, updateRequest);

            } catch (Exception e) {
                return "Failed to execute update operation: " + e.getMessage();
            }
        }, "update data in '" + tableName + "'");
    }

    @Tool(description = "Delete database records using structured conditions with support for batch operations. "
        + "Supports multiple delete rules with different conditions in a single operation with transaction safety.")
    public String deleteData(@ToolParam(
        description = "Target database table name (e.g., 'users', 'orders', 'products'). Must be a valid existing table.") String tableName,

        @ToolParam(
            description = "JSON configuration for structured delete operations. Supports multiple delete rules:\n\n"
                + "SIMPLE DELETE EXAMPLE:\n" + "{\n" + "  \"deleteRules\": [\n" + "    {\n"
                + "      \"conditions\": [{\"field\": \"status\", \"operator\": \"=\", \"value\": 0}]\n" + "    }\n"
                + "  ]\n" + "}\n" + "Result: Deletes records where status=0\n\n"

                + "MULTIPLE RULES EXAMPLE:\n" + "{\n" + "  \"deleteRules\": [\n" + "    {\n"
                + "      \"conditions\": [{\"field\": \"status\", \"operator\": \"=\", \"value\": 0}]\n" + "    },\n"
                + "    {\n"
                + "      \"conditions\": [{\"field\": \"created_at\", \"operator\": \"<\", \"value\": \"2023-01-01\"}]\n"
                + "    }\n" + "  ]\n" + "}\n" + "Result: Deletes status=0 records AND old records before 2023-01-01\n\n"

                + "BATCH DELETE EXAMPLE:\n" + "{\n" + "  \"deleteRules\": [\n" + "    {\n"
                + "      \"conditions\": [{\"field\": \"status\", \"operator\": \"IN\", \"values\": [0, -1, 2]}]\n"
                + "    }\n" + "  ]\n" + "}\n" + "Result: Deletes all records where status IN (0, -1, 2)\n\n"

                + "ADVANCED OPTIONS:\n" + "{\n" + "  \"deleteRules\": [...],\n" + "  \"useTransaction\": true,\n"
                + "  \"maxTotalAffectedRecords\": 1000,\n" + "  \"dryRun\": false,\n" + "  \"returnDetails\": true\n"
                + "}\n\n"

                + "RULE-SPECIFIC OPTIONS:\n" + "{\n" + "  \"deleteRules\": [\n" + "    {\n"
                + "      \"conditions\": [...],\n" + "      \"maxAffectedRecords\": 500,\n"
                + "      \"requireConditions\": true,\n" + "      \"description\": \"Delete inactive users\"\n"
                + "    }\n" + "  ]\n" + "}\n\n"

                + "SUPPORTED OPERATORS:\n" + "- =, !=, >, <, >=, <= (for single values)\n"
                + "- IN, NOT IN (for multiple values using 'values' array)\n"
                + "- LIKE, NOT LIKE (for pattern matching)\n\n"

                + "SAFETY FEATURES:\n" + "• Transaction support with automatic rollback on errors\n"
                + "• Configurable limits on affected records per rule and total\n"
                + "• SQL injection prevention and input validation\n"
                + "• Dry run mode to preview deletions without executing\n"
                + "• Detailed result reporting with affected row counts\n"
                + "• Mandatory conditions by default (prevents accidental full table deletes)",
            required = false) String deleteConfigJson,

        @ToolParam(description = "Datasource identifier - USE SIMPLE STRING VALUES ONLY:\n" + "Valid examples:\n"
            + "  null (for default datasource)\n" + "  \"pms\" (for PMS database)\n"
            + "  \"admin\" (for Admin database)\n" + "  \"test\" (for test database)\n"
            + "  Complete JSON: {\"host\":\"192.168.31.50\",\"port\":\"3306\",\"username\":\"root\",\"password\":\"Aa040832@\",\"database\":\"mall_pms\",\"databaseType\":\"mysql\"}\n\n"
            + "CRITICAL: NEVER use {\"database\":\"xxx\"} format!\n" + "WRONG: {\"database\":\"admin\"}\n"
            + "CORRECT: \"admin\"", required = false) String dataSourceConfig) {

        return dataSourceManager.executeWithDataSource(dataSourceConfig, (dataSource, strategy) -> {
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

                // Execute delete using database strategy
                return strategy.batchDeleteData(dataSource, deleteRequest);

            } catch (Exception e) {
                return "Failed to execute delete operation: " + e.getMessage();
            }
        }, "delete data from '" + tableName + "'");
    }

}