package com.mcp.server.elasticsearch;

import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;
import com.mcp.protocol.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Elasticsearch MCP Server 实现
 *
 * 提供 Elasticsearch 操作的 MCP 工具，包括：
 * - search：搜索文档
 * - index_document：索引文档
 * - get_document：获取文档
 * - delete_document：删除文档
 * - list_indices：列出所有索引
 * - get_index_mapping：获取索引映射
 * - get_index_stats：获取索引统计
 * - count：统计文档数量
 * - bulk：批量操作
 * - cluster_health：获取集群健康状态
 *
 * 使用 JDK HttpURLConnection 发送 HTTP 请求，无需外部依赖。
 * 支持 Basic Auth 认证。
 *
 * @author MCP Java SDK
 */
@McpServer(name = "elasticsearch-server", version = "1.0.0")
public class ElasticsearchMcpServer {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchMcpServer.class);

    /** Elasticsearch 基础 URL */
    private final String baseUrl;

    /** Basic Auth 认证头（可选） */
    private final String authHeader;

    /** JSON 序列化器 */
    private final ObjectMapper objectMapper;

    /**
     * 创建 Elasticsearch MCP Server（无认证）
     *
     * @param host   主机地址
     * @param port   端口号
     * @param scheme 协议（http 或 https）
     */
    public ElasticsearchMcpServer(String host, int port, String scheme) {
        this(host, port, scheme, null, null);
    }

    /**
     * 创建 Elasticsearch MCP Server（带认证）
     *
     * @param host     主机地址
     * @param port     端口号
     * @param scheme   协议（http 或 https）
     * @param username 用户名（可选）
     * @param password 密码（可选）
     */
    public ElasticsearchMcpServer(String host, int port, String scheme, String username, String password) {
        this.baseUrl = String.format("%s://%s:%d", scheme, host, port);
        this.objectMapper = JsonUtils.mapper();

        if (username != null && password != null) {
            String credentials = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));
            this.authHeader = "Basic " + credentials;
        } else {
            this.authHeader = null;
        }

        log.info("Elasticsearch server initialized: {}", baseUrl);
    }

    /**
     * 搜索文档
     *
     * 使用 Query DSL 搜索指定索引中的文档。
     *
     * @param index 索引名称
     * @param query 查询 DSL（JSON 格式）
     * @return 搜索结果
     */
    @McpTool(name = "search", description = "Search documents with query DSL")
    public ToolCallResult search(
            @Param(name = "index", description = "Index name") String index,
            @Param(name = "query", description = "Query DSL (JSON)") String query
    ) {
        String endpoint = index != null ? "/" + index + "/_search" : "/_search";
        String body = query != null ? query : "{\"query\":{\"match_all\":{}}}";
        return executeRequest("POST", endpoint, body);
    }

    /**
     * 索引文档
     *
     * 将文档添加到指定索引。
     *
     * @param index    索引名称
     * @param id       文档 ID（可选）
     * @param document 文档内容（JSON 格式）
     * @return 索引结果
     */
    @McpTool(name = "index_document", description = "Index a document")
    public ToolCallResult indexDocument(
            @Param(name = "index", description = "Index name") String index,
            @Param(name = "id", description = "Document ID (optional)", required = false) String id,
            @Param(name = "document", description = "Document content (JSON)") String document
    ) {
        String endpoint = id != null ? "/" + index + "/_doc/" + id : "/" + index + "/_doc";
        return executeRequest("POST", endpoint, document);
    }

    /**
     * 获取文档
     *
     * 根据 ID 获取指定索引中的文档。
     *
     * @param index 索引名称
     * @param id    文档 ID
     * @return 文档内容
     */
    @McpTool(name = "get_document", description = "Get a document by ID")
    public ToolCallResult getDocument(
            @Param(name = "index", description = "Index name") String index,
            @Param(name = "id", description = "Document ID") String id
    ) {
        return executeRequest("GET", "/" + index + "/_doc/" + id, null);
    }

    /**
     * 删除文档
     *
     * 根据 ID 删除指定索引中的文档。
     *
     * @param index 索引名称
     * @param id    文档 ID
     * @return 删除结果
     */
    @McpTool(name = "delete_document", description = "Delete a document by ID")
    public ToolCallResult deleteDocument(
            @Param(name = "index", description = "Index name") String index,
            @Param(name = "id", description = "Document ID") String id
    ) {
        return executeRequest("DELETE", "/" + index + "/_doc/" + id, null);
    }

    /**
     * 列出所有索引
     *
     * 获取集群中所有索引的列表。
     *
     * @return 索引列表
     */
    @McpTool(name = "list_indices", description = "List all indices")
    public ToolCallResult listIndices() {
        return executeRequest("GET", "/_cat/indices?v&s=index", null);
    }

    /**
     * 获取索引映射
     *
     * 获取指定索引的字段映射定义。
     *
     * @param index 索引名称
     * @return 索引映射信息
     */
    @McpTool(name = "get_index_mapping", description = "Get index mapping")
    public ToolCallResult getIndexMapping(
            @Param(name = "index", description = "Index name") String index
    ) {
        return executeRequest("GET", "/" + index + "/_mapping", null);
    }

    /**
     * 获取索引统计
     *
     * 获取指定索引的统计信息，包括文档数量、存储大小等。
     *
     * @param index 索引名称
     * @return 索引统计信息
     */
    @McpTool(name = "get_index_stats", description = "Get index statistics")
    public ToolCallResult getIndexStats(
            @Param(name = "index", description = "Index name") String index
    ) {
        return executeRequest("GET", "/" + index + "/_stats", null);
    }

    /**
     * 统计文档数量
     *
     * 统计指定索引中匹配查询的文档数量。
     *
     * @param index 索引名称
     * @param query 查询条件（可选）
     * @return 文档数量
     */
    @McpTool(name = "count", description = "Count documents matching a query")
    public ToolCallResult count(
            @Param(name = "index", description = "Index name") String index,
            @Param(name = "query", description = "Query (optional)", required = false) String query
    ) {
        String body = query != null ? query : "{\"query\":{\"match_all\":{}}}";
        return executeRequest("POST", "/" + index + "/_count", body);
    }

    /**
     * 批量操作
     *
     * 执行批量索引、更新或删除操作。
     *
     * @param body 批量操作请求体（NDJSON 格式）
     * @return 批量操作结果
     */
    @McpTool(name = "bulk", description = "Bulk operations")
    public ToolCallResult bulk(
            @Param(name = "body", description = "Bulk request body (NDJSON)") String body
    ) {
        return executeRequest("POST", "/_bulk", body + "\n");
    }

    /**
     * 获取集群健康状态
     *
     * 获取 Elasticsearch 集群的健康状态信息。
     *
     * @return 集群健康状态
     */
    @McpTool(name = "cluster_health", description = "Get cluster health status")
    public ToolCallResult clusterHealth() {
        return executeRequest("GET", "/_cluster/health", null);
    }

    /**
     * 执行 HTTP 请求
     *
     * 向 Elasticsearch 发送 HTTP 请求并返回结果。
     *
     * @param method  HTTP 方法
     * @param endpoint API 端点
     * @param body    请求体（可选）
     * @return 响应结果
     */
    private ToolCallResult executeRequest(String method, String endpoint, String body) {
        try {
            URL url = new URL(baseUrl + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);

            if (authHeader != null) {
                conn.setRequestProperty("Authorization", authHeader);
            }

            // 发送请求体
            if (body != null && !body.isEmpty()) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            // 读取响应
            int responseCode = conn.getResponseCode();
            InputStream is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = is != null ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "";

            if (responseCode >= 200 && responseCode < 300) {
                return ToolCallResult.success(response);
            } else {
                return ToolCallResult.error("Elasticsearch error (HTTP " + responseCode + "): " + response);
            }

        } catch (Exception e) {
            log.error("Elasticsearch request failed", e);
            return ToolCallResult.error("Request failed: " + e.getMessage());
        }
    }
}
