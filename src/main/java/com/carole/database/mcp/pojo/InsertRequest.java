package com.carole.database.mcp.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Enhanced insert request data structure for flexible data insertion
 * 
 * This class supports complex data insertion scenarios with multiple groups, sequences, and conditional generation. It
 * allows creating different groups of records with different configurations.
 * 
 * @author CaroLe
 * @Date 2025/7/9
 * @Description Enhanced insert request supporting flexible data generation with groups and sequences
 */
@Data
public class InsertRequest {

    /**
     * Target table name for data insertion
     */
    private String tableName;

    /**
     * Number of records to insert (default: 1)
     */
    private int recordCount = 1;

    /**
     * Fixed values to apply to specific columns Key: column name, Value: fixed value
     */
    private Map<String, Object> fixedValues = new HashMap<>();

    /**
     * Conditions parsed from natural language Key: column name, Value: condition value
     */
    private Map<String, Object> conditions = new HashMap<>();

    /**
     * Whether to generate realistic test data or simple sequential data
     */
    private boolean generateRealistic = true;

    /**
     * List of data generation groups for complex insertion scenarios
     */
    private List<DataGroup> dataGroups = new ArrayList<>();

    /**
     * Sequence definitions for generating sequential values Key: column name, Value: sequence definition
     */
    private Map<String, SequenceDefinition> sequences = new HashMap<>();

    /**
     * Add a fixed value for a specific column
     * 
     * @param columnName Column name
     * @param value Fixed value
     */
    public void addFixedValue(String columnName, Object value) {
        this.fixedValues.put(columnName, value);
    }

    /**
     * Add a condition for a specific column
     * 
     * @param columnName Column name
     * @param value Condition value
     */
    public void addCondition(String columnName, Object value) {
        this.conditions.put(columnName, value);
    }

    /**
     * Add a data group for complex insertion scenarios
     * 
     * @param group Data group
     */
    public void addDataGroup(DataGroup group) {
        this.dataGroups.add(group);
    }

    /**
     * Add a sequence definition for a column
     * 
     * @param columnName Column name
     * @param sequence Sequence definition
     */
    public void addSequence(String columnName, SequenceDefinition sequence) {
        this.sequences.put(columnName, sequence);
    }

    /**
     * Check if there are any fixed values or conditions
     * 
     * @return true if there are constraints
     */
    public boolean hasConstraints() {
        return !fixedValues.isEmpty() || !conditions.isEmpty();
    }

    /**
     * Check if there are data groups
     * 
     * @return true if there are data groups
     */
    public boolean hasDataGroups() {
        return !dataGroups.isEmpty();
    }

    /**
     * Check if there are sequences
     * 
     * @return true if there are sequences
     */
    public boolean hasSequences() {
        return !sequences.isEmpty();
    }

    /**
     * Get total record count including all groups
     * 
     * @return Total record count
     */
    public int getTotalRecordCount() {
        if (hasDataGroups()) {
            return dataGroups.stream().mapToInt(DataGroup::getRecordCount).sum();
        }
        return recordCount;
    }

}