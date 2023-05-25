package app.services;
import java.util.Objects;
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
                HandlerOrderFailed(data);
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
        //the item stock is zero
        if (result!=null && result.getInt("stock")==0)
            item_price=-1;
        //there is enough stock of the item
        if (result!=null && result.getInt("stock")>=1){
            item_price = result.getFloat("price");
            Row sub = stockervice.SubStock(itemid,1);
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
            Row subtract = stockservice.AddStock(itemid,1);
        }
//        System.out.println(item_price);
        StockService.sendEvent("ItemStock",orderId+" "+itemId+" "+ item_price);

    }
    private static void HandlerOrderFailed(String data)
    {
        if (Objects.equals(data, ""))
            return;

        String[] uuidStrings = data.split(" ");
        UUID[] uuids = new UUID[uuidStrings.length];
        StockService stockservice = new StockService();
        System.out.println(data);

        for (int i = 0; i < uuidStrings.length; i++) {
            System.out.println(uuidStrings[i]);
            uuids[i] = UUID.fromString(uuidStrings[i]);
            Row subtract = stockservice.AddStock(uuids[i],1);
        }
    }
}
