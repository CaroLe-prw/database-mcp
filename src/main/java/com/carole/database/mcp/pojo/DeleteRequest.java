package com.carole.database.mcp.pojo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Delete request for database delete operations
 * 
 * This class represents a complete delete request containing multiple delete rules, transaction settings, and safety
 * limits. It provides structured and safe database deletion operations with comprehensive validation and control.
 * 
 * @author CaroLe
 * @Date 2025/07/10
 * @Description Delete request object with rules, transaction support, and safety settings
 */
@Data
public class DeleteRequest {
    
    /**
     * Target table name for delete operations
     */
    private String tableName;
    
    /**
     * List of delete rules to execute
     */
    private List<DeleteRule> deleteRules = new ArrayList<>();
    
    /**
     * Whether to use transaction for all delete operations
     * Default is true for safety
     */
    private boolean useTransaction = true;
    
    /**
     * Maximum total number of records that can be affected across all rules
     * Default is 5000 for safety
     */
    private int maxTotalAffectedRecords = 5000;
    
    /**
     * Whether to run in dry run mode (preview only, no actual deletion)
     * Default is false
     */
    private boolean dryRun = false;
    
    /**
     * Whether to return detailed information about each rule execution
     * Default is true
     */
    private boolean returnDetails = true;
    
    /**
     * Create a new delete request for the specified table
     * 
     * @param tableName Target table name
     */
    public DeleteRequest(String tableName) {
        this.tableName = tableName;
    }
    
    /**
     * Create a new delete request with a single rule
     * 
     * @param tableName Target table name
     * @param rule Delete rule
     * @return DeleteRequest instance
     */
    public static DeleteRequest withRule(String tableName, DeleteRule rule) {
        DeleteRequest request = new DeleteRequest(tableName);
        request.addDeleteRule(rule);
        return request;
    }
    
    /**
     * Create a new delete request with multiple rules
     * 
     * @param tableName Target table name
     * @param rules List of delete rules
     * @return DeleteRequest instance
     */
    public static DeleteRequest withRules(String tableName, List<DeleteRule> rules) {
        DeleteRequest request = new DeleteRequest(tableName);
        request.setDeleteRules(new ArrayList<>(rules));
        return request;
    }
    
    /**
     * Add a delete rule to this request
     * 
     * @param rule Delete rule to add
     * @return This DeleteRequest instance for method chaining
     */
    public DeleteRequest addDeleteRule(DeleteRule rule) {
        if (rule != null) {
            this.deleteRules.add(rule);
        }
        return this;
    }
    
    /**
     * Add multiple delete rules to this request
     * 
     * @param rules List of delete rules to add
     * @return This DeleteRequest instance for method chaining
     */
    public DeleteRequest addDeleteRules(List<DeleteRule> rules) {
        if (rules != null) {
            this.deleteRules.addAll(rules);
        }
        return this;
    }
    
    /**
     * Set whether to use transaction
     * 
     * @param useTransaction Whether to use transaction
     * @return This DeleteRequest instance for method chaining
     */
    public DeleteRequest withTransaction(boolean useTransaction) {
        this.useTransaction = useTransaction;
        return this;
    }
    
    /**
     * Set the maximum total affected records
     * 
     * @param maxTotalAffectedRecords Maximum total affected records
     * @return This DeleteRequest instance for method chaining
     */
    public DeleteRequest withMaxTotalAffectedRecords(int maxTotalAffectedRecords) {
        this.maxTotalAffectedRecords = maxTotalAffectedRecords;
        return this;
    }
    
    /**
     * Set dry run mode
     * 
     * @param dryRun Whether to run in dry run mode
     * @return This DeleteRequest instance for method chaining
     */
    public DeleteRequest withDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }
    
    /**
     * Set whether to return detailed information
     * 
     * @param returnDetails Whether to return detailed information
     * @return This DeleteRequest instance for method chaining
     */
    public DeleteRequest withReturnDetails(boolean returnDetails) {
        this.returnDetails = returnDetails;
        return this;
    }
    
    /**
     * Check if this request has delete rules
     * 
     * @return true if request has delete rules
     */
    public boolean hasDeleteRules() {
        return deleteRules != null && !deleteRules.isEmpty();
    }
    
    /**
     * Get the number of delete rules in this request
     * 
     * @return Number of delete rules
     */
    public int getDeleteRuleCount() {
        return deleteRules != null ? deleteRules.size() : 0;
    }
    
    /**
     * Validate this delete request
     * 
     * @return Error message if invalid, null if valid
     */
    public String validate() {
        // Check table name
        if (tableName == null || tableName.trim().isEmpty()) {
            return "Table name cannot be null or empty";
        }
        
        // Check if we have rules
        if (!hasDeleteRules()) {
            return "Delete request must have at least one delete rule";
        }
        
        // Validate maximum total affected records
        if (maxTotalAffectedRecords <= 0) {
            return "Maximum total affected records must be positive";
        }
        
        if (maxTotalAffectedRecords > 100000) {
            return "Maximum total affected records cannot exceed 100000 for safety";
        }
        
        // Validate each rule
        for (int i = 0; i < deleteRules.size(); i++) {
            DeleteRule rule = deleteRules.get(i);
            String ruleError = rule.validate();
            if (ruleError != null) {
                return "Rule " + (i + 1) + " validation failed: " + ruleError;
            }
        }
        
        return null;
    }
    
    /**
     * Get a description of this request
     * 
     * @return Human-readable description
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Delete from ").append(tableName);
        
        if (hasDeleteRules()) {
            desc.append(" with ").append(deleteRules.size()).append(" rule(s)");
        }
        
        if (dryRun) {
            desc.append(" (DRY RUN)");
        }
        
        if (useTransaction) {
            desc.append(" (TRANSACTION)");
        }
        
        return desc.toString();
    }
    
    /**
     * Create a request that deletes all records (no conditions)
     * WARNING: This is dangerous and should be used with extreme caution
     * 
     * @param tableName Target table name
     * @param maxRecords Maximum records to delete
     * @return DeleteRequest instance with no conditions
     */
    public static DeleteRequest deleteAll(String tableName, int maxRecords) {
        DeleteRequest request = new DeleteRequest(tableName);
        request.addDeleteRule(DeleteRule.deleteAll(maxRecords));
        request.setMaxTotalAffectedRecords(maxRecords);
        return request;
    }
    
    /**
     * Create a request that deletes records by ID
     * 
     * @param tableName Target table name
     * @param idField ID field name
     * @param id ID value
     * @return DeleteRequest instance
     */
    public static DeleteRequest deleteById(String tableName, String idField, Object id) {
        DeleteRequest request = new DeleteRequest(tableName);
        request.addDeleteRule(DeleteRule.deleteById(idField, id));
        return request;
    }
    
    /**
     * Create a request that deletes records by multiple IDs
     * 
     * @param tableName Target table name
     * @param idField ID field name
     * @param ids List of ID values
     * @return DeleteRequest instance
     */
    public static DeleteRequest deleteByIds(String tableName, String idField, List<Object> ids) {
        DeleteRequest request = new DeleteRequest(tableName);
        request.addDeleteRule(DeleteRule.deleteByIds(idField, ids));
        return request;
    }
    
    /**
     * Create a request that deletes records by status
     * 
     * @param tableName Target table name
     * @param statusField Status field name
     * @param status Status value
     * @return DeleteRequest instance
     */
    public static DeleteRequest deleteByStatus(String tableName, String statusField, Object status) {
        DeleteRequest request = new DeleteRequest(tableName);
        request.addDeleteRule(DeleteRule.deleteByStatus(statusField, status));
        return request;
    }
}