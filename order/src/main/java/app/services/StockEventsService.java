package app.services;

import app.controllers.OrderApiController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
                float price = Float.parseFloat(data.split(" ")[2]);
                OrderService.priceMap.put(orderId + " " + itemId, price);
                break;
//            case "StockSubtractedFailed":
//                System.out.println("StockSubtractedFailed: " + data);
//                break;
            case "StockSubtractedFailed":
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode jsonNode = objectMapper.readTree(data);
                    UUID failedOrderId = UUID.fromString(jsonNode.get("orderID").asText());
                    UUID transactionId = UUID.fromString(jsonNode.get("transactionID").asText());
                    String errorMsg = jsonNode.get("errorMsg").asText();
                    OrderApiController.checkoutTransactionMap.get(transactionId).complete(errorMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("Unknown event: " + event);
        }
    }
}
