package com.carole.database.mcp.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.carole.database.mcp.pojo.DeleteCondition;
import com.carole.database.mcp.pojo.DeleteRule;
import com.carole.database.mcp.pojo.UpdateCondition;
import com.carole.database.mcp.pojo.UpdateRule;

/**
 * SQL statement builder utility for database operations
 * 
 * This utility provides common SQL building methods for UPDATE and DELETE operations that are shared across
 * different database types. It handles SQL construction, parameter binding, and security validation.
 * 
 * @author CaroLe
 * @Date 2025/07/10
 * @Description Common SQL building utilities for cross-database compatibility
 */
public class SqlBuilder {

    /**
     * Build UPDATE SQL statement for a single update rule
     * 
     * This method constructs a complete UPDATE SQL statement with SET clause and WHERE conditions.
     * It uses proper identifier escaping and parameter placeholders for security.
     * 
     * @param tableName Target table name
     * @param rule Update rule containing conditions and update values
     * @return SQL UPDATE statement with parameter placeholders
     * @author CaroLe
     * @Date 2025/07/10
     * @Description UPDATE SQL construction with security validation
     */
    public static String buildUpdateSql(String tableName, UpdateRule rule) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(SqlSecurityValidator.escapeIdentifier(tableName));
        sql.append(" SET ");

        // Add SET clause
        boolean first = true;
        for (String field : rule.getUpdateValues().keySet()) {
            if (!first) {
                sql.append(", ");
            }
            sql.append(SqlSecurityValidator.escapeIdentifier(field)).append(" = ?");
            first = false;
        }

        // Add WHERE clause if conditions exist
        if (rule.hasConditions()) {
            sql.append(" WHERE ");
            first = true;
            for (UpdateCondition condition : rule.getConditions()) {
                if (!first) {
                    sql.append(" ").append(condition.getConnector()).append(" ");
                }

                sql.append(SqlSecurityValidator.escapeIdentifier(condition.getField()));
                sql.append(" ").append(condition.getOperator()).append(" ");

                if (condition.hasMultipleValues()) {
                    // Handle IN/NOT IN operators
                    sql.append("(");
                    for (int i = 0; i < condition.getValues().size(); i++) {
                        if (i > 0) {
                            sql.append(", ");
                        }
                        sql.append("?");
                    }
                    sql.append(")");
                } else {
                    sql.append("?");
                }

                first = false;
            }
        }

