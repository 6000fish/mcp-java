package com.mcp.protocol;

/**
 * MCP 协议方法常量
 */
public final class McpMethods {

    private McpMethods() {}

    // 初始化
    public static final String INITIALIZE = "initialize";
    public static final String INITIALIZED = "notifications/initialized";

    // 工具相关
    public static final String TOOLS_LIST = "tools/list";
    public static final String TOOLS_CALL = "tools/call";

    // 资源相关
    public static final String RESOURCES_LIST = "resources/list";
    public static final String RESOURCES_READ = "resources/read";
    public static final String RESOURCES_SUBSCRIBE = "resources/subscribe";
    public static final String RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";
    public static final String RESOURCES_UPDATED = "notifications/resources/updated";

    // Prompt 相关
    public static final String PROMPTS_LIST = "prompts/list";
    public static final String PROMPTS_GET = "prompts/get";

    // 补全相关
    public static final String COMPLETION_COMPLETE = "completion/complete";

    // 日志相关
    public static final String LOGGING_SET_LEVEL = "logging/setLevel";
    public static final String LOG_MESSAGE = "notifications/message";

    // 根目录相关
    public static final String ROOTS_LIST = "roots/list";
    public static final String ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

    // 采样相关
    public static final String SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

    // Ping
    public static final String PING = "ping";

    // 进度通知
    public static final String PROGRESS = "notifications/progress";

    // 取消通知
    public static final String CANCELLED = "notifications/cancelled";
}
