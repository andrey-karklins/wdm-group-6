package controller;

import java.util.Map;

public class StockApiController {
    public Map<String, Object> find(String itemId) {
        Map<String, Object> res = Map.of("stock", 10, "price", 100);
        return res;
    }

    public String subtract(String itemId, int amount) {
        return "Success";
    }

    public String add(String itemId, int amount) {
        return "Success";
    }

    public Map<String, Object> createItem(int price) {
        Map<String, Object> res = Map.of("item_id", 123);
        return res;
    }
}
