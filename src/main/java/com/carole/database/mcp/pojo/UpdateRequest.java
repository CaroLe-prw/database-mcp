package com.carole.database.mcp.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Update request class for structured database updates
 * 
 * This class represents a complete update request that can contain multiple update rules. Each request targets a
 * specific table and can perform multiple different updates with different conditions in a single operation.
 * 
 * @author CaroLe
 * @Date 2025/07/10
 * @Description Complete update request supporting multiple update rules for structured database updates
 */
@Data
public class UpdateRequest {

    /**
     * Target table name for the update operation
     */
    private String tableName;

    /**
     * List of update rules to apply Each rule contains its own conditions and update values
     */
    private List<UpdateRule> updateRules;

    /**
     * Whether to execute all rules in a single transaction Default is true for data consistency
     */
    private boolean useTransaction = true;

    /**
     * Maximum total number of records that can be affected by all rules combined Default is 5000 to prevent accidental
     * mass updates
     */
    private int maxTotalAffectedRecords = 5000;

    /**
     * Whether to return detailed information about affected records Default is true for better feedback
     */
    private boolean returnDetails = true;

    /**
     * Whether to perform a dry run (validate and show what would be updated without actually updating) Default is false
     */
    private boolean dryRun = false;

    /**
     * Default constructor
     */
    public UpdateRequest() {
        this.updateRules = new ArrayList<>();
    }

    /**
     * Constructor with table name
     * 
     * @param tableName Target table name
     */
    public UpdateRequest(String tableName) {
        this.tableName = tableName;
        this.updateRules = new ArrayList<>();
    }

    /**
     * Constructor with table name and update rules
     * 
     * @param tableName Target table name
     * @param updateRules List of update rules
     */
    public UpdateRequest(String tableName, List<UpdateRule> updateRules) {
        this.tableName = tableName;
        this.updateRules = updateRules != null ? updateRules : new ArrayList<>();
    }

    /**
     * Add an update rule to this request
     * 
     * @param rule The update rule to add
     * @return This UpdateRequest instance for method chaining
     */
    public UpdateRequest addUpdateRule(UpdateRule rule) {
        if (this.updateRules == null) {
            this.updateRules = new ArrayList<>();
        }
        this.updateRules.add(rule);
        return this;
    }

    /**
     * Add a simple update rule with single condition
     * 
     * @param conditionField Field name for condition
     * @param conditionValue Value for condition
     * @param updateField Field name to update
     * @param updateValue New value to set
     * @return This UpdateRequest instance for method chaining
     */
    public UpdateRequest addSimpleRule(String conditionField, Object conditionValue, String updateField,
        Object updateValue) {
        UpdateRule rule = UpdateRule.simple(conditionField, conditionValue, updateField, updateValue);
        return addUpdateRule(rule);
    }

    /**
     * Add an update rule with multiple conditions and updates
     * 
     * @param conditions Map of field names to condition values
     * @param updateValues Map of field names to update values
     * @return This UpdateRequest instance for method chaining
     */
    public UpdateRequest addRule(Map<String, Object> conditions, Map<String, Object> updateValues) {
        UpdateRule rule = UpdateRule.withConditions(conditions, updateValues);
        return addUpdateRule(rule);
    }

    /**
     * Create a simple update request with single rule
     * 
     * @param tableName Target table name
     * @param conditionField Field name for condition
     * @param conditionValue Value for condition
     * @param updateField Field name to update
     * @param updateValue New value to set
     * @return New UpdateRequest instance
     */
    public static UpdateRequest simple(String tableName, String conditionField, Object conditionValue,
        String updateField, Object updateValue) {
        UpdateRequest request = new UpdateRequest(tableName);
        request.addSimpleRule(conditionField, conditionValue, updateField, updateValue);
        return request;
    }

    /**
     * Create an update request with multiple rules
     * 
     * @param tableName Target table name
     * @param rules List of update rules
     * @return New UpdateRequest instance
     */
    public static UpdateRequest withRules(String tableName, List<UpdateRule> rules) {
        return new UpdateRequest(tableName, rules);
    }

    /**
     * Check if this request has any update rules
     * 
     * @return true if request has rules
     */
    public boolean hasUpdateRules() {
        return updateRules != null && !updateRules.isEmpty();
    }

    /**
     * Get the number of update rules in this request
     * 
     * @return Number of update rules
     */
    public int getRuleCount() {
        return updateRules != null ? updateRules.size() : 0;
    }

    /**
     * Get the total number of conditions across all rules
     * 
     * @return Total condition count
     */
    public int getTotalConditionCount() {
        if (updateRules == null) {
            return 0;
        }
        return updateRules.stream().mapToInt(UpdateRule::getConditionCount).sum();
    }

    /**
     * Get the total number of update values across all rules
     * 
     * @return Total update value count
     */
    public int getTotalUpdateValueCount() {
        if (updateRules == null) {
            return 0;
        }
        return updateRules.stream().mapToInt(UpdateRule::getUpdateValueCount).sum();
    }

    /**
     * Check if any rule in this request has no conditions (affects all records)
     * 
     * @return true if any rule has no conditions
     */
    public boolean hasUnconditionalRules() {
        if (updateRules == null) {
            return false;
        }
        return updateRules.stream().anyMatch(rule -> !rule.hasConditions());
    }

    /**
     * Validate this update request
     * 
     * @return Error message if invalid, null if valid
     */
    public String validate() {
        // Check table name
        if (tableName == null || tableName.trim().isEmpty()) {
            return "Table name cannot be null or empty";
        }

        // Basic table name validation
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return "Invalid table name format: " + tableName;
        }

        // Check if we have update rules
        if (!hasUpdateRules()) {
            return "Update request must have at least one update rule";
        }

        // Validate each update rule
        for (int i = 0; i < updateRules.size(); i++) {
            UpdateRule rule = updateRules.get(i);
            String ruleError = rule.validate();
            if (ruleError != null) {
                return "Update rule " + (i + 1) + " is invalid: " + ruleError;
            }
        }

        // Check for safety limits
        if (maxTotalAffectedRecords <= 0) {
            return "Maximum total affected records must be positive";
        }

        // Warn about unconditional rules
        if (hasUnconditionalRules() && !dryRun) {
            return "Unconditional update rules detected. This will affect all records in the table. Use dryRun=true to preview or add conditions.";
        }

        return null;
    }

    /**
     * Get a summary description of this update request
     * 
     * @return Human-readable summary
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Update request for table '").append(tableName).append("':\n");
        summary.append("- Rules: ").append(getRuleCount()).append("\n");
        summary.append("- Total conditions: ").append(getTotalConditionCount()).append("\n");
        summary.append("- Total updates: ").append(getTotalUpdateValueCount()).append("\n");
        summary.append("- Transaction: ").append(useTransaction).append("\n");
        summary.append("- Max affected records: ").append(maxTotalAffectedRecords).append("\n");
        summary.append("- Dry run: ").append(dryRun).append("\n");

        if (hasUnconditionalRules()) {
            summary.append("- WARNING: Contains unconditional rules (affects all records)\n");
        }

        return summary.toString();
    }

    /**
     * Get detailed description of all rules
     * 
     * @return Detailed rule descriptions
     */
    public String getDetailedDescription() {
        StringBuilder details = new StringBuilder();
        details.append(getSummary()).append("\n");
        details.append("Rules:\n");

        for (int i = 0; i < updateRules.size(); i++) {
            UpdateRule rule = updateRules.get(i);
            details.append("  ").append(i + 1).append(". ").append(rule.getDescription()).append("\n");
        }

        return details.toString();
    }
}