package com.carole.database.mcp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.carole.database.mcp.pojo.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON parsing utilities for MCP operations
 * 
 * This utility provides simple JSON parsing functions for handling fixed values and other JSON data in MCP tool
 * operations.
 * 
 * @author CaroLe
 * @Date 2025/7/7
 * @Description JSON parsing utilities for MCP data insertion
 */
public class JsonParseUtils {

    /**
     * Static ObjectMapper instance for JSON parsing
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parse fixed values JSON string and populate InsertRequest
     * 
     * @param fixedValuesJson JSON string with fixed values
     * @param insertRequest InsertRequest object to populate
     */
    public static void parseFixedValues(String fixedValuesJson, InsertRequest insertRequest) {
        try {
            // Improved JSON parsing to handle URLs and complex values
            String json = fixedValuesJson.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);

                // Parse JSON more carefully to handle quoted values with commas/colons
                List<String> pairs = parseJsonPairs(json);
                for (String pair : pairs) {
                    // Split on first colon only to handle URLs with colons
                    int colonIndex = pair.indexOf(":");
                    if (colonIndex > 0 && colonIndex < pair.length() - 1) {
                        String key = pair.substring(0, colonIndex).trim().replaceAll("[\"']", "");
                        String value = pair.substring(colonIndex + 1).trim().replaceAll("[\"']", "");

                        // Validate column name - skip if invalid
                        if (SqlSecurityValidator.isValidColumnName(key)) {
                            continue;
                        }

                        // Convert value to appropriate type
                        Object convertedValue = convertValue(value);
                        insertRequest.addFixedValue(key, convertedValue);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore JSON parsing errors, continue without fixed values
        }
    }

    /**
     * Convert string value to appropriate type
     * 
     * @param value String value to convert
     * @return Converted value (Integer, Double, Boolean, or String)
     */
    public static Object convertValue(String value) {
        if (value == null || "null".equals(value)) {
            return null;
        }

        // Try integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Continue
        }

        // Try double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Continue
        }

        // Try boolean
        if ("true".equals(value) || "false".equals(value)) {
            return Boolean.parseBoolean(value);
        }

