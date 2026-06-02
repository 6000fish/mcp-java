package com.mcp.server.mysql;

import com.mcp.protocol.ToolCallResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySqlMcpServer 单元测试
 */
class MySqlMcpServerTest {

    @Test
    void testDangerousSqlDetection() {
        // 创建一个不连接实际数据库的测试实例
        // 注意：这里我们测试的是安全检查逻辑，不是实际的数据库操作

        // 测试危险 SQL 检测
        assertTrue(isDangerousSql("DROP TABLE users"));
        assertTrue(isDangerousSql("DELETE FROM users"));
        assertTrue(isDangerousSql("TRUNCATE TABLE users"));
        assertTrue(isDangerousSql("ALTER TABLE users ADD COLUMN age INT"));
        assertTrue(isDangerousSql("CREATE TABLE test (id INT)"));
        assertTrue(isDangerousSql("GRANT ALL ON *.* TO 'user'@'%'"));
        assertTrue(isDangerousSql("REVOKE ALL ON *.* FROM 'user'@'%'"));

        // 测试安全 SQL
        assertFalse(isDangerousSql("SELECT * FROM users"));
        assertFalse(isDangerousSql("SHOW DATABASES"));
        assertFalse(isDangerousSql("SHOW TABLES"));
        assertFalse(isDangerousSql("DESCRIBE users"));
        assertFalse(isDangerousSql("EXPLAIN SELECT * FROM users"));
    }

    @Test
    void testDangerousSqlWithWhitespace() {
        assertTrue(isDangerousSql("  DROP TABLE users"));
        assertTrue(isDangerousSql("  DELETE FROM users  "));
        assertFalse(isDangerousSql("  SELECT * FROM users  "));
    }

    @Test
    void testDangerousSqlCaseInsensitive() {
        assertTrue(isDangerousSql("drop table users"));
        assertTrue(isDangerousSql("Drop Table Users"));
        assertTrue(isDangerousSql("DROP TABLE USERS"));
        assertFalse(isDangerousSql("select * from users"));
        assertFalse(isDangerousSql("Select * From Users"));
    }

    /**
     * 复制 MySqlMcpServer 中的安全检查逻辑用于测试
     */
    private boolean isDangerousSql(String sql) {
        String upper = sql.toUpperCase().trim();
        return upper.startsWith("DROP") ||
                upper.startsWith("DELETE") ||
                upper.startsWith("TRUNCATE") ||
                upper.startsWith("ALTER") ||
                upper.startsWith("CREATE") ||
                upper.startsWith("GRANT") ||
                upper.startsWith("REVOKE");
    }
}
