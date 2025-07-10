# Database MCP Server

一个基于 Spring AI 的数据库 MCP (Model Context Protocol) 服务器，提供智能数据库操作功能，支持高级数据生成、更新、删除等操作。

## 🚀 功能特性

### 核心数据库工具

- ✅ **listTables**: 列出数据库中所有表
- ✅ **describeTable**: 查看表结构和字段信息
- ✅ **insertData**: 高级测试数据生成
  - 🎯 **4种生成模式**: 简单固定值、分组、序列、组合模式
  - 🎲 **智能数据生成**: 使用 DataFaker 生成真实业务数据
  - 📊 **ENUM/SET 支持**: 自动提取数据库枚举值
  - 🔄 **事务安全**: 完整的 ACID 事务支持
- ✅ **updateData**: 智能数据更新
  - 🎯 **多条件更新**: 支持多种操作符（=, !=, >, <, >=, <=, IN, NOT IN, LIKE, NOT LIKE）
  - 📊 **批量更新**: 支持多个更新规则
  - 🔢 **记录限制**: 可配置的安全更新限制
  - 🔄 **事务安全**: 完整的 ACID 事务支持
- ✅ **deleteData**: 灵活数据删除
  - 🎯 **条件删除**: 支持多种操作符和复杂条件
  - 🗑️ **批量删除**: 支持多个删除规则
  - 🔢 **安全限制**: 可配置的最大删除记录数
  - 🔄 **事务安全**: 完整的 ACID 事务支持
- ✅ **executeQuery**: 安全 SQL 查询执行
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

#### 数据库连接配置
```bash
export DATABASE_TYPE=mysql
export HOST=your_host
export PORT=3306
export USER=your_username
export PASSWORD=your_password
export DATABASE=your_database
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

在 MCP 客户端（如 Claude Desktop）中配置此服务器：

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

## 📖 使用示例

### 1. 基础数据库操作

```java
// 列出所有表
listTables()

// 查看表结构
describeTable("users")

// 执行 SELECT 查询
executeQuery("SELECT * FROM users WHERE status = 'active'")
```

### 2. 高级数据生成

#### 简单固定值
```json
{
  "tableName": "users",
  "recordCount": 10,
  "fixedValuesJson": "{\"status\":1,\"type\":\"active\"}"
}
```

#### 分组生成
```json
{
  "tableName": "orders",
  "recordCount": 15,
  "fixedValuesJson": "{\"groups\":[{\"recordCount\":10,\"fixedValues\":{\"status\":\"pending\"}},{\"recordCount\":5,\"fixedValues\":{\"status\":\"completed\"}}]}"
}
```

#### 序列生成
```json
{
  "tableName": "products",
  "recordCount": 5,
  "fixedValuesJson": "{\"sequences\":{\"product_code\":{\"type\":\"CUSTOM_VALUES\",\"customValues\":[\"P001\",\"P002\",\"P003\",\"P004\",\"P005\"]}}}"
}
```

#### 组合模式
```json
{
  "tableName": "employees",
  "recordCount": 8,
  "fixedValuesJson": "{\"groups\":[{\"recordCount\":5,\"fixedValues\":{\"department\":\"IT\"}},{\"recordCount\":3,\"fixedValues\":{\"department\":\"HR\"}}],\"sequences\":{\"employee_id\":{\"type\":\"INCREMENT\",\"startValue\":1000,\"step\":1}}}"
}
```

### 3. 数据更新操作

#### 简单更新
```json
{
  "tableName": "users",
  "updateConfigJson": "{\"updateRules\":[{\"conditions\":[{\"field\":\"id\",\"operator\":\"=\",\"value\":1}],\"updateData\":{\"status\":\"active\"}}]}"
}
```

#### 条件更新
```json
{
  "tableName": "orders",
  "updateConfigJson": "{\"updateRules\":[{\"conditions\":[{\"field\":\"status\",\"operator\":\"=\",\"value\":\"pending\"},{\"field\":\"created_time\",\"operator\":\"<\",\"value\":\"2024-01-01\"}],\"updateData\":{\"status\":\"expired\"},\"maxRecords\":100}]}"
}
```

#### 多规则更新
```json
{
  "tableName": "products",
  "updateConfigJson": "{\"updateRules\":[{\"conditions\":[{\"field\":\"category\",\"operator\":\"=\",\"value\":\"electronics\"}],\"updateData\":{\"discount\":0.1}},{\"conditions\":[{\"field\":\"category\",\"operator\":\"=\",\"value\":\"books\"}],\"updateData\":{\"discount\":0.05}}]}"
}
```

### 4. 数据删除操作

#### 简单删除
```json
{
  "tableName": "users",
  "deleteConfigJson": "{\"deleteRules\":[{\"conditions\":[{\"field\":\"id\",\"operator\":\"=\",\"value\":1}],\"maxRecords\":1}]}"
}
```

#### 条件删除
```json
{
  "tableName": "logs",
  "deleteConfigJson": "{\"deleteRules\":[{\"conditions\":[{\"field\":\"status\",\"operator\":\"=\",\"value\":\"inactive\"},{\"field\":\"last_login\",\"operator\":\"<\",\"value\":\"2023-01-01\"}],\"maxRecords\":50}]}"
}
```

#### 多规则删除
```json
{
  "tableName": "temp_data",
  "deleteConfigJson": "{\"deleteRules\":[{\"conditions\":[{\"field\":\"status\",\"operator\":\"=\",\"value\":\"deleted\"}],\"maxRecords\":100},{\"conditions\":[{\"field\":\"type\",\"operator\":\"IN\",\"values\":[\"temp\",\"test\"]}],\"maxRecords\":50}]}"
}
```

## 🏗️ 架构设计

### 核心架构组件

```
DatabaseMcpApplication (主应用)
├── DatabaseService (服务层)
│   ├── listTables()
│   ├── describeTable()
│   ├── insertData()
│   ├── updateData()
│   ├── deleteData()
│   └── executeQuery()
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
│   │   │       │   ├── DatabaseConfig.java
│   │   │       │   └── DatabaseOperationConfig.java
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

### 常见问题

#### 1. MCP 连接问题
**现象**: 无法连接到 MCP 服务器
**解决**: 检查 Java 版本，确保使用 Java 21+，检查环境变量配置

#### 2. 数据库连接失败
**现象**: 无法连接到数据库
**解决**: 验证数据库连接参数，检查网络连接，确认数据库服务运行正常

#### 3. 内存不足
**现象**: 应用启动缓慢或内存溢出
**解决**: 使用推荐的 JVM 参数配置，调整内存分配

#### 4. SQL 注入警告
**现象**: SQL 查询被拒绝
**解决**: 使用参数化查询，避免直接拼接 SQL 语句

#### 5. 操作权限被拒绝
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
