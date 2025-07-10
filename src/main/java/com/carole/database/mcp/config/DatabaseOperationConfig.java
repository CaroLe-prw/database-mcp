package com.carole.database.mcp.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Database operation configuration for controlling database access permissions and limits
 * 
 * This configuration class manages operation-level permissions and safety limits through environment variables,
 * allowing different restrictions in development, testing, and production environments.
 * 
 * @author CaroLe
 * @Date 2025/07/10
 * @Description Database operation permissions and limits configuration via environment variables
 */
@Data
@Component
public class DatabaseOperationConfig {

    /**
     * Whether INSERT operations are allowed Environment variable: ALLOW_INSERT_OPERATION Default: true (allow
     * insertions)
     */
    @Value("${ALLOW_INSERT_OPERATION:true}")
    private boolean allowInsertOperation;

    /**
     * Whether UPDATE operations are allowed Environment variable: ALLOW_UPDATE_OPERATION Default: true (allow updates)
     */
    @Value("${ALLOW_UPDATE_OPERATION:true}")
    private boolean allowUpdateOperation;

    /**
     * Whether DELETE operations are allowed Environment variable: ALLOW_DELETE_OPERATION Default: true (allow
     * deletions)
     */
    @Value("${ALLOW_DELETE_OPERATION:true}")
    private boolean allowDeleteOperation;

    /**
     * Maximum number of records allowed in a single INSERT operation Environment variable: MAX_INSERT_RECORDS Default:
     * 10000 records
     */
    @Value("${MAX_INSERT_RECORDS:10000}")
    private int maxInsertRecords;

    /**
     * Maximum number of records allowed to be affected in a single UPDATE operation Environment variable:
     * MAX_UPDATE_RECORDS Default: 5000 records
     */
    @Value("${MAX_UPDATE_RECORDS:5000}")
    private int maxUpdateRecords;

    /**
     * Maximum number of records allowed to be affected in a single DELETE operation Environment variable:
     * MAX_DELETE_RECORDS Default: 1000 records
     */
    @Value("${MAX_DELETE_RECORDS:1000}")
    private int maxDeleteRecords;

    /**
     * Validate INSERT operation permissions and limits
     * 
     * @param recordCount Number of records to insert
     * @return Error message if validation fails, null if valid
     */
    public String validateInsertOperation(int recordCount) {
        if (!allowInsertOperation) {
            return "INSERT operations are disabled by configuration (ALLOW_INSERT_OPERATION=false)";
        }

        if (recordCount > maxInsertRecords) {
            return String.format("Insert record count %d exceeds maximum limit of %d (MAX_INSERT_RECORDS)", recordCount,
                maxInsertRecords);
        }

        if (recordCount <= 0) {
            return "Insert record count must be positive";
        }

        return null;
    }

    /**
     * Validate UPDATE operation permissions and limits
     * 
     * @param maxAffectedRecords Maximum records that could be affected
     * @param updateRules List of update rules to validate
     * @return Error message if validation fails, null if valid
     */
    public String validateUpdateOperation(int maxAffectedRecords, List<?> updateRules) {
        if (!allowUpdateOperation) {
            return "UPDATE operations are disabled by configuration (ALLOW_UPDATE_OPERATION=false)";
        }

        if (maxAffectedRecords > maxUpdateRecords) {
            return String.format(
                "Update operation could affect %d records, exceeding maximum limit of %d (MAX_UPDATE_RECORDS)",
                maxAffectedRecords, maxUpdateRecords);
        }

        return null;
    }

    /**
     * Validate DELETE operation permissions and limits
     * 
     * @param maxAffectedRecords Maximum records that could be affected
     * @param deleteRules List of delete rules to validate
     * @return Error message if validation fails, null if valid
     */
    public String validateDeleteOperation(int maxAffectedRecords, List<?> deleteRules) {
        if (!allowDeleteOperation) {
            return "DELETE operations are disabled by configuration (ALLOW_DELETE_OPERATION=false)";
        }

        if (maxAffectedRecords > maxDeleteRecords) {
            return String.format(
                "Delete operation could affect %d records, exceeding maximum limit of %d (MAX_DELETE_RECORDS)",
                maxAffectedRecords, maxDeleteRecords);
        }

        return null;
    }

    /**
     * Check if operation is in development mode (most operations allowed)
     * 
     * @return true if in development mode
     */
    public boolean isDevelopmentMode() {
        return allowInsertOperation && allowUpdateOperation && allowDeleteOperation;
    }

}