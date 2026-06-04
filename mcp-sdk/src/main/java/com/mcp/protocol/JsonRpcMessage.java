package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 消息基类。
 * <p>
 * MCP 协议基于 JSON-RPC 2.0 规范进行通信，本类是所有消息的统一模型，
 * 涵盖请求（request）、响应（response）和通知（notification）三种消息类型。
 * 通过静态工厂方法可以方便地创建各类消息实例。
 * </p>
 *
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 规范</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcMessage {

    /** JSON-RPC 协议版本号，固定为 "2.0" */
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    /** 消息唯一标识，用于请求与响应的关联匹配。通知消息中该字段为 null */
    @JsonProperty("id")
    private Object id;

    /** 要调用的方法名称（如 "initialize"、"tools/list" 等），仅请求和通知消息包含此字段 */
    @JsonProperty("method")
    private String method;

    /** 方法调用参数，可以是对象或数组类型，仅请求和通知消息包含此字段 */
    @JsonProperty("params")
    private Object params;

    /** 方法调用的成功返回结果，仅成功响应消息包含此字段 */
    @JsonProperty("result")
    private Object result;

    /** 方法调用的错误信息，仅失败响应消息包含此字段 */
    @JsonProperty("error")
    private JsonRpcError error;

    /**
     * 创建请求消息。
     * <p>
     * 请求消息包含 id、method 和 params，期望对端返回一个响应。
     * </p>
     *
     * @param id     消息唯一标识，通常为自增整数或 UUID
     * @param method 要调用的 MCP 方法名称
     * @param params 方法参数，可以为 null
     * @return 新的请求消息实例
     */
    public static JsonRpcMessage request(Object id, String method, Object params) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(id);
        msg.setMethod(method);
        msg.setParams(params);
        return msg;
    }

    /**
     * 创建成功响应消息。
     * <p>
     * 成功响应包含 id 和 result，表示对应请求已成功处理。
     * </p>
     *
     * @param id     对应请求消息的唯一标识
     * @param result 方法调用的返回结果
     * @return 新的成功响应消息实例
     */
    public static JsonRpcMessage successResponse(Object id, Object result) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(id);
        msg.setResult(result);
        return msg;
    }

    /**
     * 创建错误响应消息。
     * <p>
     * 错误响应包含 id 和 error，表示对应请求处理失败。
     * </p>
     *
     * @param id      对应请求消息的唯一标识
     * @param code    错误码（如 -32600 表示无效请求，-32601 表示方法未找到等）
     * @param message 错误描述信息
     * @return 新的错误响应消息实例
     */
    public static JsonRpcMessage errorResponse(Object id, int code, String message) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(id);
        msg.setError(new JsonRpcError(code, message));
        return msg;
    }

    /**
     * 创建通知消息。
     * <p>
     * 通知消息不包含 id 字段，对端收到后不需要返回响应。
     * 通常用于状态变更通知、进度更新等场景。
     * </p>
     *
     * @param method 通知方法名称
     * @param params 通知参数，可以为 null
     * @return 新的通知消息实例
     */
    public static JsonRpcMessage notification(String method, Object params) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setMethod(method);
        msg.setParams(params);
        return msg;
    }

    /**
     * 判断当前消息是否为请求消息。
     * <p>
     * 请求消息的特征：同时包含 method 和 id 字段。
     * </p>
     *
     * @return 如果是请求消息返回 true，否则返回 false
     */
    @JsonIgnore
    public boolean isRequest() {
        return method != null && id != null;
    }

    /**
     * 判断当前消息是否为通知消息。
     * <p>
     * 通知消息的特征：包含 method 但不包含 id 字段。
     * </p>
     *
     * @return 如果是通知消息返回 true，否则返回 false
     */
    @JsonIgnore
    public boolean isNotification() {
        return method != null && id == null;
    }

    /**
     * 判断当前消息是否为响应消息。
     * <p>
     * 响应消息的特征：包含 id 但不包含 method 字段。
     * </p>
     *
     * @return 如果是响应消息返回 true，否则返回 false
     */
    @JsonIgnore
    public boolean isResponse() {
        return id != null && method == null;
    }

    /**
     * JSON-RPC 错误对象。
     * <p>
     * 当请求处理失败时，响应中会包含此对象，描述错误的类型和详情。
     * </p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JsonRpcError {
        /** 错误码，遵循 JSON-RPC 规范定义的错误代码 */
        private int code;
        /** 错误描述信息，简要说明错误原因 */
        private String message;
        /** 附加错误数据，可选字段，用于携带额外的错误详情 */
        private Object data;

        /**
         * 创建仅包含错误码和描述信息的错误对象。
         *
         * @param code    错误码
         * @param message 错误描述信息
         */
        public JsonRpcError(int code, String message) {
            this.code = code;
            this.message = message;
            this.data = null;
        }
    }
}
