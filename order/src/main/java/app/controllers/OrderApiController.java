package app.controllers;

import app.services.OrderService;

import java.util.List;
import java.util.Map;

public class OrderApiController {
    public Map<String, Object> createOrder(String userId) {
        Map<String, Object> res = Map.of("order_id", 123);
        return res;
    }

    public String cancelOrder(String orderId) {
        return "Success";
    }

    public Map<String, Object> findOrder(String orderId) {
        OrderService.sendEvent("order-created", "test");
        Map<String, Object> res = Map.of("order_id", orderId,
                "paid", true,
                "items", List.of("123", "456"),
                "user_id", "123",
                "total_cost", 100);
        return res;
    }

    public String addItem(String orderId, String itemId) {
        return "Success";
    }

    public String removeItem(String orderId, String itemId) {
        return "Success";
    }

    public String checkout(String orderId) {
        return "Success";
    }
}
