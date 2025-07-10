package com.carole.database.mcp.pojo;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * Data group for grouping records with different conditions
 * 
 * This class represents a group of records that share the same generation rules and fixed values.
 * Multiple groups can be used to create different sets of data with different characteristics.
 * 
 * @author CaroLe
 * @Date 2025/7/9
 * @Description Data group for flexible record generation with shared conditions
 */
@Data
public class DataGroup {
    
    /**
     * Number of records in this group
     */
    private int recordCount;

    /**
     * Fixed values for this group
     */
    private Map<String, Object> fixedValues = new HashMap<>();

    /**
     * Conditions for this group
     */
    private Map<String, Object> conditions = new HashMap<>();

    /**
     * Group description
     */
    private String description;

    /**
     * Add a fixed value for this group
     * 
     * @param columnName Column name
     * @param value Fixed value
     */
    public void addFixedValue(String columnName, Object value) {
        this.fixedValues.put(columnName, value);
    }

    /**
     * Add a condition for this group
     * 
     * @param columnName Column name
     * @param value Condition value
     */
    public void addCondition(String columnName, Object value) {
        this.conditions.put(columnName, value);
    }
}