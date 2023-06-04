package app.services;

import app.controllers.PaymentApiController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class StockEventsService {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static boolean connected = false;

    public static void listen() throws InterruptedException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target("http://stock-service:5000/sse");
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
            case "ItemStock":
                break;
            case "StockReturned":
            case "StockSubtracted":
                try {
                    //Parsing the JSON response from the data
                    JsonNode responseJSON = mapper.readTree(data);
                    UUID transactionID = UUID.fromString(responseJSON.get("TransactionID").asText());
                    UUID userID = UUID.fromString(responseJSON.get("UserID").asText());
                    UUID orderID = UUID.fromString(responseJSON.get("OrderID").asText());
                    JsonNode itemsNode = responseJSON.get("Items");
                    List<UUID> items = new ArrayList<>();
                    for (JsonNode itemNode : itemsNode) {
                        UUID item = UUID.fromString(itemNode.asText());
                        items.add(item);
                    }
                    int totalCost = responseJSON.get("TotalCost").asInt();
                    //Based on the event, either call returnFunds or subtractFunds
                    if (event.equals("StockReturned")) {
                        PaymentApiController.returnFunds(userID, orderID, transactionID, totalCost, items);
                    } else {
                        PaymentApiController.subtractFunds(userID, orderID, transactionID, totalCost, items);
                    }

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                break;

            default:
        }
    }
}
