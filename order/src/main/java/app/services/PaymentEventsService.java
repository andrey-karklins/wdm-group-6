package app.services;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

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

            case "OrderCanceled":
                OrderService.cancelOrder(UUID.fromString(data));
                break;
            case "PaymentSucceeded":
                OrderService.changePaidStatus(UUID.fromString(data));
                break;

            case "ifItemExists":
                System.out.println("payment received it too");
                break;
            default:
                System.out.println("Unknown event: " + event);
        }
    }
}
