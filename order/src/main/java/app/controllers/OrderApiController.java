package app.controllers;

import app.OrderError;
import app.models.Order;
import app.services.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class OrderApiController {
    public static final ConcurrentHashMap<UUID, CompletableFuture<String>> checkoutTransactionMap = new ConcurrentHashMap<>();
    private static final OrderService orderService = new OrderService();

    public Map<String, String> createOrder(String userId) {
        UUID userIdUUID = UUID.fromString(userId);
        UUID orderId = orderService.createOrder(userIdUUID);
        Map<String, String> res = Map.of("order_id", orderId.toString());
        return res;
    }

    public String removeOrder(String orderId) {
        Order resultOrder = OrderService.findOrderById(UUID.fromString(orderId));
        String items = "";
        for (int i = 0; i < resultOrder.items.size(); i++) {
            items += resultOrder.items.get(i).toString();
            if (i != resultOrder.items.size() - 1) {
                items += " ";
            }
        }
//        OrderService.sendEvent("OrderCanceled", orderId);
//        OrderService.sendEvent("OrderFailed", items);
        UUID orderIdUUID = UUID.fromString(orderId);
        boolean result = OrderService.removeOrder(orderIdUUID);
        return result ? "Success" : "Failure";
    }

    public Map<String, Object> findOrder(String orderId) {
        UUID OrderId = UUID.fromString(orderId);
        Order result = OrderService.findOrderById(OrderId);
        List<UUID> items = result.items;
        if (items == null) {
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

//    public static void handleFailedTransaction(UUID orderId, String errorMsg) {
//        // Update the order status to Failure
//        orderStatusMap.put(orderId, "Failure");
//        System.out.println("Transaction for order " + orderId + " failed: " + errorMsg);
//    }

    //TODO Call the payments microservice order to pay and the stock microservice to decrement the stock nr
    //Check if it is unpaid
    public String checkout(String orderId) throws ExecutionException, InterruptedException {
        UUID orderIdUUID = UUID.fromString(orderId);
        Order resultOrder = OrderService.findOrderById(UUID.fromString(orderId));
        //Check if it is unpaid
        UUID transactionId = UUID.randomUUID();
        CompletableFuture<String> future = new CompletableFuture<>();
        checkoutTransactionMap.put(transactionId, future);
        Map<String, Object> data = new HashMap<>();
        data.put("UserID", resultOrder.user_id);
        data.put("OrderID", resultOrder.order_id);
        data.put("Items", resultOrder.items);
        data.put("TransactionID", transactionId);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String data_json = objectMapper.writeValueAsString(data);
            OrderService.sendEvent("OrderCheckout", data_json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //todo: edge case: it was already paid by other transaction
        String result = future.get();
        checkoutTransactionMap.remove(transactionId);
        if (result == "Success") {
            OrderService.changePaidStatus(orderIdUUID);
            return result;
        } else {
            throw new OrderError(result);
        }
    }

    public String cancelPayment(String orderId) {
        UUID orderIdUUID = UUID.fromString(orderId);
        boolean result = OrderService.changePaidStatus(orderIdUUID);
        return result ? "Success" : "Failure";
    }
}
