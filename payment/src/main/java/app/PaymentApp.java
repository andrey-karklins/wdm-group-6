package app;

import app.controllers.PaymentApiController;
import app.services.OrderEventsService;
import app.services.StockEventsService;
import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;

import java.util.concurrent.ConcurrentLinkedQueue;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class PaymentApp {
    public static ConcurrentLinkedQueue<SseClient> clients = new ConcurrentLinkedQueue<>();
    public static void main(String[] args) {
        var app = Javalin.create(/*config*/);
        var api = new PaymentApiController();
        app.routes(() -> {
            post("pay/{user_id}/{order_id}/{amount}", ctx -> ctx.json(api.pay(ctx.pathParam("user_id"), ctx.pathParam("order_id"), Integer.parseInt(ctx.pathParam("amount")))));
            post("cancel/{user_id}/{order_id}", ctx -> ctx.json(api.cancel(ctx.pathParam("user_id"), ctx.pathParam("order_id"))));
            get("status/{user_id}/{order_id}", ctx -> ctx.json(api.status(ctx.pathParam("user_id"), ctx.pathParam("order_id"))));
            post("add_funds/{user_id}/{amount}", ctx -> ctx.json(api.addFunds(ctx.pathParam("user_id"), Integer.parseInt(ctx.pathParam("amount")))));
            post("create_user", ctx -> ctx.json(api.createUser()));
            get("find_user/{user_id}", ctx -> ctx.json(api.findUser(ctx.pathParam("user_id"))));
        });
        app.sse("/sse", (client) -> {
            client.sendEvent("connected", "payment-service");
            client.keepAlive();
            client.onClose(() -> clients.remove(client));
            clients.add(client);
        });
        app.events(event -> {
            event.serverStarted(() -> {
                try {
//                    PaymentService.init();
                    OrderEventsService.listen();
                    StockEventsService.listen();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        });
        app.start(5000);
    }
}