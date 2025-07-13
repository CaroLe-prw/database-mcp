# Database MCP Server

一个基于 Spring AI 的数据库 MCP (Model Context Protocol) 服务器，提供智能数据库操作功能，支持多数据源切换、高级数据生成、更新、删除等操作。

## 🚀 功能特性

### 🔄 多数据源支持

- ✅ **预定义数据源**: 支持环境变量前缀配置多个数据源
- ✅ **动态数据源**: 支持 JSON 配置动态创建数据源连接
- ✅ **无缝切换**: 所有工具支持数据源参数，可在不同数据库间切换
- ✅ **配置灵活**: 支持 3 种配置方式：环境变量、JSON 配置、工具参数
- ✅ **向后兼容**: 保持原有单数据源模式完全兼容

### 核心数据库工具

- ✅ **listTables**: 列出数据库中所有表（支持多数据源）
- ✅ **describeTable**: 查看表结构和字段信息（支持多数据源）
- ✅ **insertData**: 高级测试数据生成（支持多数据源）
    - 🎯 **4种生成模式**: 简单固定值、分组、序列、组合模式
    - 🎲 **智能数据生成**: 使用 DataFaker 生成真实业务数据
    - 📊 **ENUM/SET 支持**: 自动提取数据库枚举值
    - 🔄 **事务安全**: 完整的 ACID 事务支持
- ✅ **updateData**: 智能数据更新（支持多数据源）
    - 🎯 **多条件更新**: 支持多种操作符（=, !=, >, <, >=, <=, IN, NOT IN, LIKE, NOT LIKE）
    - 📊 **批量更新**: 支持多个更新规则
    - 🔢 **记录限制**: 可配置的安全更新限制
    - 🔄 **事务安全**: 完整的 ACID 事务支持
- ✅ **deleteData**: 灵活数据删除（支持多数据源）
    - 🎯 **条件删除**: 支持多种操作符和复杂条件
    - 🗑️ **批量删除**: 支持多个删除规则
    - 🔢 **安全限制**: 可配置的最大删除记录数
    - 🔄 **事务安全**: 完整的 ACID 事务支持
- ✅ **executeQuery**: 安全 SQL 查询执行（支持多数据源）
    - 🔍 **SELECT 查询**: 仅支持 SELECT 语句
    - 🛡️ **SQL 注入防护**: 参数化查询防护
    - 📄 **自动分页**: 自动添加 LIMIT 限制

### 高级特性

- 🔒 **操作权限控制**: 通过环境变量控制各种数据库操作
- 🎛️ **记录数限制**: 可配置的插入、更新、删除记录数限制
- 🔍 **严格验证模式**: 基于反射的增强安全验证
- 🏗️ **跨数据库支持**: SqlBuilder 工具支持多数据库类型
- 📊 **智能表格输出**: ASCII 表格格式化输出
- 🔄 **连接池管理**: Druid 连接池优化
- 📝 **内存优化**: 最小化内存占用的 MCP 配置

## 🛠️ 环境要求

- Java 21+
- Spring Boot 3.5.3+
- MySQL 8.0+
- Maven 3.8+

## 📦 安装配置

### 1. 环境变量配置

#### 默认数据库连接配置

```bash
export DATABASE_TYPE=mysql
export HOST=your_host
export PORT=3306
export USER=your_username
export PASSWORD=your_password
export DATABASE=your_database
```

#### 多数据源配置方式一：环境变量前缀

```bash
# PMS 数据库配置
export PMS_DB_HOST=192.168.31.50
export PMS_DB_PORT=3306
export PMS_DB_USERNAME=root
export PMS_DB_PASSWORD=your_password
export PMS_DB_DATABASE=mall_pms
export PMS_DB_DATABASE_TYPE=mysql

# Admin 数据库配置
export ADMIN_DB_HOST=192.168.31.50
export ADMIN_DB_PORT=3306
export ADMIN_DB_USERNAME=root
export ADMIN_DB_PASSWORD=your_password
export ADMIN_DB_DATABASE=mall_admin
export ADMIN_DB_DATABASE_TYPE=mysql

# 支持的前缀：PROD_DB, TEST_DB, DEV_DB, STAGING_DB, PMS_DB, ADMIN_DB
```

