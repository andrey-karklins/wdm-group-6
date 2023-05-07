package controller;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;

import java.util.Map;
import java.util.UUID;

import services.StockService;
public class StockApiController {
    private static final StockService stock_service=new StockService();
    public Map<String, Object> find(String itemId) {
        UUID uuid = UUID.fromString(itemId);
        Row row= stock_service.findItemByID(uuid);
//      return Map.of("stock", 10, "price", 100);
        return Map.of("item_id", row.getUUID("item_id"), "stock", row.getInt("stock"), "price", row.getInt("price"));
    }

    public String subtract(String itemId, int amount) {
        UUID id = UUID.fromString(itemId);
        Row row = stock_service.SubStock(id,amount);
        Map<String, String> resultMap = Map.of(
                "item_id", row.getUUID("item_id").toString(),
                "stock", Integer.toString(row.getInt("stock")),
                "price", Integer.toString(row.getInt("price")));
        return resultMap.toString();
    }

    public String add(String itemId, int amount) {
        UUID id = UUID.fromString(itemId);
        Row row = stock_service.AddStock(id,amount);
        Map<String, String> resultMap = Map.of(
                "item_id", row.getUUID("item_id").toString(),
                "stock", Integer.toString(row.getInt("stock")),
                "price", Integer.toString(row.getInt("price")));
        return resultMap.toString();
    }

    public Map<String, Object> createItem(int price) {
        UUID itemID=stock_service.CreateItem(price);
        Map<String, Object> res = Map.of("item_id", itemID,"price:",price,"stock:",0);
        return res;
    }
}
