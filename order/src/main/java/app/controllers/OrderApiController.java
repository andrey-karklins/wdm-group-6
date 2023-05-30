package app.controllers;

import app.services.OrderService;
import app.models.Order;
import com.fasterxml.jackson.databind.ObjectMapper; // import jackson library
import java.util.Map;
import java.util.HashMap;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class OrderApiController {
    private static final OrderService orderService = new OrderService();
    public static final ConcurrentHashMap<UUID, CompletableFuture<String>> checkoutTransactionMap = new ConcurrentHashMap<>();


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
//        OrderService.sendEvent("OrderCanceled", orderId);
//        OrderService.sendEvent("OrderFailed", items);
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
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String data_json = objectMapper.writeValueAsString(data);
            OrderService.sendEvent("OrderCheckout",data_json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //todo: edge case: it was already paid by other transaction
        boolean result = OrderService.changePaidStatus(orderIdUUID);
        return future.get(); //result ? "Success" : "Failure";
    }

    public String cancelPayment(String orderId) {
        UUID orderIdUUID = UUID.fromString(orderId);
        Order resultOrder = OrderService.findOrderById(UUID.fromString(orderId));
        Map<String, Object> data = new HashMap<>();
        data.put("UserID", resultOrder.user_id);
        data.put("OrderID", resultOrder.order_id);
        data.put("Items", resultOrder.items);
        data.put("TotalCost", resultOrder.total_cost);
        ObjectMapper objectMapper = new ObjectMapper();

        if (!resultOrder.paid) {
            try {
                String data_json = objectMapper.writeValueAsString(data);
                OrderService.sendEvent("CancelPaymentFailed",data_json);
                return "Failure";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            String data_json = objectMapper.writeValueAsString(data);
            OrderService.sendEvent("OrderCancelled",data_json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean result = OrderService.changePaidStatus(orderIdUUID);
        return result ? "Success" : "Failure";
    }
}
