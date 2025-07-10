package com.carole.database.mcp.util;

import java.util.Arrays;
import java.util.List;

import com.carole.database.mcp.pojo.TableInfo;
import com.carole.database.mcp.pojo.TableMetadata;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

/**
 * Table formatting utility class for unified database table display formatting
 * 
 * This utility class provides static methods to format database table metadata and table structure information into
 * readable ASCII tables. It encapsulates the complex AsciiTable column configuration and formatting logic.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Utility class for formatting database table information into ASCII tables
 */
public class TableFormatter {

    /**
     * Format table list with metadata into ASCII table
     * 
     * @param metadataList List of table metadata containing table names and remarks
     * @return Formatted ASCII table string
     */
    public static String formatTableList(List<TableMetadata> metadataList) {
        if (metadataList == null || metadataList.isEmpty()) {
            return "No tables found.";
        }
        return AsciiTable.getTable(metadataList,
            Arrays.asList(
                new Column().header("TABLE_NAME").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
                    .minWidth(20).with(TableMetadata::getTableName),
                new Column().header("REMARKS").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
                    .minWidth(20).with(TableMetadata::getRemark)));

    }

    /**
     * Format table structure with detailed column information into ASCII table
     * 
     * @param tableInfos List of table column information including types, constraints, indexes
     * @return Formatted ASCII table string with detailed structure
     */
    public static String formatTableStructure(List<TableInfo> tableInfos) {
        if (tableInfos == null || tableInfos.isEmpty()) {
            return "No table structure found.";
        }
        return AsciiTable.getTable(tableInfos,
            Arrays.asList(
                new Column().header("COLUMN_NAME").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.LEFT)
                    .minWidth(15).with(TableInfo::getName),
                new Column().header("DATA_TYPE").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.LEFT)
                    .minWidth(12).with(TableInfo::getDataType),
                new Column().header("SIZE").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
                    .minWidth(8).with(TableFormatter::formatSize),
                new Column().header("NULLABLE").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
                    .minWidth(8).with(col -> col.isNullable() ? "YES" : "NO"),
                new Column().header("KEY").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
                    .minWidth(5).with(col -> col.isPrimaryKey() ? "PRI" : ""),
                new Column().header("INDEX").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.LEFT)
                    .minWidth(20).with(TableFormatter::formatIndexes),
                new Column().header("DEFAULT").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
                    .minWidth(10).with(col -> col.getDefaultValue() != null ? col.getDefaultValue() : ""),
                new Column().header("EXTRA").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
                    .minWidth(12).with(TableFormatter::formatExtra),
                new Column().header("COMMENT").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.CENTER)
                    .minWidth(15).with(col -> col.getColumnRemarks() != null ? col.getColumnRemarks() : "")));
    }

    /**
     * Format column size information for display
     * 
     * @param tableInfo Table column information
     * @return Formatted size string (e.g., "255", "10,2" for decimal)
     */
    private static String formatSize(TableInfo tableInfo) {
        if (tableInfo.getColumnSize() != null && tableInfo.getColumnSize() > 0) {
            if (tableInfo.getDecimalDigits() != null && tableInfo.getDecimalDigits() > 0) {
                return tableInfo.getColumnSize() + "," + tableInfo.getDecimalDigits();
            }
            return tableInfo.getColumnSize().toString();
        }
        return "";
    }

    /**
     * Format index information for display
     * 
     * @param tableInfo Table column information
     * @return Formatted index string (e.g., "idx_name(UNI), idx_name2(MUL)")
     */
    private static String formatIndexes(TableInfo tableInfo) {
        if (tableInfo.getIndexes().isEmpty()) {
            return "";
        }
        return String.join(", ", tableInfo.getIndexes());
    }

    /**
     * Format extra column information for display
     * 
     * @param tableInfo Table column information
     * @return Formatted extra information string (e.g., "auto_increment", "generated")
     */
    private static String formatExtra(TableInfo tableInfo) {
        StringBuilder extra = new StringBuilder();
        if (tableInfo.isAutoIncrement()) {
            extra.append("auto_increment");
        }
        if (tableInfo.isGenerated()) {
            if (!extra.isEmpty()) {
                extra.append(",");
            }
            extra.append("generated");
        }
        return extra.toString();
    }

    /**
     * Format query results into ASCII table
     * 
     * @param headers Column headers from result set
     * @param rows Data rows from result set
     * @param totalRows Total number of rows processed
     * @return Formatted ASCII table string with query results
     */
    public static String formatQueryResults(List<String> headers, List<List<String>> rows, int totalRows) {
        if (headers == null || headers.isEmpty()) {
            return "No query results to display.";
        }

        if (rows == null || rows.isEmpty()) {
            return "Query executed successfully but returned no data.";
        }

        // Convert List<List<String>> to String[][] for AsciiTable
        String[][] data = new String[rows.size() + 1][headers.size()];

        // Add headers as first row
        for (int i = 0; i < headers.size(); i++) {
            data[0][i] = headers.get(i);
        }

        // Add data rows
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            for (int j = 0; j < Math.min(row.size(), headers.size()); j++) {
                data[i + 1][j] = row.get(j);
            }
        }

        // Generate table using AsciiTable with array data
        String table = AsciiTable.getTable(data);

        // Add summary information
        StringBuilder result = new StringBuilder(table);
        result.append("\n");
        result.append(String.format("Query executed successfully. Showing %d records.", totalRows));

        if (totalRows >= 10) {
            result.append(" (Limited to 10 records for performance)");
        }

        return result.toString();
    }
}