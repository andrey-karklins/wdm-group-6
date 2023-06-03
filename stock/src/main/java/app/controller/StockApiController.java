package app.controller;
import app.models.Item;
import app.StockError;
import app.services.StockService;
import com.datastax.driver.core.Row;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class StockApiController {
    private static final StockService stock_service=new StockService();
    public Map<String, Object> find(String itemId) {
        UUID uuid = UUID.fromString(itemId);
        Item item= null;
        try{
            item= stock_service.findItemByID(uuid);
        }catch(StockError e){
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            throw new StockError(e.getMessage());
//            return errorMap;
        }

//      return Map.of("stock", 10, "price", 100);
        return Map.of("item_id", item.item_id, "stock", item.stock, "price", item.price);
    }

    public String subtract(String itemId, int amount) {
        UUID id = UUID.fromString(itemId);
        boolean res = stock_service.SubStock(id, amount);
        return res?"Success" : "Failure";
    }


    public String add(String itemId, int amount) {
        UUID id = UUID.fromString(itemId);
        boolean res = stock_service.AddStock(id, amount);
//        Map<String, String> resultMap = Map.of(
//                "item_id", row.getUUID("item_id").toString(),
//                "stock", Integer.toString(row.getInt("stock")),
//                "price", Integer.toString(row.getInt("price")));
        return res?"Success" : "Failure";
    }

    public Map<String, Object> createItem(int price) {
        UUID itemID = stock_service.CreateItem(price);
        Map<String, Object> res = Map.of("item_id", itemID, "price:", price, "stock:", 0);
        return res;
    }
}