#### 多数据源配置方式二：JSON 配置

```bash
# 通过 DATASOURCE_CONFIG 环境变量配置多个数据源
export DATASOURCE_CONFIG='{"dataSources":[{"name":"pms","description":"Product Management System","host":"192.168.31.50","port":"3306","username":"root","password":"your_password","database":"mall_pms","databaseType":"mysql"},{"name":"admin","description":"Admin Management System","host":"192.168.31.50","port":"3306","username":"root","password":"your_password","database":"mall_admin","databaseType":"mysql"}]}'
```

#### 操作权限控制

```bash
# 操作权限开关
export ALLOW_INSERT_OPERATION=true
export ALLOW_UPDATE_OPERATION=true
export ALLOW_DELETE_OPERATION=true
export ALLOW_QUERY_OPERATION=true

# 记录数限制
export MAX_INSERT_RECORDS=1000
export MAX_UPDATE_RECORDS=500
export MAX_DELETE_RECORDS=100

# 安全控制
export ALLOW_UNCONDITIONAL_UPDATE=false
export ALLOW_UNCONDITIONAL_DELETE=false
export STRICT_VALIDATION_MODE=false
```

### 2. 编译和运行

```bash
# 编译项目
./mvnw clean compile

# 打包项目
./mvnw clean package

# 运行 MCP 服务器（内存优化版本）
java -Xms32m -Xmx128m -XX:MetaspaceSize=32m -XX:MaxMetaspaceSize=64m \
     -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:TieredStopAtLevel=1 \
     -Dcom.sun.management.jmxremote=false \
     -jar target/database-mcp-0.0.1-SNAPSHOT.jar
```

### 3. 测试

```bash
# 运行所有测试
./mvnw test

# 运行测试并生成覆盖率报告
./mvnw test jacoco:report

# 快速编译检查
./mvnw clean compile -q
```

## 🎯 MCP 客户端配置

### 单数据源配置（向后兼容）

```json
{
  "mcpServers": {
    "database-mcp": {
      "command": "java",
      "args": [
        "-Xms32m",
        "-Xmx128m",
        "-XX:MetaspaceSize=32m",
        "-XX:MaxMetaspaceSize=64m",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100",
        "-XX:TieredStopAtLevel=1",
        "-Dcom.sun.management.jmxremote=false",
        "-jar",
        "/path/to/database-mcp-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "DATABASE": "your_database",
        "DATABASE_TYPE": "mysql",
        "HOST": "your_host",
        "PASSWORD": "your_password",
        "PORT": "3306",
        "USER": "your_username",
        "ALLOW_INSERT_OPERATION": "true",
        "ALLOW_UPDATE_OPERATION": "true",
        "ALLOW_DELETE_OPERATION": "true",
        "ALLOW_QUERY_OPERATION": "true",
        "MAX_INSERT_RECORDS": "1000",
        "MAX_UPDATE_RECORDS": "500",
        "MAX_DELETE_RECORDS": "100"
      }
    }
  }
}
```

### 多数据源配置（推荐）

