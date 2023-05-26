package app.controllers;

import app.services.OrderService;
import app.models.Order;

import java.util.*;
import java.util.stream.Collectors;


public class OrderApiController {
    private static final OrderService orderService = new OrderService();

    public Map<String, String> createOrder(String userId) {
        UUID userIdUUID = UUID.fromString(userId);
        UUID orderId = orderService.createOrder(userIdUUID);
        Map<String, String> res = Map.of("order_id", orderId.toString());
        return res;
    }

    public String cancelOrder(String orderId) {
        Order resultOrder = OrderService.findOrderById(UUID.fromString(orderId));
        String items = "";
        for (int i = 0; i < resultOrder.items.size(); i++) {
            items += resultOrder.items.get(i).toString();
            if(i!=resultOrder.items.size()-1)
            {
                items+=" ";
            }
        }
        OrderService.sendEvent("OrderCanceled", orderId);
        OrderService.sendEvent("OrderFailed", items);
        UUID orderIdUUID = UUID.fromString(orderId);
        boolean result = OrderService.cancelOrder(orderIdUUID);
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
        OrderService.sendEvent("ifItemExists",orderId+" "+itemId);
        UUID orderIdUUID = UUID.fromString(orderId);
        UUID itemIdUUID = UUID.fromString(itemId);
        boolean result = orderService.addItemToOrder(orderIdUUID, itemIdUUID);
        return result ? "Success" : "Failure";
    }

    public String removeItem(String orderId, String itemId) {
        OrderService.sendEvent("ItemRemoval", orderId + " " + itemId);
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
