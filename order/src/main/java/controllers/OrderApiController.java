package controllers;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.util.List;
import java.util.Map;

public class OrderApiController {
    public Map<String, Object> createOrder(String userId) {
        Map<String, Object> res = Map.of("order_id", 123);
        return res;
    }

    public String cancelOrder(String orderId) {
        return "Success";
    }

    public Map<String, Object> findOrder(String orderId) {
        Map<String, Object> res = Map.of("order_id", orderId,
                "paid", true,
                "items", List.of("123", "456"),
                "user_id", "123",
                "total_cost", 100);
        Cluster cluster = null;
        try {
            cluster = Cluster.builder()                                                    // (1)
                    .addContactPoint("order-db")
                    .build();
            Session session = cluster.connect();                                           // (2)

            ResultSet rs = session.execute("select release_version from system.local");    // (3)
            Row row = rs.one();
            System.out.println(row.getString("release_version"));                          // (4)
        } finally {
            if (cluster != null) cluster.close();                                          // (5)
        }
        return res;
    }

    public String addItem(String orderId, String itemId) {
        return "Success";
    }

    public String removeItem(String orderId, String itemId) {
        return "Success";
    }

    public String checkout(String orderId) {
        return "Success";
    }
}
