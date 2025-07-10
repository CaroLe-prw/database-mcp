package com.carole.database.mcp.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Update rule class for structured database updates
 * 
 * This class represents a single update rule that combines conditions and update values. Each rule defines what
 * conditions must be met and what values to update when those conditions are satisfied.
 * 
 * @author CaroLe
 * @Date 2025/07/10
 * @Description Update rule combining conditions and update values for structured database updates
 */
@Data
public class UpdateRule {

    /**
     * List of conditions that must be met for this update rule to apply Multiple conditions are combined using AND/OR
     * logic based on each condition's connector
     */
    private List<UpdateCondition> conditions;

    /**
     * Map of field names to their new values when conditions are met Key: field name, Value: new value to set
     */
    private Map<String, Object> updateValues;

    /**
     * Maximum number of records this rule can affect (safety limit) Default is 1000 to prevent accidental mass updates
     */
    private int maxAffectedRecords = 1000;

    /**
     * Whether to require at least one condition (prevents full table updates) Default is true for safety
     */
    private boolean requireConditions = true;

    /**
     * Default constructor
     */
    public UpdateRule() {
        this.conditions = new ArrayList<>();
        this.updateValues = new HashMap<>();
    }

    /**
     * Constructor with conditions and update values
     * 
     * @param conditions List of conditions
     * @param updateValues Map of field names to new values
     */
    public UpdateRule(List<UpdateCondition> conditions, Map<String, Object> updateValues) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.updateValues = updateValues != null ? updateValues : new HashMap<>();
    }

    /**
     * Add a condition to this update rule
     * 
     * @param condition The condition to add
     * @return This UpdateRule instance for method chaining
     */
    public UpdateRule addCondition(UpdateCondition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    /**
     * Add an update value to this rule
     * 
     * @param field Field name to update
     * @param value New value to set
     * @return This UpdateRule instance for method chaining
     */
    public UpdateRule addUpdateValue(String field, Object value) {
        if (this.updateValues == null) {
            this.updateValues = new HashMap<>();
        }
        this.updateValues.put(field, value);
        return this;
    }

    /**
     * Create a simple update rule with single condition
     * 
     * @param field Condition field name
     * @param conditionValue Condition value
     * @param updateField Update field name
     * @param updateValue Update value
     * @return New UpdateRule instance
     */
    public static UpdateRule simple(String field, Object conditionValue, String updateField, Object updateValue) {
        UpdateRule rule = new UpdateRule();
        rule.addCondition(UpdateCondition.equals(field, conditionValue));
        rule.addUpdateValue(updateField, updateValue);
        return rule;
    }

    /**
     * Create an update rule with multiple conditions (AND logic)
     * 
     * @param conditions Map of field names to condition values
     * @param updateValues Map of field names to update values
     * @return New UpdateRule instance
     */
    public static UpdateRule withConditions(Map<String, Object> conditions, Map<String, Object> updateValues) {
        UpdateRule rule = new UpdateRule();

        if (conditions != null) {
            conditions.forEach((field, value) -> {
                rule.addCondition(UpdateCondition.equals(field, value));
            });
        }

        if (updateValues != null) {
            rule.setUpdateValues(new HashMap<>(updateValues));
        }

        return rule;
    }

    /**
     * Check if this rule has any conditions
     * 
     * @return true if rule has conditions
     */
    public boolean hasConditions() {
        return conditions != null && !conditions.isEmpty();
    }

    /**
     * Check if this rule has any update values
     * 
     * @return true if rule has update values
     */
    public boolean hasUpdateValues() {
        return updateValues != null && !updateValues.isEmpty();
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
     * Get the number of update values in this rule
     * 
     * @return Number of update values
     */
    public int getUpdateValueCount() {
        return updateValues != null ? updateValues.size() : 0;
    }

    /**
     * Validate this update rule
     * 
     * @return Error message if invalid, null if valid
     */
    public String validate() {
        // Check if conditions are required and present
        if (requireConditions && !hasConditions()) {
            return "Update rule requires at least one condition for safety";
        }

        // Check if update values are present
        if (!hasUpdateValues()) {
            return "Update rule must have at least one update value";
        }

        // Validate each condition
        if (conditions != null) {
            for (int i = 0; i < conditions.size(); i++) {
                UpdateCondition condition = conditions.get(i);
                String conditionError = condition.validate();
                if (conditionError != null) {
                    return "Condition " + (i + 1) + " is invalid: " + conditionError;
                }
            }
        }

        // Validate update values
        for (Map.Entry<String, Object> entry : updateValues.entrySet()) {
            String field = entry.getKey();
            if (field == null || field.trim().isEmpty()) {
                return "Update field name cannot be null or empty";
            }

            // Value can be null (for setting NULL values)
            // But we should validate field names for SQL injection
            if (!isValidFieldName(field)) {
                return "Invalid update field name: " + field;
            }
        }

        // Validate max affected records
        if (maxAffectedRecords <= 0) {
            return "Maximum affected records must be positive";
        }

        return null;
    }

    /**
     * Simple field name validation (basic SQL identifier rules)
     * 
     * @param fieldName Field name to validate
     * @return true if valid
     */
    private boolean isValidFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }

        // Basic SQL identifier pattern: starts with letter or underscore, contains only letters, digits, underscores
        return fieldName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    /**
     * Get a description of this update rule for logging/debugging
     * 
     * @return Human-readable description
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Update rule: ");

        if (hasConditions()) {
            desc.append("WHERE ");
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    desc.append(" ").append(conditions.get(i).getConnector()).append(" ");
                }
                UpdateCondition condition = conditions.get(i);
                desc.append(condition.getField()).append(" ").append(condition.getOperator()).append(" ");
                if (condition.hasMultipleValues()) {
                    desc.append(condition.getValues());
                } else {
                    desc.append(condition.getValue());
                }
            }
        } else {
            desc.append("No conditions (affects all records)");
        }

        desc.append(" SET ");
        updateValues.forEach((field, value) -> {
            desc.append(field).append("=").append(value).append(", ");
        });

        // Remove trailing comma and space
        if (desc.toString().endsWith(", ")) {
            desc.setLength(desc.length() - 2);
        }

        return desc.toString();
    }
}