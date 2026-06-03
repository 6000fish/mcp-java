package com.mcp.example.business;

import com.mcp.annotation.McpPrompt;
import com.mcp.annotation.McpResource;
import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@McpServer(name = "business-server-example", version = "1.0.0")
public class BusinessMcpServer {

    private final Map<String, CustomerProfile> customers = Map.of(
            "CUST-1001", new CustomerProfile("CUST-1001", "Alice Chen", "gold", "alice@example.com"),
            "CUST-1002", new CustomerProfile("CUST-1002", "Bob Li", "silver", "bob@example.com"),
            "CUST-1003", new CustomerProfile("CUST-1003", "Carol Wang", "platinum", "carol@example.com")
    );

    private final List<OrderSummary> orders = List.of(
            new OrderSummary("ORD-9001", "CUST-1001", LocalDate.of(2026, 5, 20), new BigDecimal("129.90"), "paid"),
            new OrderSummary("ORD-9002", "CUST-1001", LocalDate.of(2026, 5, 28), new BigDecimal("59.00"), "shipped"),
            new OrderSummary("ORD-9003", "CUST-1002", LocalDate.of(2026, 5, 30), new BigDecimal("299.50"), "processing"),
            new OrderSummary("ORD-9004", "CUST-1003", LocalDate.of(2026, 6, 1), new BigDecimal("899.00"), "paid")
    );

    @McpTool(name = "get_customer_profile", description = "Get a sample customer profile by customer id")
    public ToolCallResult getCustomerProfile(
            @Param(name = "customerId", description = "Customer id, for example CUST-1001") String customerId
    ) {
        CustomerProfile customer = customers.get(customerId);
        if (customer == null) {
            return ToolCallResult.error("Customer not found: " + customerId);
        }
        return ToolCallResult.json(Map.of(
                "customerId", customer.customerId(),
                "name", customer.name(),
                "tier", customer.tier(),
                "email", customer.email()
        ));
    }

    @McpTool(name = "list_recent_orders", description = "List recent sample orders, optionally filtered by customer id")
    public ToolCallResult listRecentOrders(
            @Param(name = "customerId", description = "Optional customer id filter, for example CUST-1001", required = false) String customerId
    ) {
        List<Map<String, Object>> result = orders.stream()
                .filter(order -> customerId == null || customerId.isBlank() || order.customerId().equals(customerId))
                .map(order -> Map.<String, Object>of(
                        "orderId", order.orderId(),
                        "customerId", order.customerId(),
                        "orderDate", order.orderDate().toString(),
                        "amount", order.amount(),
                        "status", order.status()
                ))
                .toList();
        return ToolCallResult.json(result);
    }

    @McpTool(name = "create_support_ticket", description = "Create a sample support ticket for a customer")
    public ToolCallResult createSupportTicket(
            @Param(name = "customerId", description = "Customer id, for example CUST-1001") String customerId,
            @Param(name = "subject", description = "Support ticket subject") String subject,
            @Param(name = "priority", description = "Ticket priority: low, normal, high") String priority
    ) {
        if (!customers.containsKey(customerId)) {
            return ToolCallResult.error("Customer not found: " + customerId);
        }
        String ticketId = "TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return ToolCallResult.json(Map.of(
                "ticketId", ticketId,
                "customerId", customerId,
                "subject", subject,
                "priority", priority,
                "status", "open"
        ));
    }

    @McpTool(name = "calculate_loyalty_discount", description = "Calculate a sample loyalty discount from customer tier and order amount")
    public ToolCallResult calculateLoyaltyDiscount(
            @Param(name = "customerId", description = "Customer id, for example CUST-1001") String customerId,
            @Param(name = "orderAmount", description = "Order amount before discount") double orderAmount
    ) {
        CustomerProfile customer = customers.get(customerId);
        if (customer == null) {
            return ToolCallResult.error("Customer not found: " + customerId);
        }
        BigDecimal amount = BigDecimal.valueOf(orderAmount);
        BigDecimal rate = discountRate(customer.tier());
        BigDecimal discount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return ToolCallResult.json(Map.of(
                "customerId", customerId,
                "tier", customer.tier(),
                "orderAmount", amount,
                "discountRate", rate,
                "discountAmount", discount,
                "finalAmount", amount.subtract(discount).setScale(2, RoundingMode.HALF_UP)
        ));
    }

    @McpResource(uri = "business://catalog", name = "businessCatalog", description = "Business example catalog", mimeType = "application/json")
    public String businessCatalog() {
        return """
                {
                  "customers": "Sample customer profiles keyed by customerId",
                  "orders": "Recent order summaries for demo customers",
                  "tools": [
                    "get_customer_profile",
                    "list_recent_orders",
                    "create_support_ticket",
                    "calculate_loyalty_discount"
                  ]
                }
                """;
    }

    @McpPrompt(name = "draft_customer_reply", description = "Draft a support reply for a customer issue")
    public String draftCustomerReply(
            @Param(name = "customerId", description = "Customer id, for example CUST-1001") String customerId,
            @Param(name = "issue", description = "Customer issue summary") String issue
    ) {
        CustomerProfile customer = customers.get(customerId);
        String customerName = customer == null ? customerId : customer.name();
        return "Draft a concise and friendly support reply for " + customerName + ". Issue: " + issue;
    }

    private BigDecimal discountRate(String tier) {
        return switch (tier) {
            case "platinum" -> new BigDecimal("0.15");
            case "gold" -> new BigDecimal("0.10");
            case "silver" -> new BigDecimal("0.05");
            default -> BigDecimal.ZERO;
        };
    }

    private record CustomerProfile(String customerId, String name, String tier, String email) {
    }

    private record OrderSummary(String orderId, String customerId, LocalDate orderDate, BigDecimal amount, String status) {
    }
}
