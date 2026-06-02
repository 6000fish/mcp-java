package com.mcp.protocol;

/**
 * MCP 协议方法常量定义。
 * <p>
 * 集中定义了 MCP（Model Context Protocol）协议中所有标准方法名称的字符串常量，
 * 包括初始化、工具（Tools）、资源（Resources）、提示（Prompts）、
 * 补全（Completions）、日志（Logging）、根目录（Roots）、采样（Sampling）等核心能力。
 * </p>
 * <p>
 * 这些常量用于 JSON-RPC 消息中的 method 字段，确保通信双方使用一致的方法名称。
 * 方法分为两类：
 * <ul>
 *   <li>请求方法：需要对端返回响应（如 "tools/list"）</li>
 *   <li>通知方法：以 "notifications/" 前缀开头，不需要返回响应（如 "notifications/initialized"）</li>
 * </ul>
 * </p>
 */
public final class McpMethods {

    /** 私有构造方法，防止实例化工具类 */
    private McpMethods() {}

    // ==================== 初始化相关 ====================

    /** 初始化请求：客户端向服务端发起的握手请求，协商协议版本和能力 */
    public static final String INITIALIZE = "initialize";
    /** 初始化完成通知：客户端告知服务端初始化已完成，可以开始正式通信 */
    public static final String INITIALIZED = "notifications/initialized";

    // ==================== 工具（Tools）相关 ====================

    /** 工具列表请求：获取服务端提供的所有可用工具列表 */
    public static final String TOOLS_LIST = "tools/list";
    /** 工具调用请求：请求服务端执行指定工具 */
    public static final String TOOLS_CALL = "tools/call";

    // ==================== 资源（Resources）相关 ====================

    /** 资源列表请求：获取服务端提供的所有可用资源列表 */
    public static final String RESOURCES_LIST = "resources/list";
    /** 资源读取请求：读取指定 URI 的资源内容 */
    public static final String RESOURCES_READ = "resources/read";
    /** 资源订阅请求：订阅指定资源的变更通知 */
    public static final String RESOURCES_SUBSCRIBE = "resources/subscribe";
    /** 资源取消订阅请求：取消对指定资源的变更订阅 */
    public static final String RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";
    /** 资源更新通知：服务端通知客户端某个已订阅的资源内容已变更 */
    public static final String RESOURCES_UPDATED = "notifications/resources/updated";

    // ==================== 提示（Prompts）相关 ====================

    /** 提示列表请求：获取服务端提供的所有可用提示模板列表 */
    public static final String PROMPTS_LIST = "prompts/list";
    /** 提示获取请求：获取指定提示模板的详细内容 */
    public static final String PROMPTS_GET = "prompts/get";

    // ==================== 补全（Completions）相关 ====================

    /** 补全请求：请求服务端对提示参数进行自动补全 */
    public static final String COMPLETION_COMPLETE = "completion/complete";

    // ==================== 日志（Logging）相关 ====================

    /** 设置日志级别请求：客户端请求服务端调整日志输出级别 */
    public static final String LOGGING_SET_LEVEL = "logging/setLevel";
    /** 日志消息通知：服务端向客户端发送的日志消息 */
    public static final String LOG_MESSAGE = "notifications/message";

    // ==================== 根目录（Roots）相关 ====================

    /** 根目录列表请求：客户端告知服务端其可访问的文件系统根目录 */
    public static final String ROOTS_LIST = "roots/list";
    /** 根目录变更通知：客户端通知服务端其根目录列表已发生变更 */
    public static final String ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

    // ==================== 采样（Sampling）相关 ====================

    /** 创建采样消息请求：服务端请求客户端进行 LLM 采样（即让客户端调用大模型生成回复） */
    public static final String SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

    // ==================== 其他 ====================

    /** 心跳检测请求：用于检测对端是否存活，对端需返回空响应 */
    public static final String PING = "ping";

    /** 进度通知：在长时间操作过程中向对端报告当前进度 */
    public static final String PROGRESS = "notifications/progress";

    /** 取消通知：通知对端某个正在进行的操作已被取消 */
    public static final String CANCELLED = "notifications/cancelled";
}
