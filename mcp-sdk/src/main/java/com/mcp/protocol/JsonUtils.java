package com.mcp.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * JSON 序列化/反序列化工具类。
 * <p>
 * 基于 Jackson {@link ObjectMapper} 封装的 JSON 工具类，提供对象与 JSON 字符串之间的转换功能。
 * 内部使用单例模式共享同一个 {@link ObjectMapper} 实例，已禁用空 Bean 序列化异常。
 * </p>
 * <p>
 * 主要用途：
 * <ul>
 *   <li>MCP 协议消息的序列化与反序列化</li>
 *   <li>工具调用参数和结果的 JSON 转换</li>
 *   <li>资源内容的 JSON 格式化</li>
 * </ul>
 * </p>
 */
public final class JsonUtils {

    /** 共享的 ObjectMapper 实例，已禁用空 Bean 序列化异常 */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    /** 私有构造方法，防止实例化工具类 */
    private JsonUtils() {}

    /**
     * 获取共享的 ObjectMapper 实例。
     * <p>
     * 返回内部单例的 ObjectMapper，可用于自定义的序列化/反序列化操作。
     * </p>
     *
     * @return 共享的 ObjectMapper 实例
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param obj 要序列化的对象，可以是任意 Java 对象
     * @return JSON 格式的字符串
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象。
     *
     * @param json  JSON 格式的字符串
     * @param clazz 目标对象的类型
     * @param <T>   目标对象的泛型类型
     * @return 反序列化后的对象实例
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize from JSON", e);
        }
    }
}
