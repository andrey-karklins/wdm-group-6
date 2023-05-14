package app.services;
import java.util.UUID;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

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

    private static void HandleAddItem(String data){
        String[] parts = data.split(" ");
        String orderId = parts[0];
        String itemId = parts[1];
        UUID order_id = UUID.fromString(orderId);
        UUID item_id=UUID.fromString(itemId);
        StockService stockService = new StockService();
        Row res = stockService.findItemByID(item_id);
        int price=0;
        //cant find item
        if( res ==  null){
            price=-2;
        }
        //item stock is zero
        if (res!=null && res.getInt("stock")==0)
        {
            price=-1;
        }
        //there is enough stock of item
        if (res!=null && res.getInt("stock")>=1){
            price = res.getInt("price");
            Row sub = stockService.SubStock(item_id,1);
        }
        System.out.println(price);
        StockService.sendEvent("ItemStock",orderId+" "+itemId+" "+Integer.toString(price));
    }
    private static void HandleRemoveItem(String data){
        String[] parts = data.split(" ");
        String orderId = parts[0];
        String itemId = parts[1];
        UUID order_id = UUID.fromString(orderId);
        UUID item_id=UUID.fromString(itemId);
        StockService stockService = new StockService();
        Row res = stockService.findItemByID(item_id);
        int price=0;
        //cant find item
        if( res ==  null){
            price = -2;
        }
        //there is enough stock of item
        if (res!=null && res.getInt("stock")>=1){
            price = res.getInt("price");
            Row sub = stockService.AddStock(item_id,1);
        }
        System.out.println(price);
        StockService.sendEvent("ItemStock",orderId+" "+itemId+" "+Integer.toString(price));
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
                HandleAddItem(data);
                break;
            case "ItemRemoval":
                HandleRemoveItem(data0);
                break;
            default:
                System.out.println("Unknown event: " + event);
        }
    }
}