```json
{
  "mcpServers": {
    "database-mcp": {
      "command": "java",
      "args": [
        "-Xms32m",
        "-Xmx128m",
        "-XX:MetaspaceSize=32m",
        "-XX:MaxMetaspaceSize=64m",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100",
        "-XX:TieredStopAtLevel=1",
        "-Dcom.sun.management.jmxremote=false",
        "-jar",
        "/path/to/database-mcp-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "DATABASE": "mall_admin",
        "DATABASE_TYPE": "mysql",
        "HOST": "192.168.31.50",
        "PASSWORD": "your_password",
        "PORT": "3306",
        "USER": "root",
        "PMS_DB_HOST": "192.168.31.50",
        "PMS_DB_PORT": "3306",
        "PMS_DB_USERNAME": "root",
        "PMS_DB_PASSWORD": "your_password",
        "PMS_DB_DATABASE": "mall_pms",
        "PMS_DB_DATABASE_TYPE": "mysql",
        "ADMIN_DB_HOST": "192.168.31.50",
        "ADMIN_DB_PORT": "3306",
        "ADMIN_DB_USERNAME": "root",
        "ADMIN_DB_PASSWORD": "your_password",
        "ADMIN_DB_DATABASE": "mall_admin",
        "ADMIN_DB_DATABASE_TYPE": "mysql",
        "ALLOW_INSERT_OPERATION": "true",
        "ALLOW_UPDATE_OPERATION": "true",
        "ALLOW_DELETE_OPERATION": "true",
        "ALLOW_QUERY_OPERATION": "true",
        "MAX_INSERT_RECORDS": "1000",
        "MAX_UPDATE_RECORDS": "500",
        "MAX_DELETE_RECORDS": "100"
      }
    }
  }
}
```

## 📖 使用示例

### 1. 基础数据库操作

#### 单数据源模式（向后兼容）

```java
// 列出默认数据源的所有表
listTables()

// 查看默认数据源的表结构
describeTable("users")

// 在默认数据源执行查询
executeQuery("SELECT * FROM users WHERE status = 'active'")
```

#### 多数据源模式

```java
// 使用预定义数据源名称
listTables("pms")           // PMS 数据库的表

listTables("admin")         // Admin 数据库的表

// 查看不同数据源的表结构
describeTable("products","pms")    // PMS 数据库的 products 表

describeTable("sys_user","admin")  // Admin 数据库的 sys_user 表

// 在不同数据源执行查询
executeQuery("SELECT * FROM products WHERE status = 1","pms")

executeQuery("SELECT * FROM sys_user WHERE username = 'admin'","admin")

// 使用完整 JSON 配置（动态数据源）
executeQuery("SELECT COUNT(*) FROM orders",
                     "{\"host\":\"192.168.31.50\",\"port\":\"3306\",\"username\":\"root\",\"password\":\"password\",\"database\":\"mall_oms\",\"databaseType\":\"mysql\"}")
```

### 2. 高级数据生成

#### 简单固定值

```java
// 在默认数据源插入数据
insertData("users",10,"{\"status\":1,\"type\":\"active\"}")

// 在指定数据源插入数据
insertData("products",5,"{\"status\":1,\"category\":\"electronics\"}","pms")

insertData("sys_user",3,"{\"status\":1,\"role\":\"admin\"}","admin")
```

#### 分组生成

```java
// 在不同数据源进行分组数据生成
insertData("orders",15,
                   "{\"groups\":[{\"recordCount\":10,\"fixedValues\":{\"status\":\"pending\"}},{\"recordCount\":5,\"fixedValues\":{\"status\":\"completed\"}}]}",
                   "pms")

insertData("sys_log",20,
                   "{\"groups\":[{\"recordCount\":15,\"fixedValues\":{\"level\":\"INFO\"}},{\"recordCount\":5,\"fixedValues\":{\"level\":\"ERROR\"}}]}",
                   "admin")
```

#### 序列生成

```java
// 在不同数据源进行序列数据生成
insertData("products",5,
                   "{\"sequences\":{\"product_code\":{\"type\":\"CUSTOM_VALUES\",\"customValues\":[\"P001\",\"P002\",\"P003\",\"P004\",\"P005\"]}}}",
                   "pms")

insertData("sys_user",3,
                   "{\"sequences\":{\"username\":{\"type\":\"CUSTOM_VALUES\",\"customValues\":[\"admin\",\"manager\",\"user\"]}}}",
                   "admin")
```

#### 组合模式

```java
// 在不同数据源进行组合模式数据生成
insertData("employees",8,
                   "{\"groups\":[{\"recordCount\":5,\"fixedValues\":{\"department\":\"IT\"}},{\"recordCount\":3,\"fixedValues\":{\"department\":\"HR\"}}],\"sequences\":{\"employee_id\":{\"type\":\"INCREMENT\",\"startValue\":1000,\"step\":1}}}",
                   "admin")
```

