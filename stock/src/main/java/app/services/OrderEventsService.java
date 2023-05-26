package app.services;
import app.StockError;
import java.util.*;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import com.datastax.driver.core.*;
import java.util.concurrent.TimeUnit;
import app.services.StockService;
public class OrderEventsService {
    private static boolean connected = false;
    public static void listen() throws InterruptedException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target("http://order-service:5000/sse");
        SseEventSource sseEventSource = SseEventSource.target(target).reconnectingEvery(1, TimeUnit.SECONDS).build();
        sseEventSource.register(event -> handler(event.getName(), event.readData(String.class)));
        sseEventSource.open();
        while(!connected) {
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
            case "ifItemExists":
//OrderService.sendEvent("ifItemExists",orderId+" "+itemId);
                HandlerAddItem(data);
                break;
            case "ItemRemoval":
                HandlerRemoveItem(data);
                break;
            case "OrderFailed":
                break;
            case "OrderCheckout":
                System.out.println(data);
                HandlerOrderCheckout(data);
                break;
            default:
                System.out.println("Unknown event: " + event);
        }
    }

    private static void HandlerAddItem(String data){
        String[] part =data.split(" ");
        String orderId =part[0];
        String itemId =part[1];
        UUID itemid=UUID.fromString(itemId);
        StockService stockervice = new StockService();
        Row result = stockervice.findItemByID(itemid);
        float item_price=0.0f;
        //cant find the item
        if( result ==  null)
            item_price=-2;
        //there is enough stock of the item
        if (result!=null){
            item_price = result.getFloat("price");
            //Row sub = stockervice.SubStock(itemid,1);
        }
//        System.out.println(item_price);
        StockService.sendEvent("ItemStock",orderId+" "+itemId+" "+ item_price);
    }

    private static void HandlerRemoveItem(String data){
        String[] part = data.split(" ");
        String orderId = part[0];
        String itemId = part[1];
        UUID itemid=UUID.fromString(itemId);
        StockService stockservice = new StockService();
        Row result = stockservice.findItemByID(itemid);
        float item_price = 0;
        //cant find the item
        if( result ==  null)
            item_price = -2;

        //there is enough stock of the item
        if (result!=null){
            item_price = result.getFloat("price");
            //Row subtract = stockservice.AddStock(itemid,1);
        }
//        System.out.println(item_price);
        StockService.sendEvent("ItemStock",orderId+" "+itemId+" "+ item_price);

    }
    private static void HandlerOrderCheckout(String data)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = new HashMap<>();
        try {
            map = objectMapper.readValue(data, Map.class);
        }catch (IOException e) {
            e.printStackTrace();
        }
        String userID = (String) map.get("UserID");
        List<String> items = (List<String>) map.get("Items");
        String orderID = (String) map.get("OrderID");
        System.out.println("UserID: " + userID);
        System.out.println("Items: " + items);
        System.out.println("OrderID: " + orderID);
        float total_price = 0.0f;
        StockService stockservice = new StockService();
        List<UUID> items_uuid = new ArrayList<>();;
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            UUID item_uuid = UUID.fromString(item);
            items_uuid.add(item_uuid);
            Row result = stockservice.findItemByID(item_uuid);
            try{
                Row subtract = stockservice.SubStock(item_uuid,1);
            }catch (StockError e){
                System.err.println("Caught a StockError: " + e.getMessage());
                Map<String, Object> ordermap = new HashMap<>();
                ordermap.put("OrderID",UUID.fromString(orderID));
                ObjectMapper objectMapper_to_user_fail = new ObjectMapper();
                String data_json_fail="";
                try {
                    data_json_fail = objectMapper_to_user_fail.writeValueAsString(ordermap);
                }catch(Exception e1) {
                    e.printStackTrace();
                }
                System.out.println("sendEvent StockSubtractFailed"+data_json_fail);
                StockService.sendEvent("StockSubtractFailed",data_json_fail);
                throw new IllegalStateException("Stock subtraction failed, aborting.");
            }
            total_price+=result.getFloat("price");
        }
        Map<String, Object> data_to_payment = new HashMap<>();
        data_to_payment.put("UserID", UUID.fromString(userID));
        data_to_payment.put("OrderID", UUID.fromString(orderID));
        data_to_payment.put("Items", items_uuid);
        data_to_payment.put("TotalCost", total_price);
        ObjectMapper objectMapper_to_payment = new ObjectMapper();
        String data_json="";
        try {
            data_json = objectMapper_to_payment.writeValueAsString(data_to_payment);
        } catch (Exception e) {
            e.printStackTrace();
        }
        StockService.sendEvent("StockSubtracted",data_json);
    }
}
