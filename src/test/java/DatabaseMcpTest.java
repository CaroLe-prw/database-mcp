import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.carole.database.mcp.DatabaseMcpApplication;
import com.carole.database.mcp.service.DatabaseService;

import jakarta.annotation.Resource;

/**
 * @author CaroLe
 * @Date 2025/7/6 15:59
 * @Description
 */
@SpringBootTest(classes = DatabaseMcpApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class DatabaseMcpTest {

    @Resource
    private DatabaseService databaseService;

    @Test
    public void testListTables() {
        // 执行测试 - 使用默认数据源
        String tables = databaseService.listTables(null);

        // 验证结果
        assertNotNull(tables);
        assertFalse(tables.isEmpty());

        // 输出结果
        System.out.println("=== Default DataSource Tables ===");
        System.out.println(tables);
    }

    @Test
    public void testListTablesWithDynamicConfig() {
        // 测试动态数据源配置
        String adminConfig = """
            {
              "host": "192.168.31.50",
              "port": "3306",
              "username": "root",
              "password": "Aa040832@",
              "database": "mall_admin",
              "databaseType": "mysql"
            }
            """;

        String tables = databaseService.listTables(adminConfig);

        // 验证结果
        assertNotNull(tables);
        assertFalse(tables.isEmpty());

        // 输出结果
        System.out.println("=== Dynamic DataSource (mall_admin) Tables ===");
        System.out.println(tables);
    }

    @Test
    public void describeTable() {
        String tableInfo = databaseService.describeTable("sys_user", null);

        // 输出结果
        System.out.println(tableInfo);
    }

    @Test
    public void insertData() {
        String jsonConfig =
            """
                {
                    "groups": [
                        {"recordCount": 2, "fixedValues": {"status": 1}},
                        {"recordCount": 3, "fixedValues": {"status": 1}},
                        {"recordCount": 1, "fixedValues": {"status": 0}}
                    ],
                    "sequences": {
                        "username": {
                            "type": "CUSTOM_VALUES",
                            "customValues": ["dept_admin", "dept_manager", "employee001", "employee002", "employee003", "temp_user"]
                        },
                        "name": {
                            "type": "CUSTOM_VALUES",
                            "customValues": ["部门管理员", "部门经理", "员工张三", "员工李四", "员工王五", "临时用户"]
                        }
                    }
                }
                """;
        String result = databaseService.insertData("sys_user", 6, jsonConfig, "admin");
        // 输出结果
        System.out.println(result);

    }

    @Test
    public void updateData() {
        String jsonConfig = """
            {
              "updateRules": [
                {
                  "conditions": [{"field": "username", "operator": "=", "value": "3"}],
                  "updateValues": {"name": "2", "username": "234"}
                },
                {
                  "conditions": [{"field": "username", "operator": "=", "value": "dept_manager"}],
                  "updateValues": {"name": "3", "username": "manager4"}
                }
              ]
            }
            """;
        String result = databaseService.updateData("sys_user", jsonConfig, "admin");
        // 输出结果
        System.out.println(result);

    }

    @Test
    public void deleteData() {
        String jsonConfig = """
            {
                "deleteRules": [
                    {
                        "conditions": [
                            {
                                "field": "username",
                                "operator": "IN",
                                "values": ["employee002", "employee003", "temp_user"]
                            }
                        ],
                        "maxAffectedRecords": 10,
                        "description": "删除测试用户"
                    }
                ],
                "useTransaction": true,
                "returnDetails": true
            }
            """;
        String result = databaseService.deleteData("sys_user", jsonConfig, "admin");
        // 输出结果
        System.out.println(result);

    }

    @Test
    public void executeQuery() {
        String sqlQuery = "SELECT * FROM sys_user WHERE username = '234'";
        String result = databaseService.executeQuery(sqlQuery, "admin");
        // 输出结果
        System.out.println(result);
    }
}