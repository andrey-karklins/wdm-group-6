package app.services;

import app.OrderApp;
import app.models.Order;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class OrderService {
    static Cluster cluster = null;
    static Session session = null;
    static MappingManager mapper = null;
    static boolean initialized = false;
    private static String keyspace = "order_keyspace";

    private final String stockUrl = "http://stock-service:5000";
    static ConcurrentHashMap<String, Integer> priceMap = new ConcurrentHashMap<>();

    static Mapper<Order> orderMapper;

    public static void init() throws InterruptedException {
        System.out.println("Initializing connection to Cassandra...");
        while (!initialized) {
            try {
                cluster = Cluster.builder()
                        .addContactPoint("order-db")
                        .build();
                session = cluster.connect();
                createKeyspace(keyspace);
                useKeyspace(keyspace);
                createTable("orders");

                mapper = new MappingManager(session);
                orderMapper = mapper.mapper(Order.class, keyspace);
                initialized = true;
            } catch (Exception e) {
                System.out.println("Cassandra is not ready yet, retrying in 5 seconds...");
                Thread.sleep(5000);
            }
        }
        System.out.println("Connection to Cassandra initialized.");
    }

    public static void createKeyspace(String keyspace) {
        String query = "CREATE KEYSPACE IF NOT EXISTS " + keyspace
                + " WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};";
        session.execute(query);
    }

    public static void useKeyspace(String keyspace) {
        session.execute("USE " + keyspace);
    }

    public static void createTable(String tableName) {
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "order_id uuid PRIMARY KEY,"
                + "paid boolean,"
                + "items list<uuid>,"
                + "user_id uuid,"
                + "total_cost int);";
        session.execute(query);
    }

    public static void sendEvent(String event, String data) {
        OrderApp.clients.forEach(client -> client.sendEvent(event, data));
    }

    public static void updateOrder(UUID orderId, UUID userId, int totalCost) {
        Order order = findOrderById(orderId);
        if (order != null && order.user_id.equals(userId)) {
            order.total_cost = totalCost;
            order.paid = true;
            orderMapper.save(order);
        }
    }

    public static boolean cancelOrder(UUID orderId) {
        OrderAccessor orderAccessor = mapper.createAccessor(OrderAccessor.class);
        Order order =  orderAccessor.getOrderById(orderId);
        orderMapper.delete(order);
        return true;
    }

    public static void cancelOrder(UUID orderId, UUID userId, UUID transactionId) {
        Order order = findOrderById(orderId);
        if (order.paid) {
            sendEvent("OrderCancelledFailed", null);
        } else {
            changePaidStatus(orderId);
        }
        String orderIdString = orderId.toString();
        String userIdString = userId.toString();
        String transactionIdSting = transactionId.toString();
        List<UUID> items = order.items;
        int total_cost = order.total_cost;
        Map<String, Object> data_to_stock = new HashMap<>();
        data_to_stock.put("OrderID", UUID.fromString(orderIdString));
        data_to_stock.put("UserID", UUID.fromString(userIdString));
        data_to_stock.put("transactionID", UUID.fromString(transactionIdSting));
        data_to_stock.put("Items", items);
        data_to_stock.put("TotalCost", total_cost);
        ObjectMapper objectMapper_to_payment = new ObjectMapper();
        String data_json="";
        try {
            data_json = objectMapper_to_payment.writeValueAsString(data_to_stock);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendEvent("OrderCancelled",data_json);
    }

    public UUID createOrder(UUID userId) {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, userId, false, 0, new ArrayList<UUID>());
        order.order_id = orderId;
        orderMapper.save(order);
        return orderId;
    }

    /**
     * Sends a request to the stockService to find the item, given the UUID
     * @param itemId the UUID of the item
     * @return the JSON representation of the item
     */
    private JsonNode requestItem(UUID itemId)  {
        // Build the request URL
        String url = stockUrl + "/find/" + itemId.toString();
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            String response = client.execute(request, httpResponse -> EntityUtils.toString(httpResponse.getEntity()));
            ObjectMapper JSONmapper = new ObjectMapper();
            try {
                return JSONmapper.readTree(response);
            } catch (JsonParseException e) {
                // Invalid JSON response
                // System.err.println("Invalid JSON response: " + response);
                return null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Finds and returns an order object given an unique orderId
     * @param orderId the order id
     * @return the order object from the database
     */
    public static Order findOrderById(UUID orderId) {
        OrderAccessor orderAccessor = mapper.createAccessor(OrderAccessor.class);
        return orderAccessor.getOrderById(orderId);
    }

    /**
     * Changes the status of the order
     * @param orderId the order id
     * @return true if the change went through
     */
    public static boolean changePaidStatus(UUID orderId) {
        boolean status = false;
        Order order =  findOrderById(orderId);

        if (order != null) {
            order.paid = !order.paid;
            orderMapper.save(order);
            status = true;

        }
        return status;
    }

    @Accessor
    public interface OrderAccessor {
        @Query("SELECT * FROM orders WHERE order_id = :order_id")
        Order getOrderById(@Param("order_id") UUID order_id);
    }

    //TODO Retrieve the price from the stock microservice
    public boolean addItemToOrder(UUID orderId, UUID itemId) {
//        JsonNode item = requestItem(itemId);
//        if (item == null) {
//            return false;
//        }
//        assert item != null;

//        int itemPrice = (int) item.get("price").asInt();

        Order order = findOrderById(orderId);
        if (order == null) {
            return false;
        }
        if (order.items == null) {
            order.items = new ArrayList<>();
        }
        order.items.add(itemId);
//        order.total_cost += itemPrice;
        orderMapper.save(order);
        return true;
    }

    //TODO Retrieve the price from the stock microservice
    public boolean removeItemFromOrder(UUID orderId, UUID itemId) {
//        int itemPrice = 1;//dummy
//        int itemPrice = priceMap.get(orderId.toString() + " " + itemId.toString());
        Order order = findOrderById(orderId);
        if (order == null) {
            return false;
        }

        if (order.items == null) {
            return false;
        }
        order.items.remove(itemId);
//        order.total_cost -= itemPrice;
        orderMapper.save(order);
        return true;
    }
}
