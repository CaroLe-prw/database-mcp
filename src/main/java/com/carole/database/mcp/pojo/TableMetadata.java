package com.carole.database.mcp.pojo;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing basic database table metadata
 * 
 * This class holds essential information about database tables including table name and comments/remarks. Used for
 * listing tables and providing basic table information in database operations.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Simple data structure for basic table metadata including name and comments
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableMetadata implements Serializable {

    @Serial
    private static final long serialVersionUID = 4168336264320085765L;

    /**
     * Database table name
     */
    private String tableName;

    /**
     * Table comment or description
     */
    private String remark;
}
