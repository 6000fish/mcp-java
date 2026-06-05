package com.mcp.example.template;

import com.mcp.annotation.McpPrompt;
import com.mcp.annotation.McpResource;
import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;

import java.time.LocalDateTime;
import java.util.Map;

@McpServer(name = "custom-server-template", version = "1.0.0")
public class CustomMcpServer {

    @McpTool(name = "hello", description = "Say hello to a user")
    public String hello(
            @Param(name = "name", description = "Name to greet") String name
    ) {
        return "Hello, " + name + "! Your custom MCP server is running.";
    }

    @McpTool(name = "get_project_status", description = "Return sample project status as JSON")
    public ToolCallResult getProjectStatus(
            @Param(name = "project", description = "Project name", required = false) String project
    ) {
        String projectName = project == null || project.isBlank() ? "custom-mcp-server" : project;
        return ToolCallResult.json(Map.of(
                "project", projectName,
                "status", "ready",
                "server", "custom-server-template",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @McpResource(uri = "template://server-info", name = "serverInfo", description = "Template server information", mimeType = "application/json")
    public String serverInfo() {
        return """
                {
                  "name": "custom-server-template",
                  "description": "A copyable Java MCP Server template",
                  "tools": ["hello", "get_project_status"],
                  "prompts": ["draft_release_note"]
                }
                """;
    }

    @McpPrompt(name = "draft_release_note", description = "Draft a short release note for a project change")
    public String draftReleaseNote(
            @Param(name = "feature", description = "Feature or change to describe") String feature,
            @Param(name = "audience", description = "Target audience", required = false) String audience
    ) {
        String targetAudience = audience == null || audience.isBlank() ? "Java developers" : audience;
        return "Draft a concise release note for " + targetAudience + " about this change: " + feature;
    }
}
