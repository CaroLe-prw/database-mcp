package com.carole.database.mcp.util;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import com.carole.database.mcp.constant.SqlTypeConstants;
import com.carole.database.mcp.pojo.DataGroup;
import com.carole.database.mcp.pojo.InsertRequest;
import com.carole.database.mcp.pojo.SequenceDefinition;
import com.carole.database.mcp.pojo.TableInfo;
import com.carole.database.mcp.strategy.DatabaseStrategy;

import net.datafaker.Faker;

/**
 * Dynamic data generator using DataFaker with database schema integration
 *
 * This generator creates realistic test data by analyzing actual database column definitions and extracting ENUM/SET
 * values directly from the database schema. All data generation is based on SQL data types and DataFaker library.
 *
 * @author CaroLe
 * @Date 2025/7/7
 * @Description Dynamic data generator that reads database schema for accurate data generation
 */
public class DataGenerator {

    private static final Faker FAKER = new Faker(Locale.CHINA);
    private static final Random RANDOM = new Random();

    // Snowflake ID generator for VARCHAR ID fields
    private static final SnowflakeIdGenerator SNOWFLAKE_GENERATOR = new SnowflakeIdGenerator();

    // Patterns for parsing ENUM and SET definitions
    private static final Pattern ENUM_PATTERN = Pattern.compile("enum\\((.+?)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_PATTERN = Pattern.compile("set\\((.+?)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUES_PATTERN = Pattern.compile("'([^']*)'");

    /**
     * Generate test data for batch insertion
     *
     * @param tableInfos Table structure information
     * @param request Insert request with constraints
     * @param dataSource Database data source for schema queries
     * @param databaseStrategy Database strategy for schema operations
     * @return List of generated data maps
     */
    public static List<Map<String, Object>> generateTestData(List<TableInfo> tableInfos, InsertRequest request,
        DataSource dataSource, DatabaseStrategy databaseStrategy) {

        List<Map<String, Object>> dataList = new ArrayList<>();

        // If there are data groups, generate data for each group
        if (request.hasDataGroups()) {
            // Initialize global sequence trackers for all groups
            Map<String, SequenceTracker> sequenceTrackers = new HashMap<>();
            for (Map.Entry<String, SequenceDefinition> entry : request.getSequences().entrySet()) {
                sequenceTrackers.put(entry.getKey(), new SequenceTracker(entry.getValue()));
            }

            int globalRecordIndex = 0;
            for (DataGroup group : request.getDataGroups()) {
                List<Map<String, Object>> groupData = generateDataForGroup(tableInfos, group, request, dataSource,
                    databaseStrategy, sequenceTrackers, globalRecordIndex);
                dataList.addAll(groupData);
                globalRecordIndex += group.getRecordCount();
            }
        } else {
            // Generate data normally
            dataList = generateDataForSingleRequest(tableInfos, request, dataSource, databaseStrategy);
        }

        return dataList;
    }

    /**
     * Generate data for a single group with sequence support
     *
     * @param tableInfos Table structure information
     * @param group Data group
     * @param request Insert request
     * @param dataSource Database data source
     * @param databaseStrategy Database strategy
     * @param sequenceTrackers Global sequence trackers
     * @param globalRecordIndex Global record index for sequence tracking
     * @return List of generated data maps
     */
    private static List<Map<String, Object>> generateDataForGroup(List<TableInfo> tableInfos, DataGroup group,
        InsertRequest request, DataSource dataSource, DatabaseStrategy databaseStrategy,
        Map<String, SequenceTracker> sequenceTrackers, int globalRecordIndex) {

        List<Map<String, Object>> groupData = new ArrayList<>();

        for (int i = 0; i < group.getRecordCount(); i++) {
            Map<String, Object> rowData = new HashMap<>();

            for (TableInfo tableInfo : tableInfos) {
                String columnName = tableInfo.getName();

                // Skip auto-increment columns
                if (tableInfo.isAutoIncrement()) {
                    continue;
                }

                // Use sequence-aware value generation
                Object value =
                    generateValueForColumnWithSequence(tableInfo, group.getFixedValues(), group.getConditions(),
                        request, sequenceTrackers, globalRecordIndex + i, dataSource, databaseStrategy);

                rowData.put(columnName, value);
            }

            groupData.add(rowData);
        }

        return groupData;
    }

    /**
     * Generate data for single request (without groups)
     *
     * @param tableInfos Table structure information
     * @param request Insert request
     * @param dataSource Database data source
     * @param databaseStrategy Database strategy
     * @return List of generated data maps
     */
    private static List<Map<String, Object>> generateDataForSingleRequest(List<TableInfo> tableInfos,
        InsertRequest request, DataSource dataSource, DatabaseStrategy databaseStrategy) {

        List<Map<String, Object>> dataList = new ArrayList<>();

        // Initialize sequence trackers
        Map<String, SequenceTracker> sequenceTrackers = new HashMap<>();
        for (Map.Entry<String, SequenceDefinition> entry : request.getSequences().entrySet()) {
            sequenceTrackers.put(entry.getKey(), new SequenceTracker(entry.getValue()));
        }

        for (int i = 0; i < request.getRecordCount(); i++) {
            Map<String, Object> rowData = new HashMap<>();

            for (TableInfo tableInfo : tableInfos) {
                String columnName = tableInfo.getName();

                // Skip auto-increment columns
                if (tableInfo.isAutoIncrement()) {
                    continue;
                }

                Object value = generateValueForColumnWithSequence(tableInfo, request.getFixedValues(),
                    request.getConditions(), request, sequenceTrackers, i, dataSource, databaseStrategy);

                rowData.put(columnName, value);
            }

            dataList.add(rowData);
        }

        return dataList;
    }

    /**
     * Generate value for a column with sequence support
     *
     * @param tableInfo Table info
     * @param fixedValues Fixed values map
     * @param conditions Conditions map
     * @param request Insert request
     * @param sequenceTrackers Sequence trackers
     * @param recordIndex Record index
     * @param dataSource Database data source
     * @param databaseStrategy Database strategy
     * @return Generated value
     */
    private static Object generateValueForColumnWithSequence(TableInfo tableInfo, Map<String, Object> fixedValues,
        Map<String, Object> conditions, InsertRequest request, Map<String, SequenceTracker> sequenceTrackers,
        int recordIndex, DataSource dataSource, DatabaseStrategy databaseStrategy) {

        String columnName = tableInfo.getName();

        // Check for sequence values first
        if (request.hasSequences() && request.getSequences().containsKey(columnName)) {
            SequenceTracker tracker = sequenceTrackers.get(columnName);
            return tracker.getNextValue();
        }

        return generateValueForColumn(tableInfo, fixedValues, conditions, request, dataSource, databaseStrategy);
    }

    /**
     * Generate value for a column
     *
     * @param tableInfo Table info
     * @param fixedValues Fixed values map
     * @param conditions Conditions map
     * @param request Insert request
     * @param dataSource Database data source
     * @param databaseStrategy Database strategy
     * @return Generated value
     */
    private static Object generateValueForColumn(TableInfo tableInfo, Map<String, Object> fixedValues,
        Map<String, Object> conditions, InsertRequest request, DataSource dataSource,
        DatabaseStrategy databaseStrategy) {

        String columnName = tableInfo.getName();

        // Check for fixed values first
        if (fixedValues.containsKey(columnName)) {
            return fixedValues.get(columnName);
        } else if (conditions.containsKey(columnName)) {
            return conditions.get(columnName);
        } else {
            // Special handling for ID fields
            if (isIdField(tableInfo.getName()) && !tableInfo.isAutoIncrement()) {
                return generateIdValue(tableInfo);
            } else {
                // Generate value based on actual database column definition
                return generateValueByDatabaseType(tableInfo, dataSource, databaseStrategy, request.getTableName());
            }
        }
    }

    /**
     * Generate value based on actual database column definition
     *
     * @param tableInfo Column information
     * @param dataSource Database data source
     * @param databaseStrategy Database strategy
     * @param tableName Table name
     * @return Generated value
     */
    private static Object generateValueByDatabaseType(TableInfo tableInfo, DataSource dataSource,
        DatabaseStrategy databaseStrategy, String tableName) {

        String dataType = tableInfo.getDataType().toUpperCase();

        // Handle null values based on nullable constraint
        if (tableInfo.isNullable() && RANDOM.nextInt(100) < SqlTypeConstants.NULL_PROBABILITY_PERCENTAGE) {
            return null;
        }

        // Handle default values
        if (tableInfo.getDefaultValue() != null && !tableInfo.getDefaultValue().isEmpty()) {
            String defaultValue = tableInfo.getDefaultValue();
            if (!"NULL".equalsIgnoreCase(defaultValue)
                && RANDOM.nextInt(100) < SqlTypeConstants.DEFAULT_VALUE_PROBABILITY_PERCENTAGE) {
                return parseDefaultValue(defaultValue, dataType);
            }
        }

        // For ENUM and SET types, get actual values from database
        if (dataType.startsWith("ENUM") || dataType.startsWith("SET")) {
            return generateEnumOrSetValue(tableInfo, dataSource, databaseStrategy, tableName);
        }

        // Generate value based on SQL data type using DataFaker
        return generateValueByStandardSqlType(tableInfo);
    }

    /**
     * Generate ENUM or SET value from actual database definition
     */
    private static Object generateEnumOrSetValue(TableInfo tableInfo, DataSource dataSource,
        DatabaseStrategy databaseStrategy, String tableName) {

        try {
            // Get actual column definition from database
            String columnDef = databaseStrategy.getColumnDefinition(dataSource, tableName, tableInfo.getName());

            // Extract the type part (e.g., "enum('A','B','C')" or "set('read','write','execute')")
            String typeInfo = extractTypeFromColumnDefinition(columnDef);

            if (typeInfo.toLowerCase().startsWith("enum")) {
                return generateEnumValue(typeInfo);
            } else if (typeInfo.toLowerCase().startsWith("set")) {
                return generateSetValue(typeInfo);
            }
        } catch (Exception e) {
            return FAKER.lorem().word();
        }

        return FAKER.lorem().word();
    }

    /**
     * Extract type information from column definition string
     */
    private static String extractTypeFromColumnDefinition(String columnDef) {
        // Column definition format: "Column: column_name, Type: enum('A','B','C'), ..."
        int typeIndex = columnDef.indexOf("Type: ");
        if (typeIndex != -1) {
            String remaining = columnDef.substring(typeIndex + 6);

            // For ENUM/SET types, need to find the complete definition including parentheses
            if (remaining.toLowerCase().startsWith("enum(") || remaining.toLowerCase().startsWith("set(")) {
                int openParen = remaining.indexOf('(');
                int closeParen = remaining.indexOf(')', openParen);
                if (openParen != -1 && closeParen != -1) {
                    return remaining.substring(0, closeParen + 1);
                }
            }

            // For other types, take until next comma or end
            int nextComma = remaining.indexOf(", ");
            if (nextComma != -1) {
                return remaining.substring(0, nextComma).trim();
            } else {
                return remaining.trim();
            }
        }
        return "";
    }

    /**
     * Generate ENUM value from actual database definition
     */
    private static String generateEnumValue(String enumDef) {
        Matcher matcher = ENUM_PATTERN.matcher(enumDef);
        if (matcher.find()) {
            String valuesStr = matcher.group(1);
            List<String> values = extractQuotedValues(valuesStr);
            if (!values.isEmpty()) {
                return values.get(RANDOM.nextInt(values.size()));
            }
        }
        return FAKER.lorem().word();
    }

    /**
     * Generate SET value from actual database definition
     */
    private static String generateSetValue(String setDef) {
        Matcher matcher = SET_PATTERN.matcher(setDef);
        if (matcher.find()) {
            String valuesStr = matcher.group(1);
            List<String> values = extractQuotedValues(valuesStr);
            if (!values.isEmpty()) {
                // For SET, select 1-3 random values
                int count = RANDOM.nextInt(Math.min(3, values.size())) + 1;
                Set<String> selectedValues = new HashSet<>();

                for (int i = 0; i < count; i++) {
                    selectedValues.add(values.get(RANDOM.nextInt(values.size())));
                }

                return String.join(",", selectedValues);
            }
        }
        return FAKER.lorem().word();
    }

    /**
     * Extract quoted values from ENUM/SET definition
     */
    private static List<String> extractQuotedValues(String valuesStr) {
        List<String> values = new ArrayList<>();
        Matcher matcher = VALUES_PATTERN.matcher(valuesStr);

        while (matcher.find()) {
            values.add(matcher.group(1));
        }

        return values;
    }

    /**
     * Generate value based on standard SQL data type using DataFaker
     */
    private static Object generateValueByStandardSqlType(TableInfo tableInfo) {
        String dataType = tableInfo.getDataType().toUpperCase();

        return switch (dataType) {
            case "TINYINT" -> FAKER.number().numberBetween(SqlTypeConstants.TINYINT_MIN, SqlTypeConstants.TINYINT_MAX);
            case "SMALLINT" ->
                FAKER.number().numberBetween(SqlTypeConstants.SMALLINT_MIN, SqlTypeConstants.SMALLINT_MAX);
            case "MEDIUMINT" ->
                FAKER.number().numberBetween(SqlTypeConstants.MEDIUMINT_MIN, SqlTypeConstants.MEDIUMINT_MAX);
            case "INT", "INTEGER" -> FAKER.number().numberBetween(SqlTypeConstants.INT_MIN, SqlTypeConstants.INT_MAX);
            case "BIGINT" -> FAKER.number().numberBetween(0, Long.MAX_VALUE);
            case "DECIMAL", "NUMERIC" -> generateNonNegativeDecimal(tableInfo.getColumnSize(),
                tableInfo.getDecimalDigits(), tableInfo.getName());
            case "FLOAT" -> (float)(FAKER.number().randomDouble(6, 0, 1000000));
            case "DOUBLE" -> FAKER.number().randomDouble(15, 0, 1000000);
            case "CHAR" -> generateChar(tableInfo.getColumnSize());
            case "VARCHAR" -> generateVarchar(tableInfo.getColumnSize());
            case "TEXT" -> FAKER.lorem().paragraph();
            case "MEDIUMTEXT" -> FAKER.lorem().paragraph(FAKER.number().numberBetween(5, 15));
            case "LONGTEXT" -> FAKER.lorem().paragraph(FAKER.number().numberBetween(15, 30));
            case "DATE" -> {
                Instant pastInstant = FAKER.timeAndDate().past(3650, java.util.concurrent.TimeUnit.DAYS);
                yield pastInstant.toString().substring(0, 10);
            }
            case "TIME" -> String.format("%02d:%02d:%02d",
                FAKER.number().numberBetween(SqlTypeConstants.MIN_HOUR, SqlTypeConstants.MAX_HOUR + 1),
                FAKER.number().numberBetween(SqlTypeConstants.MIN_MINUTE, SqlTypeConstants.MAX_MINUTE + 1),
                FAKER.number().numberBetween(SqlTypeConstants.MIN_SECOND, SqlTypeConstants.MAX_SECOND + 1));
            case "DATETIME" -> {
                Instant datetimeInstant = FAKER.timeAndDate().past(3650, java.util.concurrent.TimeUnit.DAYS);
                yield datetimeInstant.toString().replace("T", " ").substring(0, 19);
            }
            case "TIMESTAMP" -> {
                Instant timestampInstant = FAKER.timeAndDate().past(3650, java.util.concurrent.TimeUnit.DAYS);
                yield Timestamp.from(timestampInstant);
            }
            case "YEAR" -> FAKER.number().numberBetween(SqlTypeConstants.MIN_YEAR, SqlTypeConstants.MAX_YEAR);
            case "BOOLEAN", "TINYINT(1)" -> FAKER.bool().bool();
            case "BINARY", "VARBINARY" -> generateBinary(tableInfo.getColumnSize());
            case "BLOB" -> generateBinary(FAKER.number().numberBetween(1, 1000));
            case "JSON" -> generateDynamicJson();
            default ->
                // For unknown types, generate as varchar
                generateVarchar(Math.min(tableInfo.getColumnSize(), 255));
        };
    }

    /**
     * Generate non-negative decimal value with precision and scale
     */
    private static BigDecimal generateNonNegativeDecimal(int precision, int scale, String columnName) {
        if (precision <= 0) {
            precision = SqlTypeConstants.DEFAULT_DECIMAL_PRECISION;
        }
        if (scale < 0) {
            scale = SqlTypeConstants.DEFAULT_DECIMAL_SCALE;
        }

        double maxValue = Math.pow(10, precision - scale) - 1;
        double value = FAKER.number().randomDouble(scale, 0, (int)maxValue);
        return BigDecimal.valueOf(value);
    }

    /**
     * Generate fixed-length character string
     */
    private static String generateChar(int length) {
        if (length <= 0) {
            length = 1;
        }
        return FAKER.lorem().characters(length, length);
    }

    /**
     * Generate variable-length character string
     */
    private static String generateVarchar(int maxLength) {
        if (maxLength <= 0) {
            maxLength = 255;
        }
        int length = FAKER.number().numberBetween(1, Math.min(maxLength, 50));
        return FAKER.lorem().characters(length);
    }

    /**
     * Generate binary data
     */
    private static byte[] generateBinary(int length) {
        if (length <= 0) {
            length = 1;
        }
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Generate completely dynamic JSON without predefined fields
     */
    private static String generateDynamicJson() {
        Map<String, Object> jsonData = new HashMap<>();

        // Random number of fields
        int fieldCount =
            FAKER.number().numberBetween(SqlTypeConstants.MIN_JSON_FIELDS, SqlTypeConstants.MAX_JSON_FIELDS + 1);

        for (int i = 0; i < fieldCount; i++) {
            String key = FAKER.lorem().word();
            Object value = generateDynamicJsonValue(0);
            jsonData.put(key, value);
        }

        return convertMapToJsonString(jsonData);
    }

    /**
     * Generate dynamic JSON value with depth control
     */
    private static Object generateDynamicJsonValue(int currentDepth) {
        // Limit nesting depth
        if (currentDepth >= SqlTypeConstants.MAX_JSON_NESTING_DEPTH) {
            return generateSimpleJsonValue();
        }

        int type = FAKER.number().numberBetween(1, 8);

        return switch (type) {
            // String
            case 1 -> FAKER.lorem().word();
            // Integer
            case 2 -> FAKER.number().numberBetween(1, 1000);
            // Boolean
            case 3 -> FAKER.bool().bool();
            // Double
            case 4 -> FAKER.number().randomDouble(2, 1, 1000);
            // Null
            case 5 -> null;
            // Array
            case 6 -> generateDynamicJsonArray(currentDepth + 1);
            // Nested object
            case 7 -> generateDynamicJsonObject(currentDepth + 1);
            default -> FAKER.lorem().word();
        };
    }

    /**
     * Generate dynamic JSON array
     */
    private static List<Object> generateDynamicJsonArray(int currentDepth) {
        List<Object> array = new ArrayList<>();
        int size = FAKER.number().numberBetween(1, 4);

        for (int i = 0; i < size; i++) {
            array.add(generateDynamicJsonValue(currentDepth));
        }

        return array;
    }

    /**
     * Generate dynamic nested JSON object
     */
    private static Map<String, Object> generateDynamicJsonObject(int currentDepth) {
        Map<String, Object> nestedObject = new HashMap<>();
        int fieldCount = FAKER.number().numberBetween(1, 3);

        for (int i = 0; i < fieldCount; i++) {
            String key = FAKER.lorem().word();
            Object value = generateDynamicJsonValue(currentDepth);
            nestedObject.put(key, value);
        }

        return nestedObject;
    }

    /**
     * Generate simple JSON value (no nesting)
     */
    private static Object generateSimpleJsonValue() {
        int type = FAKER.number().numberBetween(1, 5);

        return switch (type) {
            case 1 -> FAKER.lorem().word();
            case 2 -> FAKER.number().numberBetween(1, 100);
            case 3 -> FAKER.bool().bool();
            case 4 -> FAKER.number().randomDouble(2, 1, 100);
            default -> null;
        };
    }

    /**
     * Convert Map to JSON string
     */
    private static String convertMapToJsonString(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;

            json.append("\"").append(entry.getKey()).append("\":");
            json.append(convertValueToJsonString(entry.getValue()));
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Convert value to JSON string representation
     */
    @SuppressWarnings("unchecked")
    private static String convertValueToJsonString(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof List<?> list) {
            StringBuilder array = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    array.append(",");
                }
                array.append(convertValueToJsonString(list.get(i)));
            }
            array.append("]");
            return array.toString();
        } else if (value instanceof Map<?, ?> map) {
            return convertMapToJsonString((Map<String, Object>)map);
        } else {
            return "\"" + value.toString() + "\"";
        }
    }

    /**
     * Parse default value based on data type
     */
    private static Object parseDefaultValue(String defaultValue, String dataType) {
        try {
            // Handle MySQL functions first
            if ("CURRENT_TIMESTAMP".equalsIgnoreCase(defaultValue) || "NOW()".equalsIgnoreCase(defaultValue)) {
                return new Timestamp(System.currentTimeMillis());
            }

            if ("CURRENT_DATE".equalsIgnoreCase(defaultValue) || "CURDATE()".equalsIgnoreCase(defaultValue)) {
                return Date.valueOf(LocalDate.now());
            }

            if ("CURRENT_TIME".equalsIgnoreCase(defaultValue) || "CURTIME()".equalsIgnoreCase(defaultValue)) {
                return Time.valueOf(LocalTime.now());
            }

            // Remove quotes if present
            if (defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
                defaultValue = defaultValue.substring(1, defaultValue.length() - 1);
            }

            return switch (dataType.toUpperCase()) {
                case "TINYINT", "SMALLINT", "MEDIUMINT", "INT", "INTEGER", "YEAR" -> Integer.parseInt(defaultValue);
                case "BIGINT" -> Long.parseLong(defaultValue);
                case "DECIMAL", "NUMERIC" -> new BigDecimal(defaultValue);
                case "FLOAT" -> Float.parseFloat(defaultValue);
                case "DOUBLE" -> Double.parseDouble(defaultValue);
                case "BOOLEAN" -> Boolean.parseBoolean(defaultValue);
                case "DATETIME", "TIMESTAMP" -> {
                    // Try to parse as timestamp string, fallback to current time
                    try {
                        yield Timestamp.valueOf(defaultValue);
                    } catch (Exception ex) {
                        yield new Timestamp(System.currentTimeMillis());
                    }
                }
                case "DATE" -> {
                    try {
                        yield Date.valueOf(defaultValue);
                    } catch (Exception ex) {
                        yield Date.valueOf(LocalDate.now());
                    }
                }
                case "TIME" -> {
                    try {
                        yield Time.valueOf(defaultValue);
                    } catch (Exception ex) {
                        yield Time.valueOf(LocalTime.now());
                    }
                }
                default -> defaultValue;
            };
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Check if field name suggests it's an ID field
     */
    private static boolean isIdField(String columnName) {
        String name = columnName.toLowerCase();
        return name.endsWith("_id") || name.endsWith("id") || "uuid".equals(name) || name.contains("identifier");
    }

    /**
     * Generate appropriate ID value based on column type
     */
    private static Object generateIdValue(TableInfo tableInfo) {
        String dataType = tableInfo.getDataType().toUpperCase();

        // For VARCHAR/CHAR ID fields, generate snowflake ID as string
        if ("VARCHAR".equals(dataType) || "CHAR".equals(dataType)) {
            return SNOWFLAKE_GENERATOR.nextIdAsString();
        }

        // For BIGINT ID fields, generate snowflake ID as number
        if ("BIGINT".equals(dataType)) {
            return SNOWFLAKE_GENERATOR.nextId();
        }

        // For other numeric ID fields, generate reasonable positive numbers
        return switch (dataType) {
            case "TINYINT" -> FAKER.number().numberBetween(1, 127);
            case "SMALLINT" -> FAKER.number().numberBetween(1, 32767);
            case "MEDIUMINT" -> FAKER.number().numberBetween(1, 8388607);
            case "INT", "INTEGER" -> FAKER.number().numberBetween(1, 2147483647);
            default -> FAKER.number().numberBetween(1, 999999);
        };
    }

    /**
     * Sequence tracker for managing sequence generation state
     */
    private static class SequenceTracker {
        private final SequenceDefinition definition;
        private long currentValue;
        private int currentIndex;

        public SequenceTracker(SequenceDefinition definition) {
            this.definition = definition;
            this.currentValue = definition.getStartValue();
            this.currentIndex = 0;
        }

        public Object getNextValue() {
            return switch (definition.getType()) {
                case INCREMENT -> {
                    long value = currentValue;
                    currentValue += definition.getStep();
                    yield value;
                }
                case CUSTOM_VALUES -> {
                    Object[] values = definition.getCustomValues();
                    if (values == null || values.length == 0) {
                        yield null;
                    }
                    Object value = values[currentIndex];
                    currentIndex++;
                    if (currentIndex >= values.length) {
                        currentIndex = definition.isCycle() ? 0 : values.length - 1;
                    }
                    yield value;
                }
                case PATTERN -> {
                    String pattern = definition.getPattern();
                    if (pattern == null) {
                        yield null;
                    }
                    String result = pattern.replace("{seq}", String.valueOf(currentValue));
                    // Handle formatted sequences like {seq:03d}
                    if (result.contains("{seq:")) {
                        int start = result.indexOf("{seq:");
                        int end = result.indexOf("}", start);
                        if (end > start) {
                            String format = result.substring(start + 5, end);
                            String formatted = String.format("%" + format, currentValue);
                            result = result.substring(0, start) + formatted + result.substring(end + 1);
                        }
                    }
                    currentValue++;
                    yield result;
                }
            };
        }
    }
}