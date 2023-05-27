package app.services;

import com.datastax.driver.core.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PaymentEventsService {
    private static boolean connected = false;
    public static void listen() throws InterruptedException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target("http://payment-service:5000/sse");
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
                connected = true;
                System.out.println("Connected to " + data);
                break;
            // Below events to be handled
            // ... (TODO)
            case "OrderPaidFailed":
                HandlerOrderPaidFailed(data);
                break;
            case "ReturnFundsFailed":
                HandlerReturnFundsFailed(data);
                break;
            default:
                System.out.println("Unknown event: " + event);
        }
    }
    private static void HandlerOrderPaidFailed(String data)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Object, Object> data_map = new HashMap<>();
        try {
            data_map = objectMapper.readValue(data, Map.class);
        }catch (IOException e) {
            e.printStackTrace();
        }
        List<String> items = (List<String>) data_map.get("Items");
        String orderID = (String) data_map.get("OrderID");
        StockService stockservice = new StockService();
        List<UUID> items_uuid = new ArrayList<>();;
        for (String s:items){
            items_uuid.add(UUID.fromString(s));
            Row result = stockservice.AddStock(UUID.fromString(s),1);
        }
        //send failed event to OrderService
        Map<String, Object> failmap = new HashMap<>();
        failmap.put("OrderID",UUID.fromString(orderID));
        failmap.put("Items",items_uuid);
        ObjectMapper objectMapper_to_user_fail = new ObjectMapper();
        String data_json_fail="";
        try {
            data_json_fail = objectMapper_to_user_fail.writeValueAsString(failmap);
        }catch(Exception e1) {
            e1.printStackTrace();
        }
        StockService.sendEvent("FundsSubtractFailed",data_json_fail);
    }
    private static void HandlerReturnFundsFailed(String data)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<Object, Object> data_map = new HashMap<>();
        try {
            data_map = objectMapper.readValue(data, Map.class);
        }catch (IOException e) {
            e.printStackTrace();
        }
        List<String> items = (List<String>) data_map.get("Items");
        String orderID = (String) data_map.get("OrderID");
        StockService stockservice = new StockService();
        List<UUID> items_uuid = new ArrayList<>();;
        for (String s:items){
            items_uuid.add(UUID.fromString(s));
            Row result = stockservice.SubStock(UUID.fromString(s),1);
        }
        //send failed event to OrderService
        Map<String, Object> failmap = new HashMap<>();
        failmap.put("OrderID",UUID.fromString(orderID));
//        failmap.put("Items",items_uuid);
        ObjectMapper objectMapper_to_user_fail = new ObjectMapper();
        String data_json_fail="";
        try {
            data_json_fail = objectMapper_to_user_fail.writeValueAsString(failmap);
        }catch(Exception e1) {
            e1.printStackTrace();
        }
        StockService.sendEvent("StockReturnFailed",data_json_fail);
    }
}
