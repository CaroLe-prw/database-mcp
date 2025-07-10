package com.carole.database.mcp.pojo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Delete rule for database delete operations
 * 
 * This class represents a single delete rule containing multiple conditions and safety settings. Each rule can contain
 * multiple conditions combined with AND/OR operators, and includes safety limits and validation requirements.
 * 
 * @author CaroLe
 * @Date 2025/07/10
 * @Description Delete rule object with conditions and safety settings
 */
@Data
public class DeleteRule {
    
    /**
     * List of conditions for this delete rule
     */
    private List<DeleteCondition> conditions = new ArrayList<>();
    
    /**
     * Maximum number of records this rule can affect
     * Default is 1000 for safety
     */
    private int maxAffectedRecords = 1000;
    
    /**
     * Whether this rule requires at least one condition (prevents full table deletes)
     * Default is true for safety
     */
    private boolean requireConditions = true;
    
    /**
     * Optional description for this rule
     */
    private String description;
    
    /**
     * Create a new delete rule with a single condition
     * 
     * @param condition Delete condition
     * @return DeleteRule instance
     */
    public static DeleteRule withCondition(DeleteCondition condition) {
        DeleteRule rule = new DeleteRule();
        rule.addCondition(condition);
        return rule;
    }
    
    /**
     * Create a new delete rule with multiple conditions
     * 
     * @param conditions List of delete conditions
     * @return DeleteRule instance
     */
    public static DeleteRule withConditions(List<DeleteCondition> conditions) {
        DeleteRule rule = new DeleteRule();
        rule.setConditions(new ArrayList<>(conditions));
        return rule;
    }
    
    /**
     * Add a condition to this rule
     * 
     * @param condition Delete condition to add
     * @return This DeleteRule instance for method chaining
     */
    public DeleteRule addCondition(DeleteCondition condition) {
        if (condition != null) {
            this.conditions.add(condition);
        }
        return this;
    }
    
    /**
     * Add multiple conditions to this rule
     * 
     * @param conditions List of delete conditions to add
     * @return This DeleteRule instance for method chaining
     */
    public DeleteRule addConditions(List<DeleteCondition> conditions) {
        if (conditions != null) {
            this.conditions.addAll(conditions);
        }
        return this;
    }
    
    /**
     * Set the maximum number of records this rule can affect
     * 
     * @param maxAffectedRecords Maximum affected records
     * @return This DeleteRule instance for method chaining
     */
    public DeleteRule withMaxAffectedRecords(int maxAffectedRecords) {
        this.maxAffectedRecords = maxAffectedRecords;
        return this;
    }
    
    /**
     * Set whether this rule requires conditions
     * 
     * @param requireConditions Whether conditions are required
     * @return This DeleteRule instance for method chaining
     */
    public DeleteRule withRequireConditions(boolean requireConditions) {
        this.requireConditions = requireConditions;
        return this;
    }
    
    /**
     * Set the description for this rule
     * 
     * @param description Rule description
     * @return This DeleteRule instance for method chaining
     */
    public DeleteRule withDescription(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * Check if this rule has conditions
     * 
     * @return true if rule has conditions
     */
    public boolean hasConditions() {
        return conditions != null && !conditions.isEmpty();
    }
    
    /**
     * Get the number of conditions in this rule
     * 
     * @return Number of conditions
     */
    public int getConditionCount() {
        return conditions != null ? conditions.size() : 0;
    }
    
    /**
     * Validate this delete rule
     * 
     * @return Error message if invalid, null if valid
     */
    public String validate() {
        // Check if conditions are required but missing
        if (requireConditions && !hasConditions()) {
            return "Delete rule requires at least one condition for safety";
        }
        
        // Validate maximum affected records
        if (maxAffectedRecords <= 0) {
            return "Maximum affected records must be positive";
        }
        
        if (maxAffectedRecords > 50000) {
            return "Maximum affected records cannot exceed 50000 for safety";
        }
        
        // Validate each condition
        if (hasConditions()) {
            for (int i = 0; i < conditions.size(); i++) {
                DeleteCondition condition = conditions.get(i);
                String conditionError = condition.validate();
                if (conditionError != null) {
                    return "Condition " + (i + 1) + " validation failed: " + conditionError;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get a description of this rule
     * 
     * @return Human-readable description
     */
    public String getDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }
        
        if (hasConditions()) {
            if (conditions.size() == 1) {
                return "Delete where " + conditions.get(0).getDescription();
            } else {
                return "Delete with " + conditions.size() + " conditions";
            }
        } else {
            return "Delete all records (no conditions)";
        }
    }
    
    /**
     * Create a rule that deletes all records (no conditions)
     * WARNING: This is dangerous and should be used with caution
     * 
     * @param maxAffectedRecords Maximum records to delete
     * @return DeleteRule instance with no conditions
     */
    public static DeleteRule deleteAll(int maxAffectedRecords) {
        DeleteRule rule = new DeleteRule();
        rule.setRequireConditions(false);
        rule.setMaxAffectedRecords(maxAffectedRecords);
        rule.setDescription("Delete all records (no conditions)");
        return rule;
    }
    
    /**
     * Create a rule that deletes records by ID
     * 
     * @param idField ID field name
     * @param id ID value
     * @return DeleteRule instance
     */
    public static DeleteRule deleteById(String idField, Object id) {
        DeleteRule rule = new DeleteRule();
        rule.addCondition(DeleteCondition.equals(idField, id));
        rule.setDescription("Delete by " + idField + " = " + id);
        return rule;
    }
    
    /**
     * Create a rule that deletes records by multiple IDs
     * 
     * @param idField ID field name
     * @param ids List of ID values
     * @return DeleteRule instance
     */
    public static DeleteRule deleteByIds(String idField, List<Object> ids) {
        DeleteRule rule = new DeleteRule();
        rule.addCondition(DeleteCondition.in(idField, ids));
        rule.setDescription("Delete by " + idField + " IN (" + ids.size() + " values)");
        return rule;
    }
    
    /**
     * Create a rule that deletes records by status
     * 
     * @param statusField Status field name
     * @param status Status value
     * @return DeleteRule instance
     */
    public static DeleteRule deleteByStatus(String statusField, Object status) {
        DeleteRule rule = new DeleteRule();
        rule.addCondition(DeleteCondition.equals(statusField, status));
        rule.setDescription("Delete by " + statusField + " = " + status);
        return rule;
    }
}