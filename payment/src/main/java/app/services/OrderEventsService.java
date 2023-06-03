package app.services;

import app.controllers.PaymentApiController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class OrderEventsService {
    private static final ObjectMapper mapper = new ObjectMapper();
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
            case "ifItemExists":
                break;
            case "OrderCancelledFailed":
                try {
                    JsonNode responseJSON = mapper.readTree(data);
                    UUID transactionID = UUID.fromString(responseJSON.get("TransactionID").asText());
                    String errorMsg = responseJSON.get("ErrorMsg").asText();
                    PaymentApiController.transactionMap.get(transactionID).complete(errorMsg);

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                System.out.println("Unknown event: " + event);
        }
    }
}
