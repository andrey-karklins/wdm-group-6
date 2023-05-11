package app.services;

import app.OrderApp;
import app.models.Order;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

import java.util.UUID;

public class OrderService {
    static Cluster cluster = null;
    static Session session = null;
    static MappingManager mapper = null;
    static boolean initialized = false;

    public static void init() throws InterruptedException {
        System.out.println("Initializing connection to Cassandra...");
        while (!initialized) {
            try {
                cluster = Cluster.builder()
                        .addContactPoint("order-db")
                        .build();
                session = cluster.connect();
                createKeyspace("order_keyspace");
                useKeyspace("order_keyspace");
                createTable("orders");
                mapper = new MappingManager(session);
                initialized = true;
            } catch (Exception e) {
                System.out.println("Cassandra is not ready yet, retrying in 5 seconds...");
                Thread.sleep(5000);
            }
        }
        System.out.println("Connection to Cassandra initialized.");
    }

    // other methods...

    public static void createKeyspace(String keyspace) {
        String query = "CREATE KEYSPACE IF NOT EXISTS " + keyspace + " WITH replication "
                + "= {'class':'SimpleStrategy', 'replication_factor':3};";
        session.execute(query);
    }

    public static void useKeyspace(String keyspace) {
        String query = "USE " + keyspace + ";";
        session.execute(query);
    }

    public static void createTable(String tableName) {
        String query = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "order_id UUID PRIMARY KEY,"
                + "user_id UUID,"
                + "paid boolean,"
                + "total_cost int,"
                + "items list<UUID>"
                + ");";
        session.execute(query);
    }


    public static void sendEvent(String event, String data) {
        OrderApp.clients.forEach(client -> client.sendEvent(event, data));
    }

    // Here implement functions to interact with Cassandra
    public Order createOrder(Order order) {
        Mapper<Order> orderMapper = mapper.mapper(Order.class);
        orderMapper.save(order);
        return order;
    }

    public void deleteOrder(UUID orderId) {
        Mapper<Order> orderMapper = mapper.mapper(Order.class);
        orderMapper.delete(orderId);
    }

    public Order findOrder(UUID orderId) {
        Mapper<Order> orderMapper = mapper.mapper(Order.class);
        return orderMapper.get(orderId);
    }

    public void addItemToOrder(UUID orderId, UUID itemId) {
        Mapper<Order> orderMapper = mapper.mapper(Order.class);
        Order order = orderMapper.get(orderId);
        if (order != null) {
            order.items.add(itemId);
            orderMapper.save(order);
        }
    }

    public void removeItemFromOrder(UUID orderId, UUID itemId) {
        Mapper<Order> orderMapper = mapper.mapper(Order.class);
        Order order = orderMapper.get(orderId);
        if (order != null) {
            order.items.remove(itemId);
            orderMapper.save(order);
        }
    }

    public boolean checkoutOrder(UUID orderId) {
        Mapper<Order> orderMapper = mapper.mapper(Order.class);
        Order order = orderMapper.get(orderId);
        if (order != null && !order.paid) {
            order.paid = true;
            orderMapper.save(order);
            return true;
        }
        return false;
    }
}
