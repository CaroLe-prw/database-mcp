package com.carole.database.mcp.pojo;

import java.util.List;

import lombok.Data;

/**
 * Update condition class for structured database updates
 * 
 * This class represents a condition for database updates, supporting various operators like equals, not equals, in,
 * like, greater than, less than, etc.
 * 
 * @author CaroLe
 * @Date 2025/07/10
 * @Description Update condition for structured database updates with operator support
 */
@Data
public class UpdateCondition {

    /**
     * Field name to apply condition on
     */
    private String field;

    /**
     * Operator type for the condition Supported operators: =, !=, >, <, >=, <=, IN, NOT IN, LIKE, NOT LIKE
     */
    private String operator;

    /**
     * Single value for simple conditions (=, !=, >, <, >=, <=, LIKE, NOT LIKE)
     */
    private Object value;

    /**
     * Multiple values for IN/NOT IN conditions
     */
    private List<Object> values;

    /**
     * Logical connector for combining with other conditions (AND, OR) Default is AND
     */
    private String connector = "AND";

    /**
     * Create an equals condition
     * 
     * @param field Field name
     * @param value Value to compare
     * @return UpdateCondition with equals operator
     */
    public static UpdateCondition equals(String field, Object value) {
        UpdateCondition condition = new UpdateCondition();
        condition.setField(field);
        condition.setOperator("=");
        condition.setValue(value);
        return condition;
    }

    /**
     * Create a not equals condition
     * 
     * @param field Field name
     * @param value Value to compare
     * @return UpdateCondition with not equals operator
     */
    public static UpdateCondition notEquals(String field, Object value) {
        UpdateCondition condition = new UpdateCondition();
        condition.setField(field);
        condition.setOperator("!=");
        condition.setValue(value);
        return condition;
    }

    /**
     * Create an IN condition
     * 
     * @param field Field name
     * @param values List of values
     * @return UpdateCondition with IN operator
     */
    public static UpdateCondition in(String field, List<Object> values) {
        UpdateCondition condition = new UpdateCondition();
        condition.setField(field);
        condition.setOperator("IN");
        condition.setValues(values);
        return condition;
    }

    /**
     * Create a LIKE condition
     * 
     * @param field Field name
     * @param pattern Pattern to match
     * @return UpdateCondition with LIKE operator
     */
    public static UpdateCondition like(String field, String pattern) {
        UpdateCondition condition = new UpdateCondition();
        condition.setField(field);
        condition.setOperator("LIKE");
        condition.setValue(pattern);
        return condition;
    }

    /**
     * Create a greater than condition
     * 
     * @param field Field name
     * @param value Value to compare
     * @return UpdateCondition with greater than operator
     */
    public static UpdateCondition greaterThan(String field, Object value) {
        UpdateCondition condition = new UpdateCondition();
        condition.setField(field);
        condition.setOperator(">");
        condition.setValue(value);
        return condition;
    }

    /**
     * Create a less than condition
     * 
     * @param field Field name
     * @param value Value to compare
     * @return UpdateCondition with less than operator
     */
    public static UpdateCondition lessThan(String field, Object value) {
        UpdateCondition condition = new UpdateCondition();
        condition.setField(field);
        condition.setOperator("<");
        condition.setValue(value);
        return condition;
    }

    /**
     * Check if this condition uses multiple values (IN/NOT IN)
     * 
     * @return true if condition uses multiple values
     */
    public boolean hasMultipleValues() {
        return values != null && !values.isEmpty();
    }

    /**
     * Check if this condition has a single value
     * 
     * @return true if condition has a single value
     */
    public boolean hasSingleValue() {
        return value != null;
    }

    /**
     * Get the appropriate value for SQL generation
     * 
     * @return Single value or first value from list
     */
    public Object getEffectiveValue() {
        if (hasSingleValue()) {
            return value;
        }
        if (hasMultipleValues()) {
            return values.get(0);
        }
        return null;
    }

    /**
     * Validate the condition
     * 
     * @return Error message if invalid, null if valid
     */
    public String validate() {
        if (field == null || field.trim().isEmpty()) {
            return "Field name cannot be null or empty";
        }

        if (operator == null || operator.trim().isEmpty()) {
            return "Operator cannot be null or empty";
        }

        String upperOperator = operator.toUpperCase().trim();
        if (upperOperator.equals("IN") || upperOperator.equals("NOT IN")) {
            if (values == null || values.isEmpty()) {
                return "IN/NOT IN operators require non-empty values list";
            }
        } else {
            if (value == null) {
                return "Operator " + operator + " requires a single value";
            }
        }

        return null;
    }
}