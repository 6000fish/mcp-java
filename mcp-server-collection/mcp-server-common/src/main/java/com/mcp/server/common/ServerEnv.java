package com.mcp.server.common;

/**
 * Environment variable helpers for standalone MCP server launchers.
 */
public final class ServerEnv {

    private ServerEnv() {
    }

    public static String get(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    public static int getInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value for " + name + ": " + value, e);
        }
    }
}
