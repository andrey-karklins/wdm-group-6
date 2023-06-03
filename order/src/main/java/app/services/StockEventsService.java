package app.services;

import app.controllers.OrderApiController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static app.services.OrderService.changePaidStatus;
import static app.services.OrderService.sendEvent;

public class StockEventsService {
    private static boolean connected = false;
    public static void listen() throws InterruptedException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target("http://stock-service:5000/sse");
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
            case "ItemStock":
//                System.out.println("ItemStock: " + data);
                String orderId = data.split(" ")[0];
                String itemId = data.split(" ")[1];
                int price = Integer.parseInt(data.split(" ")[2]);
                OrderService.priceMap.put(orderId + " " + itemId, price);
                break;
//            case "StockSubtractedFailed":
//                System.out.println("StockSubtractedFailed: " + data);
//                break;
            case "StockSubtractFailed":
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode jsonNode = objectMapper.readTree(data);
                    UUID failedOrderId = UUID.fromString(jsonNode.get("OrderID").asText());
                    UUID transactionId = UUID.fromString(jsonNode.get("TransactionID").asText());
                    String errorMsg = jsonNode.get("ErrorMsg").asText();
                    OrderApiController.checkoutTransactionMap.get(transactionId).complete(errorMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "StockReturnFailed":
                ObjectMapper objectMapper2 = new ObjectMapper();
                try {
                    JsonNode jsonNode = objectMapper2.readTree(data);
                    UUID failedOrderId = UUID.fromString(jsonNode.get("OrderID").asText());
                    UUID transactionId = UUID.fromString(jsonNode.get("TransactionID").asText());
                    String errorMsg = jsonNode.get("ErrorMsg").asText();
                    changePaidStatus(failedOrderId);

                    Map<String, Object> data_to_payment = new HashMap<>();
                    data_to_payment.put("transactionID", transactionId);
                    data_to_payment.put("ErrorMsg", errorMsg);
                    String data_json="";
                    try {
                        data_json = objectMapper2.writeValueAsString(data_to_payment);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sendEvent("OrderCancelledFailed", data_json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            default:
                System.out.println("Unknown event: " + event);
        }
    }
}
