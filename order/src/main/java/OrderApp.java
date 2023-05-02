import controllers.OrderApiController;
import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.apibuilder.ApiBuilder.post;

public class OrderApp {
    public static void main(String[] args) {
        var app = Javalin.create(/*config*/);
        var api = new OrderApiController();
        app.routes(() -> path("orders", () -> {
            post("create/{user_id}", ctx -> ctx.json(api.createOrder(ctx.pathParam("user_id"))));
            delete("remove/{order_id}", ctx -> ctx.json(api.cancelOrder(ctx.pathParam("order_id"))));
            get("find/{order_id}", ctx -> ctx.json(api.findOrder(ctx.pathParam("order_id"))));
            post("addItem/{order_id}/{item_id}", ctx -> ctx.json(api.addItem(ctx.pathParam("order_id"), ctx.pathParam("item_id"))));
            delete("removeItem/{order_id}/{item_id}", ctx -> ctx.json(api.removeItem(ctx.pathParam("order_id"), ctx.pathParam("item_id"))));
            post("checkout/{order_id}", ctx -> ctx.json(api.checkout(ctx.pathParam("order_id"))));
        }));
        app.start(7070);
    }
}