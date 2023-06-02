package app;

import app.services.OrderService;
import app.controllers.OrderApiController;
import app.services.PaymentEventsService;
import app.services.StockEventsService;
import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;

import java.util.concurrent.ConcurrentLinkedQueue;

import static io.javalin.apibuilder.ApiBuilder.*;

public class OrderApp {
    public static ConcurrentLinkedQueue<SseClient> clients = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        var app = Javalin.create(/*config*/);
        var api = new OrderApiController();
        app.routes(() -> {
            post("create/{user_id}", ctx -> ctx.json(api.createOrder(ctx.pathParam("user_id"))));
            delete("remove/{order_id}", ctx -> ctx.json(api.cancelOrder(ctx.pathParam("order_id"))));
            get("find/{order_id}", ctx -> ctx.json(api.findOrder(ctx.pathParam("order_id"))));
            post("addItem/{order_id}/{item_id}", ctx -> ctx.json(api.addItem(ctx.pathParam("order_id"), ctx.pathParam("item_id"))));
            delete("removeItem/{order_id}/{item_id}", ctx -> ctx.json(api.removeItem(ctx.pathParam("order_id"), ctx.pathParam("item_id"))));
            post("checkout/{order_id}", ctx -> ctx.json(api.checkout(ctx.pathParam("order_id"))));
            post("cancelPayment/{order_id}", ctx -> ctx.json(api.cancelPayment(ctx.pathParam("order_id"))));
        });
        app.sse("/sse", (client) -> {
            client.sendEvent("connected", "order-service");
            client.keepAlive();
            client.onClose(() -> clients.remove(client));
            clients.add(client);
        });
        app.events(event -> {
            event.serverStarted(() -> {
                try {
                    OrderService.init();
                    PaymentEventsService.listen();
                    StockEventsService.listen();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });
        app.start(5000);
    }
}