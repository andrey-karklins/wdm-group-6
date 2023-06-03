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
                System.out.println("Unknown event: " + event);
        }
    }

//    private static void HandlerAddItem(String data){
//        String[] part =data.split(" ");
//        String orderId =part[0];
//        String itemId =part[1];
//        UUID itemid=UUID.fromString(itemId);
//        StockService stockervice = new StockService();
//        Item item = stockervice.findItemByID(itemid);
//        int item_price = 0;
//        //cant find the item
//        if( result ==  null)
//            item_price=-2;
//        //there is enough stock of the item
//        if (result!=null){
//            item_price = result.getInt("price");
//            //Row sub = stockervice.SubStock(itemid,1);
//        }
////        System.out.println(item_price);
//        StockService.sendEvent("ItemStock",orderId+" "+itemId+" "+ item_price);
//    }

    //    private static void HandlerRemoveItem(String data){
//        String[] part = data.split(" ");
//        String orderId = part[0];
//        String itemId = part[1];
//        UUID itemid=UUID.fromString(itemId);
//        StockService stockservice = new StockService();
//        Row result = stockservice.findItemByID(itemid);
//        int item_price = 0;
//        //cant find the item
//        if( result ==  null)
//            item_price = -2;
//
//        //there is enough stock of the item
//        if (result!=null){
//            item_price = result.getInt("price");
//            //Row subtract = stockservice.AddStock(itemid,1);
//        }
////        System.out.println(item_price);
//        StockService.sendEvent("ItemStock",orderId+" "+itemId+" "+ item_price);
//
//    }
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
        int total_price = 0;
        StockService stockservice = new StockService();
        List<UUID> items_uuid = new ArrayList<>();
        Map<String, Integer> items_map = new HashMap<>();
        for (String s : items) {
            items_uuid.add(UUID.fromString(s));
            if (!items_map.containsKey(s)) {
                items_map.put(s, 1);
            } else {
                items_map.put(s, items_map.get(s) + 1);
            }
        }
        // see if there are enough stock for each item in the order
        for (Map.Entry<String, Integer> entry : items_map.entrySet()) {
            UUID item_uuid = UUID.fromString(entry.getKey());
            Integer amount = entry.getValue();
            Map<String, Object> failmap = new HashMap<>();
//            System.out.println("Key: " + item_uuid + ", Value: " + amount);
            Item item = null;
            try {
                item = stockservice.findItemByID(item_uuid);
            } catch (StockError e) {
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
            if (item.stock < amount) {
                //NOT enough stock for each item in the order
                failmap.put("OrderID", UUID.fromString(orderID));
                failmap.put("Items", items_uuid);
                failmap.put("TransactionID", UUID.fromString(transactionID));
                String errorMsg = "NOT enough stock";
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
        }
        // there are enough stock for each item in the order
        for (Map.Entry<String, Integer> entry : items_map.entrySet()) {
            UUID item_uuid = UUID.fromString(entry.getKey());
            Integer amount = entry.getValue();
            Item result = stockservice.findItemByID(item_uuid);
            // subtract the stock
            boolean subtract = stockservice.SubStock(item_uuid, amount);
            total_price += result.price;
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

