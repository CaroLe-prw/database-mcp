package com.carole.database.mcp.util;

import java.util.regex.Pattern;

/**
 * SQL query utilities for processing and formatting queries
 * 
 * This utility provides methods for processing SQL queries, adding limits, and formatting query strings
 * for safe execution in the database MCP environment.
 * 
 * @author CaroLe
 * @Date 2025/7/9
 * @Description SQL query processing utilities for MCP operations
 */
public class QueryUtils {

    /**
     * Maximum number of records allowed in query results
     */
    public static final int MAX_QUERY_LIMIT = 10;

    /**
     * Pattern to match existing LIMIT clauses
     */
    private static final Pattern LIMIT_PATTERN = Pattern.compile("\\bLIMIT\\s+\\d+\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Prevent instantiation of utility class
     */
    private QueryUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Add LIMIT clause to SQL query if not already present
     * 
     * @param sqlQuery Original SQL query
     * @return Query with LIMIT 10 added or existing limit capped at 10
     */
    public static String addLimitToQuery(String sqlQuery) {
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            return sqlQuery;
        }

        String trimmedQuery = sqlQuery.trim();
        
        // Check if LIMIT is already present
        if (LIMIT_PATTERN.matcher(trimmedQuery).find()) {
            // Replace existing LIMIT with MAX_QUERY_LIMIT if it's higher
            return LIMIT_PATTERN.matcher(trimmedQuery).replaceAll("LIMIT " + MAX_QUERY_LIMIT);
        }
        
        // Add LIMIT to the end of the query
        return trimmedQuery + " LIMIT " + MAX_QUERY_LIMIT;
    }

    /**
     * Clean and normalize SQL query string
     * 
     * @param sqlQuery Raw SQL query
     * @return Cleaned and normalized query
     */
    public static String normalizeQuery(String sqlQuery) {
        if (sqlQuery == null) {
            return null;
        }

        // Remove leading/trailing whitespace and normalize internal whitespace
        String normalized = sqlQuery.trim().replaceAll("\\s+", " ");
        
        // Ensure query ends with semicolon removal (not needed for JDBC execution)
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        return normalized;
    }
}