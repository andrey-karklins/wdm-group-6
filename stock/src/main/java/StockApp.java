import controller.StockApiController;
import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.*;

public class StockApp {
    public static void main(String[] args) {
        var app = Javalin.create(/*config*/);
        var api = new StockApiController();
        app.routes(() -> path("stock", () -> {
            get("find/{item_id}", ctx -> ctx.json(api.find(ctx.pathParam("item_id"))));
            post("subtract/{item_id}/{amount}", ctx -> ctx.json(api.subtract(ctx.pathParam("item_id"), Integer.parseInt(ctx.pathParam("amount")))));
            post("add/{item_id}/{amount}", ctx -> ctx.json(api.add(ctx.pathParam("item_id"), Integer.parseInt(ctx.pathParam("amount")))));
            post("item/create/{price}", ctx -> ctx.json(api.createItem(Integer.parseInt(ctx.pathParam("price")))));
        }));
        app.start(7072);
    }
}