### 3. 数据更新操作

#### 简单更新

```java
// 在默认数据源更新
updateData("users",
                   "{\"updateRules\":[{\"conditions\":[{\"field\":\"id\",\"operator\":\"=\",\"value\":1}],\"updateValues\":{\"status\":\"active\"}}]}")

// 在指定数据源更新
updateData("products",
                   "{\"updateRules\":[{\"conditions\":[{\"field\":\"id\",\"operator\":\"=\",\"value\":1}],\"updateValues\":{\"status\":1}}]}",
                   "pms")

updateData("sys_user",
                   "{\"updateRules\":[{\"conditions\":[{\"field\":\"username\",\"operator\":\"=\",\"value\":\"admin\"}],\"updateValues\":{\"last_login\":\"2025-01-01\"}}]}",
                   "admin")
```

#### 条件更新

```java
// 在不同数据源进行条件更新
updateData("orders",
                   "{\"updateRules\":[{\"conditions\":[{\"field\":\"status\",\"operator\":\"=\",\"value\":\"pending\"},{\"field\":\"created_time\",\"operator\":\"<\",\"value\":\"2024-01-01\"}],\"updateValues\":{\"status\":\"expired\"},\"maxRecords\":100}]}",
                   "pms")
```

#### 多规则更新

```java
// 在指定数据源进行多规则更新
updateData("products",
                   "{\"updateRules\":[{\"conditions\":[{\"field\":\"category\",\"operator\":\"=\",\"value\":\"electronics\"}],\"updateValues\":{\"discount\":0.1}},{\"conditions\":[{\"field\":\"category\",\"operator\":\"=\",\"value\":\"books\"}],\"updateValues\":{\"discount\":0.05}}]}",
                   "pms")
```

### 4. 数据删除操作

#### 简单删除

```java
// 在默认数据源删除
deleteData("users",
                   "{\"deleteRules\":[{\"conditions\":[{\"field\":\"id\",\"operator\":\"=\",\"value\":1}],\"maxRecords\":1}]}")

// 在指定数据源删除
deleteData("products",
                   "{\"deleteRules\":[{\"conditions\":[{\"field\":\"status\",\"operator\":\"=\",\"value\":0}],\"maxRecords\":10}]}",
                   "pms")

deleteData("sys_log",
                   "{\"deleteRules\":[{\"conditions\":[{\"field\":\"level\",\"operator\":\"=\",\"value\":\"DEBUG\"}],\"maxRecords\":100}]}",
                   "admin")
```

#### 条件删除

```java
// 在不同数据源进行条件删除
deleteData("logs",
                   "{\"deleteRules\":[{\"conditions\":[{\"field\":\"status\",\"operator\":\"=\",\"value\":\"inactive\"},{\"field\":\"last_login\",\"operator\":\"<\",\"value\":\"2023-01-01\"}],\"maxRecords\":50}]}",
                   "admin")
```

#### 多规则删除

```java
// 在指定数据源进行多规则删除
deleteData("temp_data",
                   "{\"deleteRules\":[{\"conditions\":[{\"field\":\"status\",\"operator\":\"=\",\"value\":\"deleted\"}],\"maxRecords\":100},{\"conditions\":[{\"field\":\"type\",\"operator\":\"IN\",\"values\":[\"temp\",\"test\"]}],\"maxRecords\":50}]}",
                   "admin")
```

### 5. AI 集成最佳实践

#### 数据源参数格式说明

**✅ 正确的数据源参数格式：**
```java
// 使用预定义数据源名称（推荐）
listTables("pms")      // 正确
listTables("admin")    // 正确
listTables("test")     // 正确

// 使用 null 或空字符串表示默认数据源
listTables(null)       // 正确
listTables("")         // 正确

// 使用完整 JSON 配置
listTables("{\"host\":\"192.168.31.50\",\"port\":\"3306\",\"username\":\"root\",\"password\":\"password\",\"database\":\"mall_oms\",\"databaseType\":\"mysql\"}")  // 正确
```

