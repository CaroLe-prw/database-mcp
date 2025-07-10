package com.carole.database.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.carole.database.mcp.service.DatabaseService;

/**
 * Main application class for Database MCP Server
 * 
 * This Spring Boot application provides Model Context Protocol (MCP) server capabilities for intelligent database
 * querying. It excludes default DataSource auto-configuration to use custom Druid connection pool settings.
 * 
 * @author CaroLe
 * @Date 2025/7/6
 * @Description Database MCP Server main application that integrates Spring AI MCP tools with database operations
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, DruidDataSourceAutoConfigure.class})
public class DatabaseMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(DatabaseService databaseService) {
        return MethodToolCallbackProvider.builder().toolObjects(databaseService).build();
    }

}