        return sql.toString();
    }

    /**
     * Build DELETE SQL statement for a single delete rule
     * 
     * This method constructs a complete DELETE SQL statement with WHERE conditions.
     * It uses proper identifier escaping and parameter placeholders for security.
     * 
     * @param tableName Target table name
     * @param rule Delete rule containing conditions
     * @return SQL DELETE statement with parameter placeholders
     * @author CaroLe
     * @Date 2025/07/10
     * @Description DELETE SQL construction with security validation
     */
    public static String buildDeleteSql(String tableName, DeleteRule rule) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(SqlSecurityValidator.escapeIdentifier(tableName));

        // Add WHERE clause if conditions exist
        if (rule.hasConditions()) {
            sql.append(" WHERE ");
            boolean first = true;
            for (DeleteCondition condition : rule.getConditions()) {
                if (!first) {
                    sql.append(" ").append(condition.getConnector()).append(" ");
                }

                sql.append(SqlSecurityValidator.escapeIdentifier(condition.getField()));
                sql.append(" ").append(condition.getOperator()).append(" ");

                if (condition.hasMultipleValues()) {
                    // Handle IN/NOT IN operators
                    sql.append("(");
                    for (int i = 0; i < condition.getValues().size(); i++) {
                        if (i > 0) {
                            sql.append(", ");
                        }
                        sql.append("?");
                    }
                    sql.append(")");
                } else {
                    sql.append("?");
                }

                first = false;
            }
        }

        return sql.toString();
    }

    /**
     * Set parameter values for UPDATE statement
     * 
     * This method binds parameter values to a PreparedStatement for UPDATE operations.
     * It handles both SET values and WHERE condition values in the correct order.
     * 
     * @param stmt PreparedStatement to set parameters for
     * @param rule Update rule containing values and conditions
     * @return Next parameter index after setting all parameters
     * @throws SQLException If parameter setting fails
     * @author CaroLe
     * @Date 2025/07/10
     * @Description UPDATE statement parameter binding with proper ordering
     */
    public static int setUpdateParameters(PreparedStatement stmt, UpdateRule rule) throws SQLException {
        int paramIndex = 1;

        // Set UPDATE values
        for (Object value : rule.getUpdateValues().values()) {
            stmt.setObject(paramIndex++, value);
        }

        // Set WHERE condition values
        if (rule.hasConditions()) {
            for (UpdateCondition condition : rule.getConditions()) {
                if (condition.hasMultipleValues()) {
                    // Set multiple values for IN/NOT IN
                    for (Object value : condition.getValues()) {
                        stmt.setObject(paramIndex++, value);
                    }
                } else {
                    // Set single value
                    stmt.setObject(paramIndex++, condition.getValue());
                }
            }
        }

        return paramIndex;
    }

    /**
     * Set parameter values for DELETE statement
     * 
     * This method binds parameter values to a PreparedStatement for DELETE operations.
     * It handles WHERE condition values for various operators including IN/NOT IN.
     * 
     * @param stmt PreparedStatement to set parameters for
     * @param rule Delete rule containing conditions
     * @return Next parameter index after setting all parameters
     * @throws SQLException If parameter setting fails
     * @author CaroLe
     * @Date 2025/07/10
     * @Description DELETE statement parameter binding with operator support
     */
    public static int setDeleteParameters(PreparedStatement stmt, DeleteRule rule) throws SQLException {
        int paramIndex = 1;

        // Set WHERE condition values
        if (rule.hasConditions()) {
            for (DeleteCondition condition : rule.getConditions()) {
                if (condition.hasMultipleValues()) {
                    // Set multiple values for IN/NOT IN
                    for (Object value : condition.getValues()) {
                        stmt.setObject(paramIndex++, value);
                    }
                } else {
                    // Set single value
                    stmt.setObject(paramIndex++, condition.getValue());
                }
            }
        }

        return paramIndex;
    }

    /**
     * Validate SQL operation parameters
     * 
     * This method performs common validation for SQL operations including table name validation,
     * rule validation, and safety checks that apply across different database types.
     * 
     * @param tableName Target table name
     * @param hasConditions Whether the operation has WHERE conditions
     * @param allowUnconditional Whether to allow operations without conditions
     * @return Validation error message or null if valid
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Cross-database validation for SQL operations
     */
    public static String validateSqlOperation(String tableName, boolean hasConditions, boolean allowUnconditional) {
        // Validate table name
        String tableError = SqlSecurityValidator.getTableNameValidationError(tableName);
        if (tableError != null) {
            return tableError;
        }

        // Check if conditions are required
        if (!hasConditions && !allowUnconditional) {
            return "SQL operations require at least one condition for safety. Use allowUnconditional=true to bypass this check.";
        }

        return null;
    }

    /**
     * Build WHERE clause for conditions
     * 
     * This method constructs a WHERE clause from a list of conditions, handling different operators
     * and connector logic (AND/OR). This is a reusable component for various SQL operations.
     * 
     * @param conditions List of conditions to build WHERE clause from
     * @return WHERE clause SQL string without the "WHERE" keyword
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Reusable WHERE clause construction for multiple condition types
     */
    public static String buildWhereClause(List<? extends Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        StringBuilder whereClause = new StringBuilder();
        boolean first = true;

        for (Object conditionObj : conditions) {
            String field, operator, connector;
            boolean hasMultipleValues;
            List<Object> values;

            // Handle both UpdateCondition and DeleteCondition
            if (conditionObj instanceof UpdateCondition condition) {
                field = condition.getField();
                operator = condition.getOperator();
                connector = condition.getConnector();
                hasMultipleValues = condition.hasMultipleValues();
                values = condition.getValues();
            } else if (conditionObj instanceof DeleteCondition condition) {
                field = condition.getField();
                operator = condition.getOperator();
                connector = condition.getConnector();
                hasMultipleValues = condition.hasMultipleValues();
                values = condition.getValues();
            } else {
                continue; // Skip unknown condition types
            }

            if (!first) {
                whereClause.append(" ").append(connector).append(" ");
            }

            whereClause.append(SqlSecurityValidator.escapeIdentifier(field));
            whereClause.append(" ").append(operator).append(" ");

            if (hasMultipleValues) {
                // Handle IN/NOT IN operators
                whereClause.append("(");
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) {
                        whereClause.append(", ");
                    }
                    whereClause.append("?");
                }
                whereClause.append(")");
            } else {
                whereClause.append("?");
            }

            first = false;
        }

        return whereClause.toString();
    }
}