**❌ 错误的数据源参数格式：**
```java
// 错误的 JSON 格式（AI 容易误用）
listTables("{\"database\":\"admin\"}")     // 错误！
listTables("{\"database\":\"pms\"}")       // 错误！

// 正确做法
listTables("admin")                       // 正确
listTables("pms")                         // 正确
```

#### AI 使用指南

当与 AI 助手协作时，请明确说明数据源：

```
用户: "查询 PMS 数据库的所有表"
AI: listTables("pms")

用户: "在 Admin 数据库中插入用户数据"
AI: insertData("sys_user", 5, "{\"status\":1}", "admin")

用户: "查询订单表的结构"
AI: describeTable("orders", "pms")  // 如果订单在 PMS 数据库
```

## 🏗️ 架构设计

### 核心架构组件

```
DatabaseMcpApplication (主应用)
├── DatabaseService (服务层)
│   ├── listTables(dataSourceConfig?)          // 支持多数据源
│   ├── describeTable(table, dataSourceConfig?)
│   ├── insertData(table, count, config, dataSourceConfig?)
│   ├── updateData(table, config, dataSourceConfig?)
│   ├── deleteData(table, config, dataSourceConfig?)
│   └── executeQuery(sql, dataSourceConfig?)
├── DataSourceManager (数据源管理)
│   ├── resolveDataSourceConfig()            // 数据源解析
│   ├── executeWithDataSource()              // 统一执行逻辑
│   └── getDataSource()                      // 数据源获取
├── DataSourceConfigParser (配置解析)
│   ├── parseMultiDataSourceConfig()         // 多数据源解析
│   ├── parseSingleDataSourceConfig()        // 单数据源解析
│   └── createFromEnvironment()              // 环境变量解析
├── DatabaseStrategy (策略模式)
│   └── MySQLDatabaseStrategy
├── DatabaseOperationConfig (操作权限配置)
├── SqlBuilder (跨数据库 SQL 构建)
└── Utility Classes
    ├── DataGenerator (数据生成器)
    ├── JsonParseUtils (JSON 解析)
    ├── SqlSecurityValidator (安全验证)
    └── TableFormatter (表格格式化)
```

### 多数据源架构

```
多数据源支持架构
├── 配置层
│   ├── 环境变量前缀 (PMS_DB_*, ADMIN_DB_*)
│   ├── JSON 配置 (DATASOURCE_CONFIG)
│   └── 动态配置 (工具参数)
├── 解析层
│   ├── DataSourceConfigParser
│   └── JsonParseUtils
├── 管理层
│   ├── DataSourceManager (统一管理)
│   └── DataSourceFactory (连接创建)
└── 执行层
    ├── DatabaseStrategy (数据库操作)
    └── Connection Pool (连接池)
```

### 数据源配置优先级

```
数据源解析优先级 (从高到低)
1. 工具参数中的完整 JSON 配置
   - 优点: 最灵活，支持任意数据库
   - 缺点: 配置复杂，不适合频繁使用
   
2. 工具参数中的预定义数据源名称
   - 优点: 简单易用，AI 友好
   - 缺点: 需要预先配置
   
3. 默认数据源 (null 或空字符串)
   - 优点: 向后兼容，无需额外配置
   - 缺点: 只能访问一个数据库
```

### 配置示例对比

#### 方式一：环境变量前缀（推荐）
**优点**: 配置清晰，易于管理，支持容器化部署
```bash
# 生产环境
PROD_DB_HOST=prod.mysql.com
PROD_DB_DATABASE=production_db

# 测试环境
TEST_DB_HOST=test.mysql.com
TEST_DB_DATABASE=test_db

# PMS 系统
PMS_DB_HOST=pms.mysql.com
PMS_DB_DATABASE=mall_pms

# 管理系统
ADMIN_DB_HOST=admin.mysql.com
ADMIN_DB_DATABASE=mall_admin
```

