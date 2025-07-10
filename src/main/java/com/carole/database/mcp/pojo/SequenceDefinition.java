package com.carole.database.mcp.pojo;

import lombok.Data;

/**
 * Sequence definition for generating sequential values
 * 
 * This class defines how to generate sequential values for specific columns, supporting different data types and
 * patterns.
 * 
 * @author CaroLe
 * @Date 2025/7/9
 * @Description Sequence definition for sequential value generation
 */
@Data
public class SequenceDefinition {

    /**
     * Sequence type
     */
    private SequenceType type;

    /**
     * Start value for numeric sequences
     */
    private long startValue = 1;

    /**
     * Step value for increment sequences
     */
    private long step = 1;

    /**
     * Custom values for custom sequences
     */
    private Object[] customValues;

    /**
     * Pattern for pattern-based sequences
     */
    private String pattern;

    /**
     * Whether to cycle through values
     */
    private boolean cycle = false;

    /**
     * Sequence type enum
     */
    public enum SequenceType {
        /**
         * Increment sequence: 1, 2, 3, ...
         */
        INCREMENT,
        /**
         * Custom values sequence
         */
        CUSTOM_VALUES,
        /**
         * Pattern-based sequence
         */
        PATTERN
    }

}