        // Return as string
        return value;
    }

    /**
     * Parse JSON pairs considering quoted values that may contain commas
     * 
     * @param json JSON content without outer braces
     * @return List of key-value pairs
     */
    private static List<String> parseJsonPairs(String json) {
        List<String> pairs = new ArrayList<>();
        StringBuilder currentPair = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' || c == '\'') {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                }
                currentPair.append(c);
            } else if (c == ',' && !inQuotes) {
                // Found a separator outside of quotes
                if (!currentPair.isEmpty()) {
                    pairs.add(currentPair.toString().trim());
                    currentPair.setLength(0);
                }
            } else {
                currentPair.append(c);
            }
        }

        // Add the last pair
        if (!currentPair.isEmpty()) {
            pairs.add(currentPair.toString().trim());
        }

        return pairs;
    }

    /**
     * Parse enhanced configuration JSON into InsertRequest
     * 
     * @param configJson Configuration JSON
     * @param insertRequest Insert request to populate
     * @return true if parsing succeeded, false otherwise
     */
    @SuppressWarnings("unchecked")
    public static boolean parseEnhancedConfiguration(String configJson, InsertRequest insertRequest) {
        try {
            // Try to parse as enhanced configuration first
            Map<String, Object> configMap = parseJsonToMap(configJson);
            if (configMap == null) {
                // Fall back to simple fixed values parsing
                parseFixedValues(configJson, insertRequest);
                return true;
            }

            // Parse groups
            if (configMap.containsKey("groups")) {
                List<Map<String, Object>> groupsList = (List<Map<String, Object>>)configMap.get("groups");
                for (Map<String, Object> groupMap : groupsList) {
                    DataGroup group = new DataGroup();

                    if (groupMap.containsKey("recordCount")) {
                        group.setRecordCount(((Number)groupMap.get("recordCount")).intValue());
                    }

                    if (groupMap.containsKey("fixedValues")) {
                        Map<String, Object> fixedValues = (Map<String, Object>)groupMap.get("fixedValues");
                        group.setFixedValues(fixedValues);
                    }

                    if (groupMap.containsKey("description")) {
                        group.setDescription((String)groupMap.get("description"));
                    }

                    insertRequest.addDataGroup(group);
                }
            }

            // Parse sequences
            if (configMap.containsKey("sequences")) {
                Map<String, Map<String, Object>> sequencesMap =
                    (Map<String, Map<String, Object>>)configMap.get("sequences");
                for (Map.Entry<String, Map<String, Object>> entry : sequencesMap.entrySet()) {
                    String columnName = entry.getKey();
                    Map<String, Object> seqMap = entry.getValue();

                    SequenceDefinition sequence = new SequenceDefinition();

                    if (seqMap.containsKey("type")) {
                        String type = (String)seqMap.get("type");
                        sequence.setType(SequenceDefinition.SequenceType.valueOf(type));
                    }

                    if (seqMap.containsKey("startValue")) {
                        sequence.setStartValue(((Number)seqMap.get("startValue")).longValue());
                    }

                    if (seqMap.containsKey("step")) {
                        sequence.setStep(((Number)seqMap.get("step")).longValue());
                    }

                    if (seqMap.containsKey("customValues")) {
                        List<Object> customValuesList = (List<Object>)seqMap.get("customValues");
                        sequence.setCustomValues(customValuesList.toArray());
                    }

                    if (seqMap.containsKey("pattern")) {
                        sequence.setPattern((String)seqMap.get("pattern"));
                    }

                    insertRequest.addSequence(columnName, sequence);
                }
            }

            // If no groups or sequences, try simple fixed values
            if (!insertRequest.hasDataGroups() && !insertRequest.hasSequences()) {
                // Parse as simple fixed values
                for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                    if (!"groups".equals(entry.getKey()) && !"sequences".equals(entry.getKey())) {
                        insertRequest.addFixedValue(entry.getKey(), entry.getValue());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse JSON string to Map using Jackson ObjectMapper
     * 
     * @param jsonString JSON string
     * @return Map representation or null if parsing fails
     */
    public static Map<String, Object> parseJsonToMap(String jsonString) {
        try {
            // Use Jackson ObjectMapper for reliable JSON parsing
            String json = jsonString.trim();
            if (!json.startsWith("{") || !json.endsWith("}")) {
                return null;
            }

            // Parse JSON using Jackson with proper type handling
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            return OBJECT_MAPPER.readValue(json, typeRef);

        } catch (JsonProcessingException e) {
            // Log error for debugging if needed
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse JSON value (string, number, boolean, array, object)
     * 
     * @param valueStr Value string
     * @return Parsed value
     */
    private static Object parseJsonValue(String valueStr) {
        valueStr = valueStr.trim();

        if ("null".equals(valueStr)) {
            return null;
        }

        if ("true".equals(valueStr) || "false".equals(valueStr)) {
            return Boolean.parseBoolean(valueStr);
        }

        if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
            // Parse array
            List<Object> list = new ArrayList<>();
            String arrayContent = valueStr.substring(1, valueStr.length() - 1);
            if (!arrayContent.trim().isEmpty()) {
                List<String> items = parseJsonPairs(arrayContent);
                for (String item : items) {
                    list.add(parseJsonValue(item));
                }
            }
            return list;
        }

        if (valueStr.startsWith("{") && valueStr.endsWith("}")) {
            // Parse nested object
            return parseJsonToMap(valueStr);
        }

        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return valueStr.substring(1, valueStr.length() - 1);
        }

        // Try to parse as number
        return convertValue(valueStr);
    }

    /**
     * Parse JSON configuration into UpdateRequest object
     * 
     * This method parses a complete JSON configuration string into a structured UpdateRequest
     * object, including global settings, update rules, conditions, and values.
     * 
     * @param tableName Target table name
     * @param updateConfigJson JSON configuration string
     * @return Parsed UpdateRequest or null if parsing fails
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Complete JSON configuration parsing for update requests
     */
    @SuppressWarnings("unchecked")
    public static UpdateRequest parseUpdateConfiguration(String tableName, String updateConfigJson) {
        try {
            Map<String, Object> configMap = parseJsonToMap(updateConfigJson);
            if (configMap == null) {
                return null;
            }
            
            UpdateRequest updateRequest = new UpdateRequest(tableName);
            
            // Parse global settings
            parseUpdateGlobalSettings(configMap, updateRequest);
            
            // Parse update rules
            if (configMap.containsKey("updateRules")) {
                List<Map<String, Object>> rulesList = (List<Map<String, Object>>) configMap.get("updateRules");
                for (Map<String, Object> ruleMap : rulesList) {
                    UpdateRule rule = parseUpdateRule(ruleMap);
                    if (rule != null) {
                        updateRequest.addUpdateRule(rule);
                    }
                }
            }
            
            return updateRequest;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse global settings from configuration map into UpdateRequest
     * 
     * Extracts and applies global configuration settings such as transaction usage,
     * maximum affected records, dry run mode, and detail return preferences.
     * 
     * @param configMap Configuration map containing global settings
     * @param updateRequest UpdateRequest object to apply settings to
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Global settings extraction from configuration map
     */
    private static void parseUpdateGlobalSettings(Map<String, Object> configMap, UpdateRequest updateRequest) {
        if (configMap.containsKey("useTransaction")) {
            updateRequest.setUseTransaction((Boolean) configMap.get("useTransaction"));
        }
        if (configMap.containsKey("maxTotalAffectedRecords")) {
            updateRequest.setMaxTotalAffectedRecords(((Number) configMap.get("maxTotalAffectedRecords")).intValue());
        }
        if (configMap.containsKey("dryRun")) {
            updateRequest.setDryRun((Boolean) configMap.get("dryRun"));
        }
        if (configMap.containsKey("returnDetails")) {
            updateRequest.setReturnDetails((Boolean) configMap.get("returnDetails"));
        }
    }

    /**
     * Parse a single update rule from configuration map
     * 
     * Converts a configuration map into an UpdateRule object, including
     * conditions, update values, and rule-specific settings like maximum affected records.
     * 
     * @param ruleMap Configuration map containing rule configuration
     * @return Parsed UpdateRule or null if parsing fails
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Single update rule parsing with conditions and values
     */
    @SuppressWarnings("unchecked")
    public static UpdateRule parseUpdateRule(Map<String, Object> ruleMap) {
        try {
            UpdateRule rule = new UpdateRule();
            
            // Parse conditions
            if (ruleMap.containsKey("conditions")) {
                List<Map<String, Object>> conditionsList = (List<Map<String, Object>>) ruleMap.get("conditions");
                for (Map<String, Object> conditionMap : conditionsList) {
                    UpdateCondition condition = parseUpdateCondition(conditionMap);
                    if (condition != null) {
                        rule.addCondition(condition);
                    }
                }
            }
            
            // Parse update values
            if (ruleMap.containsKey("updateValues")) {
                Map<String, Object> updateValues = (Map<String, Object>) ruleMap.get("updateValues");
                rule.setUpdateValues(updateValues);
            }
            
            // Parse optional rule settings
            parseUpdateRuleSettings(ruleMap, rule);
            
            return rule;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse rule-specific settings from configuration map
     * 
     * Extracts optional rule settings such as maximum affected records and
     * whether conditions are required for the specific rule.
     * 
     * @param ruleMap Configuration map containing rule configuration
     * @param rule UpdateRule object to apply settings to
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Rule-specific settings extraction and application
     */
    private static void parseUpdateRuleSettings(Map<String, Object> ruleMap, UpdateRule rule) {
        if (ruleMap.containsKey("maxAffectedRecords")) {
            rule.setMaxAffectedRecords(((Number) ruleMap.get("maxAffectedRecords")).intValue());
        }
        if (ruleMap.containsKey("requireConditions")) {
            rule.setRequireConditions((Boolean) ruleMap.get("requireConditions"));
        }
    }

    /**
     * Parse a single update condition from configuration map
     * 
     * Converts a configuration map into an UpdateCondition object,
     * handling various operators and both single and multiple values for conditions.
     * 
     * @param conditionMap Configuration map containing condition configuration
     * @return Parsed UpdateCondition or null if parsing fails
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Single condition parsing with operator and value handling
     */
    @SuppressWarnings("unchecked")
    public static UpdateCondition parseUpdateCondition(Map<String, Object> conditionMap) {
        try {
            UpdateCondition condition = new UpdateCondition();
            
            // Parse required fields
            if (!conditionMap.containsKey("field") || !conditionMap.containsKey("operator")) {
                return null;
            }
            
            condition.setField((String) conditionMap.get("field"));
            condition.setOperator((String) conditionMap.get("operator"));
            
            // Parse connector (optional)
            if (conditionMap.containsKey("connector")) {
                condition.setConnector((String) conditionMap.get("connector"));
            }
            
            // Parse value or values
            parseUpdateConditionValues(conditionMap, condition);
            
            return condition;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse condition values (single or multiple) from configuration map
     * 
     * Handles both single values and arrays of values for different operators,
     * particularly for IN/NOT IN operations that require multiple values.
     * 
     * @param conditionMap Configuration map containing condition configuration
     * @param condition UpdateCondition object to set values on
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Condition value parsing for single and multiple value scenarios
     */
    @SuppressWarnings("unchecked")
    private static void parseUpdateConditionValues(Map<String, Object> conditionMap, UpdateCondition condition) {
        if (conditionMap.containsKey("values")) {
            // Multiple values for IN/NOT IN
            List<Object> values = (List<Object>) conditionMap.get("values");
            condition.setValues(values);
        } else if (conditionMap.containsKey("value")) {
            // Single value
            condition.setValue(conditionMap.get("value"));
        }
    }

    /**
     * Validate parsed UpdateRequest for completeness and correctness
     * 
     * Performs comprehensive validation of a parsed UpdateRequest to ensure
     * all required fields are present and values are within acceptable ranges.
     * 
     * @param updateRequest The UpdateRequest to validate
     * @return Validation error message or null if valid
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Complete validation of parsed update request configuration
     */
    public static String validateParsedUpdateRequest(UpdateRequest updateRequest) {
        if (updateRequest == null) {
            return "Update request is null";
        }
        
        // Use the built-in validation method
        return updateRequest.validate();
    }
    
    /**
     * Parse JSON configuration into DeleteRequest object
     * 
     * This method parses a complete JSON configuration string into a structured DeleteRequest
     * object, including global settings, delete rules, and conditions.
     * 
     * @param tableName Target table name
     * @param deleteConfigJson JSON configuration string
     * @return Parsed DeleteRequest or null if parsing fails
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Complete JSON configuration parsing for delete requests
     */
    @SuppressWarnings("unchecked")
    public static DeleteRequest parseDeleteConfiguration(String tableName, String deleteConfigJson) {
        try {
            Map<String, Object> configMap = parseJsonToMap(deleteConfigJson);
            if (configMap == null) {
                return null;
            }
            
            DeleteRequest deleteRequest = new DeleteRequest(tableName);
            
            // Parse global settings
            parseDeleteGlobalSettings(configMap, deleteRequest);
            
            // Parse delete rules
            if (configMap.containsKey("deleteRules")) {
                List<Map<String, Object>> rulesList = (List<Map<String, Object>>) configMap.get("deleteRules");
                for (Map<String, Object> ruleMap : rulesList) {
                    DeleteRule rule = parseDeleteRule(ruleMap);
                    if (rule != null) {
                        deleteRequest.addDeleteRule(rule);
                    }
                }
            }
            
            return deleteRequest;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Parse global settings from configuration map into DeleteRequest
     * 
     * Extracts and applies global configuration settings such as transaction usage,
     * maximum affected records, dry run mode, and detail return preferences.
     * 
     * @param configMap Configuration map containing global settings
     * @param deleteRequest DeleteRequest object to apply settings to
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Global settings extraction from configuration map for delete operations
     */
    private static void parseDeleteGlobalSettings(Map<String, Object> configMap, DeleteRequest deleteRequest) {
        if (configMap.containsKey("useTransaction")) {
            deleteRequest.setUseTransaction((Boolean) configMap.get("useTransaction"));
        }
        if (configMap.containsKey("maxTotalAffectedRecords")) {
            deleteRequest.setMaxTotalAffectedRecords(((Number) configMap.get("maxTotalAffectedRecords")).intValue());
        }
        if (configMap.containsKey("dryRun")) {
            deleteRequest.setDryRun((Boolean) configMap.get("dryRun"));
        }
        if (configMap.containsKey("returnDetails")) {
            deleteRequest.setReturnDetails((Boolean) configMap.get("returnDetails"));
        }
    }
    
    /**
     * Parse a single delete rule from configuration map
     * 
     * Converts a configuration map into a DeleteRule object, including
     * conditions and rule-specific settings like maximum affected records.
     * 
     * @param ruleMap Configuration map containing rule configuration
     * @return Parsed DeleteRule or null if parsing fails
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Single delete rule parsing with conditions and settings
     */
    @SuppressWarnings("unchecked")
    public static DeleteRule parseDeleteRule(Map<String, Object> ruleMap) {
        try {
            DeleteRule rule = new DeleteRule();
            
            // Parse conditions
            if (ruleMap.containsKey("conditions")) {
                List<Map<String, Object>> conditionsList = (List<Map<String, Object>>) ruleMap.get("conditions");
                for (Map<String, Object> conditionMap : conditionsList) {
                    DeleteCondition condition = parseDeleteCondition(conditionMap);
                    if (condition != null) {
                        rule.addCondition(condition);
                    }
                }
            }
            
            // Parse optional rule settings
            parseDeleteRuleSettings(ruleMap, rule);
            
            return rule;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Parse rule-specific settings from configuration map
     * 
     * Extracts optional rule settings such as maximum affected records,
     * whether conditions are required, and rule description.
     * 
     * @param ruleMap Configuration map containing rule configuration
     * @param rule DeleteRule object to apply settings to
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Rule-specific settings extraction and application for delete operations
     */
    private static void parseDeleteRuleSettings(Map<String, Object> ruleMap, DeleteRule rule) {
        if (ruleMap.containsKey("maxAffectedRecords")) {
            rule.setMaxAffectedRecords(((Number) ruleMap.get("maxAffectedRecords")).intValue());
        }
        if (ruleMap.containsKey("requireConditions")) {
            rule.setRequireConditions((Boolean) ruleMap.get("requireConditions"));
        }
        if (ruleMap.containsKey("description")) {
            rule.setDescription((String) ruleMap.get("description"));
        }
    }
    
    /**
     * Parse a single delete condition from configuration map
     * 
     * Converts a configuration map into a DeleteCondition object,
     * handling various operators and both single and multiple values for conditions.
     * 
     * @param conditionMap Configuration map containing condition configuration
     * @return Parsed DeleteCondition or null if parsing fails
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Single condition parsing with operator and value handling for delete operations
     */
    @SuppressWarnings("unchecked")
    public static DeleteCondition parseDeleteCondition(Map<String, Object> conditionMap) {
        try {
            DeleteCondition condition = new DeleteCondition();
            
            // Parse required fields
            if (!conditionMap.containsKey("field") || !conditionMap.containsKey("operator")) {
                return null;
            }
            
            condition.setField((String) conditionMap.get("field"));
            condition.setOperator((String) conditionMap.get("operator"));
            
            // Parse connector (optional)
            if (conditionMap.containsKey("connector")) {
                condition.setConnector((String) conditionMap.get("connector"));
            }
            
            // Parse value or values
            parseDeleteConditionValues(conditionMap, condition);
            
            return condition;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Parse condition values (single or multiple) from configuration map
     * 
     * Handles both single values and arrays of values for different operators,
     * particularly for IN/NOT IN operations that require multiple values.
     * 
     * @param conditionMap Configuration map containing condition configuration
     * @param condition DeleteCondition object to set values on
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Condition value parsing for single and multiple value scenarios in delete operations
     */
    @SuppressWarnings("unchecked")
    private static void parseDeleteConditionValues(Map<String, Object> conditionMap, DeleteCondition condition) {
        if (conditionMap.containsKey("values")) {
            // Multiple values for IN/NOT IN
            List<Object> values = (List<Object>) conditionMap.get("values");
            condition.setValues(values);
        } else if (conditionMap.containsKey("value")) {
            // Single value
            condition.setValue(conditionMap.get("value"));
        }
    }
    
    /**
     * Validate parsed DeleteRequest for completeness and correctness
     * 
     * Performs comprehensive validation of a parsed DeleteRequest to ensure
     * all required fields are present and values are within acceptable ranges.
     * 
     * @param deleteRequest The DeleteRequest to validate
     * @return Validation error message or null if valid
     * @author CaroLe
     * @Date 2025/07/10
     * @Description Complete validation of parsed delete request configuration
     */
    public static String validateParsedDeleteRequest(DeleteRequest deleteRequest) {
        if (deleteRequest == null) {
            return "Delete request is null";
        }
        
        // Use the built-in validation method
        return deleteRequest.validate();
    }
}