#### 方式二：JSON 配置
**优点**: 灵活，支持复杂配置
```bash
DATASOURCE_CONFIG='{
  "dataSources": [
    {
      "name": "production",
      "description": "Production Database",
      "host": "prod.mysql.com",
      "database": "production_db"
    },
    {
      "name": "analytics",
      "description": "Analytics Database",
      "host": "analytics.mysql.com",
      "database": "analytics_db"
    }
  ]
}'
```

### 数据结构设计

```
Data Models
├── InsertRequest (插入请求)
├── UpdateRequest (更新请求)
├── DeleteRequest (删除请求)
├── DataGroup (数据分组)
├── UpdateRule (更新规则)
├── DeleteRule (删除规则)
├── DeleteCondition (删除条件)
└── SequenceDefinition (序列定义)
```

## 🔒 安全特性

### 1. SQL 注入防护

- 参数化查询
- 输入验证和清理
- SQL 模式匹配
- 表名和列名验证

### 2. 操作权限控制

- 环境变量控制各种数据库操作
- 记录数限制防止批量操作风险
- 无条件操作控制
- 严格验证模式

### 3. 事务安全

- 完整的 ACID 事务支持
- 自动回滚机制
- 连接状态恢复
- 资源管理

## 📊 性能优化

### 1. 内存优化

- 最小化 JVM 内存占用
- 高效的连接池管理
- 资源及时释放

### 2. 连接池优化

- Druid 连接池配置
- 连接复用和监控
- 连接泄漏检测

### 3. 查询优化

- 自动 LIMIT 限制
- 结果集大小控制
- 分页查询支持

## 🧪 测试

### 单元测试

```bash
# 运行所有测试
./mvnw test

# 运行指定测试类
./mvnw test -Dtest=DatabaseServiceTest

# 运行测试并生成覆盖率报告
./mvnw test jacoco:report
```

### 集成测试

```bash
# 运行集成测试
./mvnw verify

# 运行性能测试
./mvnw test -Dtest=PerformanceTest
```

## 📁 项目结构

```
database-mcp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/carole/database/mcp/
│   │   │       ├── DatabaseMcpApplication.java
│   │   │       ├── config/
│   │   │       │   ├── DataSourceConfig.java
│   │   │       │   ├── DataSourceConfigParser.java
│   │   │       │   ├── DatabaseConfig.java
│   │   │       │   └── DatabaseOperationConfig.java
│   │   │       ├── manager/
│   │   │       │   └── DataSourceManager.java
│   │   │       ├── constant/
│   │   │       │   └── SqlTypeConstants.java
│   │   │       ├── factory/
│   │   │       │   └── DataSourceFactory.java
│   │   │       ├── pojo/
│   │   │       │   ├── DataGroup.java
│   │   │       │   ├── InsertRequest.java
│   │   │       │   ├── UpdateRequest.java
│   │   │       │   ├── DeleteRequest.java
│   │   │       │   ├── UpdateRule.java
│   │   │       │   ├── DeleteRule.java
│   │   │       │   ├── DeleteCondition.java
│   │   │       │   ├── SequenceDefinition.java
│   │   │       │   ├── TableInfo.java
│   │   │       │   └── TableMetadata.java
│   │   │       ├── service/
│   │   │       │   └── DatabaseService.java
│   │   │       ├── strategy/
│   │   │       │   ├── DatabaseStrategy.java
│   │   │       │   └── impl/
│   │   │       │       └── MySQLDatabaseStrategy.java
│   │   │       └── util/
│   │   │           ├── DataGenerator.java
│   │   │           ├── JsonParseUtils.java
│   │   │           ├── QueryUtils.java
│   │   │           ├── SnowflakeIdGenerator.java
│   │   │           ├── SqlBuilder.java
│   │   │           ├── SqlSecurityValidator.java
│   │   │           └── TableFormatter.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/
│           └── com/carole/database/mcp/
│               ├── DatabaseMcpApplicationTest.java
│               ├── service/
│               │   └── DatabaseServiceTest.java
│               └── util/
│                   ├── DataGeneratorTest.java
│                   ├── JsonParseUtilsTest.java
│                   └── SqlSecurityValidatorTest.java
├── logs/
│   └── database-mcp.log
├── target/
│   └── database-mcp-0.0.1-SNAPSHOT.jar
├── CLAUDE.md
├── README.md
└── pom.xml
```

