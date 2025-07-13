package com.carole.database.mcp.util;

import java.util.regex.Pattern;

/**
 * SQL security validator for preventing SQL injection and validating database identifiers
 * 
 * This utility provides methods to validate table names, column names, and other SQL identifiers to prevent SQL
 * injection attacks and ensure compliance with SQL naming conventions.
 * 
 * @author CaroLe
 * @Date 2025/7/7
 * @Description SQL security validator for database operations
 */
public class SqlSecurityValidator {

    // Valid SQL identifier pattern: starts with letter or underscore, followed by letters, digits, or underscores
    private static final Pattern VALID_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    // Maximum length for SQL identifiers (MySQL standard)
    private static final int MAX_IDENTIFIER_LENGTH = 64;

    // SQL keywords that should not be used as identifiers
    private static final String[] SQL_KEYWORDS =
        {"SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE", "UNION", "JOIN", "WHERE",
            "ORDER", "GROUP", "HAVING", "INDEX", "TABLE", "DATABASE", "SCHEMA", "VIEW", "PROCEDURE", "FUNCTION",
            "TRIGGER", "USER", "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "TRANSACTION", "LOCK", "UNLOCK"};

    /**
     * Validate table name for SQL safety
     * 
     * @param tableName Table name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidTableName(String tableName) {
        return isValidIdentifier(tableName);
    }

    /**
     * Validate column name for SQL safety
     * 
     * @param columnName Column name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidColumnName(String columnName) {
        return isValidIdentifier(columnName);
    }

    /**
     * Validate SQL identifier (table name, column name, etc.)
     * 
     * @param identifier Identifier to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }

        identifier = identifier.trim();

        // Check length
        if (identifier.length() > MAX_IDENTIFIER_LENGTH) {
            return false;
        }

        // Check pattern
        if (!VALID_IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            return false;
        }

        // Check if it's a reserved keyword
        return !isReservedKeyword(identifier);
    }

    /**
     * Check if identifier is a reserved SQL keyword
     * 
     * @param identifier Identifier to check
     * @return true if it's a reserved keyword
     */
    public static boolean isReservedKeyword(String identifier) {
        if (identifier == null) {
            return false;
        }

        String upperIdentifier = identifier.toUpperCase();
        for (String keyword : SQL_KEYWORDS) {
            if (keyword.equals(upperIdentifier)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Escape SQL identifier by wrapping in backticks (MySQL style)
     * 
     * @param identifier Identifier to escape
     * @return Escaped identifier
     */
    public static String escapeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }

        // Remove existing backticks and escape any internal backticks
        String cleaned = identifier.replace("`", "``");
        return "`" + cleaned + "`";
    }

    /**
     * Validate record count for batch operations
     * 
     * @param count Record count to validate
     * @return true if valid
     */
    public static boolean isInvalidRecordCount(int count) {
        return count < 1 || count > 10000;
    }

    /**
     * Get validation error message for table name
     * 
     * @param tableName Table name to validate
     * @return Error message or null if valid
     */
    public static String getTableNameValidationError(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return "Table name cannot be null or empty";
        }

        tableName = tableName.trim();

        if (tableName.length() > MAX_IDENTIFIER_LENGTH) {
            return "Table name too long (maximum " + MAX_IDENTIFIER_LENGTH + " characters)";
        }

        if (!VALID_IDENTIFIER_PATTERN.matcher(tableName).matches()) {
            return "Table name contains invalid characters (only letters, digits, and underscores allowed, must start with letter or underscore)";
        }

        if (isReservedKeyword(tableName)) {
            return "Table name is a reserved SQL keyword: " + tableName;
        }

        return null;
    }

    /**
     * Get validation error message for column name
     * 
     * @param columnName Column name to validate
     * @return Error message or null if valid
     */
    public static String getColumnNameValidationError(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) {
            return "Column name cannot be null or empty";
        }

        columnName = columnName.trim();

        if (columnName.length() > MAX_IDENTIFIER_LENGTH) {
            return "Column name too long (maximum " + MAX_IDENTIFIER_LENGTH + " characters)";
        }

        if (!VALID_IDENTIFIER_PATTERN.matcher(columnName).matches()) {
            return "Column name contains invalid characters (only letters, digits, and underscores allowed, must start with letter or underscore)";
        }

        if (isReservedKeyword(columnName)) {
            return "Column name is a reserved SQL keyword: " + columnName;
        }

        return null;
    }

