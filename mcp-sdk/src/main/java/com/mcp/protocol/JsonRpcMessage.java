package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON-RPC 2.0 消息基类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcMessage {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Object id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Object params;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private JsonRpcError error;

    /**
     * 创建请求消息
     */
    public static JsonRpcMessage request(Object id, String method, Object params) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(id);
        msg.setMethod(method);
        msg.setParams(params);
        return msg;
    }

    /**
     * 创建成功响应
     */
    public static JsonRpcMessage successResponse(Object id, Object result) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(id);
        msg.setResult(result);
        return msg;
    }

    /**
     * 创建错误响应
     */
    public static JsonRpcMessage errorResponse(Object id, int code, String message) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setId(id);
        msg.setError(new JsonRpcError(code, message));
        return msg;
    }

    /**
     * 创建通知消息（无 id）
     */
    public static JsonRpcMessage notification(String method, Object params) {
        JsonRpcMessage msg = new JsonRpcMessage();
        msg.setMethod(method);
        msg.setParams(params);
        return msg;
    }

    /**
     * 判断是否为请求（有 method）
     */
    public boolean isRequest() {
        return method != null && id != null;
    }

    /**
     * 判断是否为通知（有 method，无 id）
     */
    public boolean isNotification() {
        return method != null && id == null;
    }

    /**
     * 判断是否为响应（有 id，无 method）
     */
    public boolean isResponse() {
        return id != null && method == null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JsonRpcError {
        private int code;
        private String message;
        private Object data;

        public JsonRpcError(int code, String message) {
            this.code = code;
            this.message = message;
            this.data = null;
        }
    }
}
