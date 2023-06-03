package app.services;

import app.StockError;
import app.models.Item;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OrderEventsService {
    private static boolean connected = false;

    public static void listen() throws InterruptedException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target("http://order-service:5000/sse");
        SseEventSource sseEventSource = SseEventSource.target(target).reconnectingEvery(1, TimeUnit.SECONDS).build();
        sseEventSource.register(event -> handler(event.getName(), event.readData(String.class)));
        sseEventSource.open();
        while (!connected) {
            Thread.sleep(1000);
        }
    }

    private static void handler(String event, String data) {
        switch (event) {
            case "connected":
                System.out.println("Connected to " + data);
                connected = true;
                break;
            // Below events to be handled
            // ... (TODO)
            case "OrderCheckout":
//                System.out.println(data);
                HandlerOrderCheckout(data);
                break;
            case "OrderCancelled":
                HandlerOrderCancelled(data);
                break;
            default:
        }
    }

    private static void HandlerOrderCheckout(String data) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Object, Object> data_map = new HashMap<>();
        try {
            data_map = objectMapper.readValue(data, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String userID = (String) data_map.get("UserID");
        List<String> items = (List<String>) data_map.get("Items");
        String orderID = (String) data_map.get("OrderID");
        String transactionID = (String) data_map.get("TransactionID");
        int total_price;
        StockService stockservice = new StockService();
        List<UUID> items_uuid = new ArrayList<>();
        HashMap<UUID, Integer> items_map = new HashMap<>();
        for (String s : items) {
            UUID key = UUID.fromString(s);
            items_uuid.add(key);
            if (!items_map.containsKey(key)) {
                items_map.put(key, 1);
            } else {
                items_map.put(key, items_map.get(key) + 1);
            }
        }
        try {
            total_price = stockservice.subStock(items_map);
        } catch (StockError e) {
            Map<String, Object> failmap = new HashMap<>();
            //Item does not exist
            failmap.put("OrderID", UUID.fromString(orderID));
            failmap.put("Items", items_uuid);
            failmap.put("TransactionID", UUID.fromString(transactionID));
            String errorMsg = "NO such item in stock!";
            failmap.put("ErrorMsg", errorMsg);
            ObjectMapper objectMapper_to_user_fail = new ObjectMapper();
            String data_json_fail = "";
            try {
                data_json_fail = objectMapper_to_user_fail.writeValueAsString(failmap);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            StockService.sendEvent("StockSubtractFailed", data_json_fail);
            return;
        }
        Map<String, Object> data_to_payment = new HashMap<>();
        data_to_payment.put("UserID", UUID.fromString(userID));
        data_to_payment.put("OrderID", UUID.fromString(orderID));
        data_to_payment.put("Items", items_uuid);
        data_to_payment.put("TotalCost", total_price);
        data_to_payment.put("TransactionID", UUID.fromString(transactionID));
        ObjectMapper objectMapper_to_payment = new ObjectMapper();
        String data_json = "";
        try {
            data_json = objectMapper_to_payment.writeValueAsString(data_to_payment);
        } catch (Exception e) {
            e.printStackTrace();
        }
        StockService.sendEvent("StockSubtracted", data_json);
    }

    private static void HandlerOrderCancelled(String data) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Object, Object> data_map = new HashMap<>();
        Map<String, Object> failmap = new HashMap<>();
        try {
            data_map = objectMapper.readValue(data, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> items = (List<String>) data_map.get("Items");
        String orderID = (String) data_map.get("OrderID");
        String userID = (String) data_map.get("UserID");
        Integer cost = (Integer) data_map.get("TotalCost");
        String transactionID = (String) data_map.get("TransactionID");
        int total_cost = 0;
        List<UUID> items_uuid = new ArrayList<>();
        StockService stockService = new StockService();
        for (String s : items) {
            Item item = null;
            items_uuid.add(UUID.fromString(s));
            try {
                item = stockService.findItemByID(UUID.fromString(s));
                total_cost += item.price;
            } catch (StockError e) {
                failmap.put("OrderID", UUID.fromString(orderID));
                failmap.put("Items", items_uuid);
                failmap.put("TransactionID", UUID.fromString(transactionID));
                String errorMsg = "NO such item in stock!";
                failmap.put("ErrorMsg", errorMsg);
                ObjectMapper objectMapper_to_user_fail = new ObjectMapper();
                String data_json_fail = "";
                try {
                    data_json_fail = objectMapper_to_user_fail.writeValueAsString(failmap);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                StockService.sendEvent("StockSubtractFailed", data_json_fail);
            }
            boolean res = stockService.AddStock(UUID.fromString(s), 1);
        }
        Map<String, Object> data_to_payment = new HashMap<>();
        data_to_payment.put("UserID", UUID.fromString(userID));
        data_to_payment.put("OrderID", UUID.fromString(orderID));
        data_to_payment.put("Items", items_uuid);
        data_to_payment.put("TotalCost", total_cost);
        data_to_payment.put("TransactionID", UUID.fromString(transactionID));
        ObjectMapper objectMapper_to_payment = new ObjectMapper();
        String data_json = "";
        try {
            data_json = objectMapper_to_payment.writeValueAsString(data_to_payment);
        } catch (Exception e) {
            e.printStackTrace();
        }
        StockService.sendEvent("StockReturned", data_json);
    }
}