## 🔧 故障排除

### 多数据源相关问题

#### 1. 数据源解析失败
**现象**: "Cannot resolve datasource configuration" 错误
**解决方案**:
```bash
# 检查预定义数据源配置
echo $PMS_DB_HOST
echo $ADMIN_DB_HOST

# 检查 JSON 配置格式
echo $DATASOURCE_CONFIG | jq .

# 验证数据源名称
# 支持的名称: pms, admin, prod, test, dev, staging
```

#### 2. AI 生成错误的数据源格式
**现象**: AI 使用 `{"database":"admin"}` 格式
**解决方案**: 
- 明确告诉 AI 使用简单字符串: `"admin"`
- 避免使用 `{"database":"xxx"}` 格式
- 查看工具描述中的正确示例

#### 3. 数据源连接失败
**现象**: 特定数据源无法连接
**解决方案**:
```bash
# 检查网络连接
telnet $PMS_DB_HOST $PMS_DB_PORT

# 检查数据库凭据
mysql -h$PMS_DB_HOST -P$PMS_DB_PORT -u$PMS_DB_USERNAME -p$PMS_DB_PASSWORD $PMS_DB_DATABASE

# 检查环境变量
env | grep "_DB_"
```

### 常见问题

#### 4. MCP 连接问题

**现象**: 无法连接到 MCP 服务器
**解决**: 检查 Java 版本，确保使用 Java 21+，检查环境变量配置

#### 5. 多数据源工具参数问题
**现象**: AI 传递错误的数据源参数格式
**解决**: 
- 确保 AI 使用简单字符串格式: `"pms"`, `"admin"`
- 避免使用嵌套 JSON: `{"database":"admin"}`
- 参考工具描述中的正确示例

#### 6. 数据库连接失败

**现象**: 无法连接到数据库
**解决**: 验证数据库连接参数，检查网络连接，确认数据库服务运行正常

#### 7. 内存不足

**现象**: 应用启动缓慢或内存溢出
**解决**: 使用推荐的 JVM 参数配置，调整内存分配

#### 8. SQL 注入警告

**现象**: SQL 查询被拒绝
**解决**: 使用参数化查询，避免直接拼接 SQL 语句

#### 9. 操作权限被拒绝

**现象**: 数据库操作被拒绝执行
**解决**: 检查环境变量中的操作权限配置，确认是否允许相应操作

### 日志分析

```bash
# 查看应用日志
tail -f logs/database-mcp.log

# 查看错误日志
grep ERROR logs/database-mcp.log

# 查看性能日志
grep PERFORMANCE logs/database-mcp.log
```

## 📋 待开发功能

### ✅ 已完成功能

- ✅ **多数据源支持**: 环境变量、JSON 配置、动态数据源
- ✅ **智能数据生成**: 4 种生成模式，业务智能数据
- ✅ **安全框架**: SQL 注入防护，操作权限控制
- ✅ **事务安全**: 完整 ACID 支持，自动回滚
- ✅ **连接池管理**: Druid 连接池，资源优化
- ✅ **内存优化**: 最小化 JVM 内存占用

### 🚧 未来增强

- 🚧 **多数据库支持**: PostgreSQL, Oracle, SQL Server 支持
- 🚧 **查询构建器**: 可视化查询构建功能
- 🚧 **模式迁移**: DDL 操作和安全控制
- 🚧 **性能监控**: 查询性能指标和日志
- 🚧 **高级安全**: 基于角色的访问控制，审计日志
- 🚧 **缓存层**: 查询结果缓存，提高频繁访问数据的性能

## 🤝 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📝 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 👨‍💻 作者

- **CaroLe** - 初始工作和持续维护

## 🙏 致谢

- Spring AI 团队提供的 MCP 框架
- DataFaker 团队提供的数据生成库
- Druid 团队提供的连接池解决方案
- 所有贡献者和用户的支持
