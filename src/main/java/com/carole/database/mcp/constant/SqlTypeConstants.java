package com.carole.database.mcp.constant;

/**
 * SQL standard constants for data type generation
 * 
 * This class contains only SQL standard constants that are defined by the SQL specification, not application-specific
 * values. All dynamic values should be generated using DataFaker or retrieved from the actual database schema.
 * 
 * @author CaroLe
 * @Date 2025/7/7
 * @Description SQL standard constants for data type ranges and limits
 */
public class SqlTypeConstants {

    /**
     * SQL standard numeric type ranges
     */
    public static final int TINYINT_MIN = 0;
    public static final int TINYINT_MAX = 127;
    public static final int SMALLINT_MIN = 0;
    public static final int SMALLINT_MAX = 32767;
    public static final int MEDIUMINT_MIN = 0;
    public static final int MEDIUMINT_MAX = 8388607;
    public static final int INT_MIN = 0;
    public static final int INT_MAX = 2147483647;

    /**
     * SQL standard time ranges
     */
    public static final int MIN_HOUR = 0;
    public static final int MAX_HOUR = 23;
    public static final int MIN_MINUTE = 0;
    public static final int MAX_MINUTE = 59;
    public static final int MIN_SECOND = 0;
    public static final int MAX_SECOND = 59;

    /**
     * SQL standard year range
     */
    public static final int MIN_YEAR = 1000;
    public static final int MAX_YEAR = 9999;

    /**
     * Default precision and scale for DECIMAL types
     */
    public static final int DEFAULT_DECIMAL_PRECISION = 10;
    public static final int DEFAULT_DECIMAL_SCALE = 2;

    /**
     * SQL standard string length limits
     */
    public static final int MAX_VARCHAR_LENGTH = 65535;
    public static final int MAX_CHAR_LENGTH = 255;

    /**
     * JSON generation configuration
     */
    public static final int MIN_JSON_FIELDS = 2;
    public static final int MAX_JSON_FIELDS = 6;
    public static final int MAX_JSON_NESTING_DEPTH = 2;

    /**
     * Probability settings for null and default values
     */
    public static final int NULL_PROBABILITY_PERCENTAGE = 5;
    public static final int DEFAULT_VALUE_PROBABILITY_PERCENTAGE = 10;

    /**
     * Character sets for string generation
     */
    public static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    public static final String NUMERIC_CHARS = "0123456789";
    public static final String ALPHA_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    // Private constructor to prevent instantiation
    private SqlTypeConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}