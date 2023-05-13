package app.controllers;

import app.services.OrderService;
import app.models.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


public class OrderApiController {
    private static final OrderService orderService = new OrderService();

    public String createOrder(String userId) {
        UUID userIdUUID = UUID.fromString(userId);
        UUID orderId = orderService.createOrder(userIdUUID);
        Map<String, String> res = Map.of("user_id", userId, "order_id", orderId.toString());
        return "user id: " + userId + " " + "order id: " + orderId.toString();
    }

    public String cancelOrder(String orderId) {
        UUID orderIdUUID = UUID.fromString(orderId);
        boolean result = orderService.cancelOrder(orderIdUUID);
        return result ? "Success" : "Failure";
    }

    public Map<String, Object> findOrder(String orderId) {
        UUID OrderId = UUID.fromString(orderId);
        Order result = OrderService.findOrderById(OrderId);
        List<UUID> items = result.items;
        if(items == null) {
            items = new ArrayList<UUID>();
        }
        List<String> itemsAsString = items.stream().map(UUID::toString).collect(Collectors.toList());
        Map<String, Object> res = Map.of(
                "order_id", result.order_id.toString(),
                "paid", result.paid,
                "items", itemsAsString,
                "user_id", result.user_id.toString(),
                "total_cost", result.total_cost
        );
        return res;
    }


    public String addItem(String orderId, String itemId) {
        UUID orderIdUUID = UUID.fromString(orderId);
        UUID itemIdUUID = UUID.fromString(itemId);
        boolean result = orderService.addItemToOrder(orderIdUUID, itemIdUUID);
        return result ? "Success" : "Failure";
    }

    public String removeItem(String orderId, String itemId) {
        UUID orderIdUUID = UUID.fromString(orderId);
        UUID itemIdUUID = UUID.fromString(itemId);
        boolean result = orderService.removeItemFromOrder(orderIdUUID, itemIdUUID);
        return result ? "Success" : "Failure";
    }

    //TODO Call the payments microservice order to pay and the stock microservice to decrement the stock nr
    public String checkout(String orderId) {
        UUID orderIdUUID = UUID.fromString(orderId);
        boolean result = OrderService.changePaidStatus(orderIdUUID);
        return result ? "Success" : "Failure";
    }
}
