package app.controllers;

import app.services.OrderService;
import app.models.Order;

import java.util.List;
import java.util.Map;

import java.util.UUID;

import com.datastax.driver.core.Row;
import java.util.stream.Collectors;

//public class OrderApiController {
//    private static final OrderService order_service=new OrderService();
//    public Map<String, Object> createOrder(String userId) {
////        Map<String, Object> res = Map.of("order_id", 123);
////        return res;
//        UUID UserId = UUID.fromString(userId)
//        UUID OrderId = order_service.CreateOrder(userId);
//        String order_id = OrderId.toString();
//        Map<String, Object> res = Map.of("user_id", userId, "order_id", order_id);
//        return res;
//    }
//
//    public String cancelOrder(String orderId) {
////        return "Success";
//        UUID id = UUID.fromString(orderId);
//        String result = order_service.CancelOrder(orderId);
//        return result;
//    }
//
//    public Map<String, Object> findOrder(String orderId) {
////        Map<String, Object> res = Map.of("order_id", orderId,
////                "paid", true,
////                "items", List.of("123", "456"),
////                "user_id", "123",
////                "total_cost", 100);
////        return res;
//        UUID OrderId= UUID.fromString(orderId);
//        Row result = order_service.FindOrder(OrderId);
//        Map<String, Object> res = Map.of("order_id", orderId,
//                "paid", result.getBool("paid"),
//                "items", result.getList("items", Integer),
//                "user_id", result.getUUID("user_id").toString(),
//                "total_cost", result.getInt("total_cost"));
//        return res;
//    }
//
//    public String addItem(String orderId, String itemId) {
//        UUID
//        return "Success";
//    }
//
//    public String removeItem(String orderId, String itemId) {
//        return "Success";
//    }
//
//    public String checkout(String orderId) {
//        return "Success";
//    }
//}


public class OrderApiController {
    private static final OrderService orderService = new OrderService();

    public String createOrder(String userId) {
        UUID userIdUUID = UUID.fromString(userId);
        UUID orderId = orderService.createOrder(userIdUUID);
        Map<String, String> res = Map.of("user_id", userId, "order_id", orderId.toString());
        return "user id: " + userId + " " + "order id: " + orderId.toString();
    }

//    public String  createOrder(String userId) {
//        UUID userIdUUID = UUID.fromString(userId);
//        UUID orderId = orderService.createOrder(userIdUUID);
//        Map<String, String> res = Map.of("user_id", userId, "order_id", orderId.toString());
//        return orderId.toString();
//    }

    public String cancelOrder(String orderId) {
        UUID orderIdUUID = UUID.fromString(orderId);
        boolean result = orderService.cancelOrder(orderIdUUID);
        return result ? "Success" : "Failure";
    }

    public Map<String, Object> findOrder(String orderId) {
        UUID OrderId = UUID.fromString(orderId);
        Row result = orderService.findOrder(OrderId);
        List<UUID> items = result.getList("items", UUID.class);
        List<String> itemsAsString = items.stream().map(UUID::toString).collect(Collectors.toList());
        Map<String, Object> res = Map.of(
                "order_id", result.getUUID("order_id").toString(),
                "paid", result.getBool("paid"),
                "items", itemsAsString,
                "user_id", result.getUUID("user_id").toString(),
                "total_cost", result.getInt("total_cost")
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

    public String checkout(String orderId) {
        UUID orderIdUUID = UUID.fromString(orderId);
        boolean result = orderService.checkoutOrder(orderIdUUID);
        return result ? "Success" : "Failure";
    }
}
