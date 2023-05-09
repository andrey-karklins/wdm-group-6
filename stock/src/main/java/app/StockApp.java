package app;

import app.controller.StockApiController;
import app.services.OrderEventsService;
import app.services.PaymentEventsService;
import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;

import java.util.concurrent.ConcurrentLinkedQueue;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class StockApp {
    public static ConcurrentLinkedQueue<SseClient> clients = new ConcurrentLinkedQueue<>();
    public static void main(String[] args) {
        var app = Javalin.create(/*config*/);
        var api = new StockApiController();
        app.routes(() -> {
            get("find/{item_id}", ctx -> ctx.json(api.find(ctx.pathParam("item_id"))));
            post("subtract/{item_id}/{amount}", ctx -> ctx.json(api.subtract(ctx.pathParam("item_id"), Integer.parseInt(ctx.pathParam("amount")))));
            post("add/{item_id}/{amount}", ctx -> ctx.json(api.add(ctx.pathParam("item_id"), Integer.parseInt(ctx.pathParam("amount")))));
            post("item/create/{price}", ctx -> ctx.json(api.createItem(Integer.parseInt(ctx.pathParam("price")))));
        });
        app.sse("/sse", (client) -> {
            client.sendEvent("connected", "stock-service");
            client.keepAlive();
            client.onClose(() -> clients.remove(client));
            clients.add(client);
        });
        app.events(event -> {
            event.serverStarted(() -> {
                try {
//                    StockService.init();
                    PaymentEventsService.listen();
                    OrderEventsService.listen();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });
        app.start(5000);

    }
}