    /**
     * Check if string contains potential SQL injection patterns
     * 
     * @param input Input string to check
     * @return true if potentially dangerous
     */
    public static boolean containsSqlInjectionPatterns(String input) {
        if (input == null) {
            return false;
        }

        String upperInput = input.toUpperCase();

        // Check for common SQL injection patterns
        String[] dangerousPatterns = {"';", "--", "/*", "*/", "UNION", "SELECT", "INSERT", "UPDATE", "DELETE", "DROP",
            "EXEC", "EXECUTE", "SP_", "XP_"};

        for (String pattern : dangerousPatterns) {
            if (upperInput.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate SELECT query for security and compliance
     * 
     * @param sqlQuery SQL query to validate
     * @return Error message if validation fails, null if valid
     */
    public static String validateSelectQuery(String sqlQuery) {
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            return "SQL query cannot be null or empty";
        }

        String trimmedQuery = sqlQuery.trim();
        String upperQuery = trimmedQuery.toUpperCase();

        // Must start with SELECT
        if (!upperQuery.startsWith("SELECT")) {
            return "Only SELECT statements are allowed. Query must start with 'SELECT'";
        }

        // Check for dangerous operations in SELECT context using word boundaries
        String[] forbiddenInSelect = {"INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE", "GRANT",
            "REVOKE", "EXEC", "EXECUTE", "CALL", "SET", "USE"};

        for (String forbidden : forbiddenInSelect) {
            // Use word boundary regex to match complete words only
            String pattern = "\\b" + forbidden + "\\b";
            if (upperQuery.matches(".*" + pattern + ".*")) {
                return "Forbidden operation detected: " + forbidden + ". Only SELECT queries are allowed";
            }
        }

        // Check for SQL injection patterns specific to queries
        String[] dangerousPatterns = {"';", "/*", "*/", "--", "XP_", "SP_", "OPENROWSET", "OPENDATASOURCE"};

        for (String pattern : dangerousPatterns) {
            if (upperQuery.contains(pattern)) {
                return "Potentially dangerous SQL pattern detected: " + pattern;
            }
        }

        // Check for nested queries with modification operations
        if (upperQuery.contains("(") && upperQuery.contains(")")) {
            String subQueryContent = extractSubQueries(upperQuery);
            for (String forbidden : forbiddenInSelect) {
                if (subQueryContent.contains(forbidden)) {
                    return "Forbidden operation in subquery: " + forbidden;
                }
            }
        }

        return null;
    }

    /**
     * Validate UPDATE operation parameters
     * 
     * @deprecated Use DatabaseOperationConfig.validateUpdateOperation() instead
     * @param tableName Table name to validate
     * @param maxAffectedRecords Maximum number of records that can be affected
     * @return Error message if validation fails, null if valid
     */
    @Deprecated
    public static String validateUpdateOperation(String tableName, int maxAffectedRecords) {
        // Validate table name
        String tableError = getTableNameValidationError(tableName);
        if (tableError != null) {
            return tableError;
        }

        // Validate max affected records
        if (maxAffectedRecords <= 0) {
            return "Maximum affected records must be positive";
        }

        if (maxAffectedRecords > 50000) {
            return "Maximum affected records cannot exceed 50000 for safety";
        }

        return null;
    }

    /**
     * Validate DELETE operation parameters
     * 
     * @deprecated Use DatabaseOperationConfig.validateDeleteOperation() instead
     * @param tableName Table name to validate
     * @param maxAffectedRecords Maximum number of records that can be affected
     * @return Error message if validation fails, null if valid
     */
    @Deprecated
    public static String validateDeleteOperation(String tableName, int maxAffectedRecords) {
        // Validate table name
        String tableError = getTableNameValidationError(tableName);
        if (tableError != null) {
            return tableError;
        }

        // Validate max affected records
        if (maxAffectedRecords <= 0) {
            return "Maximum affected records must be positive";
        }

        if (maxAffectedRecords > 100000) {
            return "Maximum affected records cannot exceed 100000 for safety";
        }

        return null;
    }

    /**
     * Extract content of subqueries for validation
     * 
     * @param upperQuery SQL query in uppercase
     * @return Content of subqueries concatenated
     */
    private static String extractSubQueries(String upperQuery) {
        StringBuilder subQueries = new StringBuilder();
        boolean inParentheses = false;

        for (char c : upperQuery.toCharArray()) {
            if (c == '(') {
                inParentheses = true;
            } else if (c == ')') {
                inParentheses = false;
            } else if (inParentheses) {
                subQueries.append(c);
            }
        }

        return subQueries.toString();
    }
}