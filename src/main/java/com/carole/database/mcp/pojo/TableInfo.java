package com.carole.database.mcp.pojo;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Entity class representing database table column information
 * 
 * This class contains detailed metadata about database table columns including data types, constraints, indexes, and
 * other column-specific properties. Used for describing table structure in database operations.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Data structure for holding comprehensive table column metadata and properties
 */
@Data
public class TableInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 8358808580361601647L;

    /**
     * Column name
     */
    private String name;

    /**
     * Data type of the column
     */
    private String dataType;

    /**
     * Ordinal position of column in table
     */
    private int ordinalPosition;

    /**
     * Maximum column size/length
     */
    private Integer columnSize;

    /**
     * Number of decimal digits for numeric types
     */
    private Integer decimalDigits;

    /**
     * Whether column allows NULL values
     */
    private boolean nullable;

    /**
     * Default value for the column
     */
    private String defaultValue;

    /**
     * Column comment/remarks
     */
    private String columnRemarks;

    /**
     * Whether column is auto-increment
     */
    private boolean autoIncrement;

    /**
     * Whether column is generated (computed)
     */
    private boolean generated;

    /**
     * Whether column is part of primary key
     */
    private boolean primaryKey;

    /**
     * List of indexes that include this column
     */
    private List<String> indexes = new ArrayList<>();

}
