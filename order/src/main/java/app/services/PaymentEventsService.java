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

public class PaymentEventsService {
    private static boolean connected = false;

    public static void listen() throws InterruptedException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target("http://payment-service:5000/sse");
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
                connected = true;
                System.out.println("Connected to " + data);
                break;
            // Below events to be handled
            // ... (TODO)

            case "OrderCanceled":
                OrderService.cancelOrder(UUID.fromString(data));
                break;
            case "PaymentSucceeded":
                OrderService.changePaidStatus(UUID.fromString(data));
                break;

            case "ifItemExists":
                break;

            case "FundsSubtracted":
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode jsonNode = objectMapper.readTree(data);
                    UUID userId = UUID.fromString(jsonNode.get("UserID").asText());
                    UUID orderId = UUID.fromString(jsonNode.get("OrderID").asText());
                    UUID transactionId = UUID.fromString(jsonNode.get("TransactionID").asText());
                    int totalCost = (int) jsonNode.get("TotalCost").asDouble();
                    OrderService.updateOrder(orderId, userId, totalCost);
                    OrderApiController.checkoutTransactionMap.get(transactionId).complete("Success");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "CancelPayment":
                ObjectMapper objectMapper2 = new ObjectMapper();
                try {
                    JsonNode jsonNode = objectMapper2.readTree(data);
                    UUID userId = UUID.fromString(jsonNode.get("UserID").asText());
                    UUID orderId = UUID.fromString(jsonNode.get("OrderID").asText());
                    UUID transactionId = UUID.fromString(jsonNode.get("transactionID").asText());
                    OrderService.cancelOrder(orderId, userId, transactionId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            default:
        }
    }
}
