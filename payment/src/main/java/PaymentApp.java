import controllers.PaymentApiController;
import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.*;

public class PaymentApp {
    public static void main(String[] args) {
        var app = Javalin.create(/*config*/);
        var api = new PaymentApiController();
        app.routes(() -> path("payment", () -> {
            post("pay/{user_id}/{order_id}/{amount}}", ctx -> ctx.json(api.pay(ctx.pathParam("user_id"), ctx.pathParam("order_id"), Integer.parseInt(ctx.pathParam("amount")))));
            post("cancel/{user_id}/{order_id}", ctx -> ctx.json(api.cancel(ctx.pathParam("user_id"), ctx.pathParam("order_id"))));
            get("status/{user_id}/{order_id}", ctx -> ctx.json(api.status(ctx.pathParam("user_id"), ctx.pathParam("order_id"))));
            post("add_funds/{user_id}/{amount}", ctx -> ctx.json(api.addFunds(ctx.pathParam("user_id"), Integer.parseInt(ctx.pathParam("amount")))));
            post("create_user", ctx -> ctx.json(api.createUser()));
            get("find_user/{user_id}", ctx -> ctx.json(api.findUser(ctx.pathParam("user_id"))));
        }));
        app.start(7071);
    }
}