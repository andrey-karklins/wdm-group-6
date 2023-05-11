package app.controllers;

import app.models.Order;
import app.services.OrderService;

import java.util.*;

public class OrderApiController {
//    private OrderService orderService;
    private static final OrderService orderService = new OrderService();

//    public OrderApiController(OrderService orderService) {
//        this.orderService = orderService;
//    }

    public Map<String, Object> createOrder(String userId) {
        Order order = new Order();
        order.user_id = UUID.fromString(userId);
        order = orderService.createOrder(order);
        return Map.of("order_id", order.order_id);
    }

    public String cancelOrder(String orderId) {
        UUID orderUUID = UUID.fromString(orderId);
        orderService.deleteOrder(orderUUID);
        return "Success";
    }

    public Map<String, Object> findOrder(String orderId) {
        UUID orderUUID = UUID.fromString(orderId);
        Order order = orderService.findOrder(orderUUID);
        if (order != null) {
            return Map.of("order_id", order.order_id,
                    "paid", order.paid,
                    "items", order.items,
                    "user_id", order.user_id,
                    "total_cost", order.total_cost);
        } else {
            return Map.of("error", "Order not found");
        }
    }

    public String addItem(String orderId, String itemId) {
        UUID orderUUID = UUID.fromString(orderId);
        UUID itemUUID = UUID.fromString(itemId);
        orderService.addItemToOrder(orderUUID, itemUUID);
        return "Success";
    }

    public String removeItem(String orderId, String itemId) {
        UUID orderUUID = UUID.fromString(orderId);
        UUID itemUUID = UUID.fromString(itemId);
        orderService.removeItemFromOrder(orderUUID, itemUUID);
        return "Success";
    }

    public String checkout(String orderId) {
        UUID orderUUID = UUID.fromString(orderId);
        boolean result = orderService.checkoutOrder(orderUUID);
        if (result) {
            return "Success";
        } else {
            return "Failed";
        }
    }
}
