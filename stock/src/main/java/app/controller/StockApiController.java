package app.controller;

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
        Row row= null;
        try{
            row= stock_service.findItemByID(uuid);
        }catch(StockError e){
            System.out.println(e.getMessage());
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            throw new StockError(e.getMessage());
//            return errorMap;
        }

//      return Map.of("stock", 10, "price", 100);
        return Map.of("item_id", row.getUUID("item_id"), "stock", row.getInt("stock"), "price", row.getFloat("price"));
    }

    public String subtract(String itemId, int amount) {
        UUID id = UUID.fromString(itemId);
        Row row = stock_service.SubStock(id,amount);
        Map<String, String> resultMap = Map.of(
                "item_id", row.getUUID("item_id").toString(),
                "stock", Integer.toString(row.getInt("stock")),
                "price", Float.toString(row.getFloat("price")));
        return resultMap.toString();
    }


    public String add(String itemId, int amount) {
        UUID id = UUID.fromString(itemId);
        Row row = stock_service.AddStock(id,amount);
        Map<String, String> resultMap = Map.of(
                "item_id", row.getUUID("item_id").toString(),
                "stock", Integer.toString(row.getInt("stock")),
                "price",Float.toString(row.getFloat("price")));
        return resultMap.toString();
    }

    public Map<String, Object> createItem(float price) {
        UUID itemID=stock_service.CreateItem(price);
        Map<String, Object> res = Map.of("item_id", itemID,"price:",price,"stock:",0);
        return res;
    }
}
