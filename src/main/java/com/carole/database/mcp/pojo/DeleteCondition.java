package com.carole.database.mcp.pojo;

import java.util.List;

import lombok.Data;

/**
 * Delete condition for database delete operations
 * 
 * This class represents a single condition in a WHERE clause for DELETE operations. It supports various operators
 * including equality, comparison, and set operations (IN/NOT IN) with both single and multiple values.
 * 
 * @author CaroLe
 * @Date 2025/07/10
 * @Description Delete condition object with support for various operators and value types
 */
@Data
public class DeleteCondition {
    
    /**
     * Field name for the condition (column name)
     */
    private String field;
    
    /**
     * SQL operator (=, !=, >, <, >=, <=, IN, NOT IN, LIKE, NOT LIKE)
     */
    private String operator;
    
    /**
     * Single value for simple conditions
     */
    private Object value;
    
    /**
     * Multiple values for IN/NOT IN operations
     */
    private List<Object> values;
    
    /**
     * Connector for combining conditions (AND, OR)
     * Default is "AND"
     */
    private String connector = "AND";
    
    /**
     * Create a simple equality condition
     * 
     * @param field Field name
     * @param value Value to compare
     * @return DeleteCondition instance
     */
    public static DeleteCondition equals(String field, Object value) {
        DeleteCondition condition = new DeleteCondition();
        condition.setField(field);
        condition.setOperator("=");
        condition.setValue(value);
        return condition;
    }
    
    /**
     * Create a NOT EQUALS condition
     * 
     * @param field Field name
     * @param value Value to compare
     * @return DeleteCondition instance
     */
    public static DeleteCondition notEquals(String field, Object value) {
        DeleteCondition condition = new DeleteCondition();
        condition.setField(field);
        condition.setOperator("!=");
        condition.setValue(value);
        return condition;
    }
    
    /**
     * Create an IN condition with multiple values
     * 
     * @param field Field name
     * @param values List of values
     * @return DeleteCondition instance
     */
    public static DeleteCondition in(String field, List<Object> values) {
        DeleteCondition condition = new DeleteCondition();
        condition.setField(field);
        condition.setOperator("IN");
        condition.setValues(values);
        return condition;
    }
    
    /**
     * Create a NOT IN condition with multiple values
     * 
     * @param field Field name
     * @param values List of values
     * @return DeleteCondition instance
     */
    public static DeleteCondition notIn(String field, List<Object> values) {
        DeleteCondition condition = new DeleteCondition();
        condition.setField(field);
        condition.setOperator("NOT IN");
        condition.setValues(values);
        return condition;
    }
    
    /**
     * Create a LIKE condition for pattern matching
     * 
     * @param field Field name
     * @param pattern Pattern to match
     * @return DeleteCondition instance
     */
    public static DeleteCondition like(String field, String pattern) {
        DeleteCondition condition = new DeleteCondition();
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
     * @return DeleteCondition instance
     */
    public static DeleteCondition greaterThan(String field, Object value) {
        DeleteCondition condition = new DeleteCondition();
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
     * @return DeleteCondition instance
     */
    public static DeleteCondition lessThan(String field, Object value) {
        DeleteCondition condition = new DeleteCondition();
        condition.setField(field);
        condition.setOperator("<");
        condition.setValue(value);
        return condition;
    }
    
    /**
     * Set the connector for this condition
     * 
     * @param connector Connector (AND/OR)
     * @return This DeleteCondition instance for method chaining
     */
    public DeleteCondition withConnector(String connector) {
        this.connector = connector;
        return this;
    }
    
    /**
     * Check if this condition uses multiple values (IN/NOT IN)
     * 
     * @return true if condition has multiple values
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
        return value != null && !hasMultipleValues();
    }
    
    /**
     * Validate this condition
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
        
        // Check if operator requires multiple values
        if ("IN".equals(upperOperator) || "NOT IN".equals(upperOperator)) {
            if (values == null || values.isEmpty()) {
                return "IN/NOT IN operators require multiple values";
            }
        } else {
            // Other operators require single value
            if (value == null && !hasMultipleValues()) {
                return "Operator " + operator + " requires a value";
            }
        }
        
        return null;
    }
    
    /**
     * Get a description of this condition
     * 
     * @return Human-readable description
     */
    public String getDescription() {
        if (hasMultipleValues()) {
            return field + " " + operator + " (" + values.size() + " values)";
        } else {
            return field + " " + operator + " " + value;
        }
    }
}