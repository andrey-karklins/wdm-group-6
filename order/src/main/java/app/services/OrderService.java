package app.services;

import app.OrderApp;
import app.models.Order;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import java.util.ArrayList;
import java.util.UUID;

import com.datastax.driver.mapping.*;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;


public class OrderService {
    static Cluster cluster = null;
    static Session session = null;
    static MappingManager mapper = null;
    static boolean initialized = false;
    static Mapper<Order> mapperOrder = null;

    private static String keyspace = "order_keyspace";

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


    public UUID createOrder(UUID userId) {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, userId, false, 0, new ArrayList<UUID>());
        order.order_id = orderId;
        orderMapper.save(order);
        return orderId;
    }

    public boolean cancelOrder(UUID orderId) {
        OrderAccessor orderAccessor = mapper.createAccessor(OrderAccessor.class);
        Order order =  orderAccessor.getOrderById(orderId);
        orderMapper.delete(order);
        return true;
    }

    //TODO Retrieve the price from the stock microservice
    public boolean addItemToOrder(UUID orderId, UUID itemId) {

        int itemPrice = 1;//dummy
        Order order =  findOrderById(orderId);
        if (order == null) {
            return false;
        }

        if (order.items == null) {
            order.items = new ArrayList<>();
        }

        order.items.add(itemId);
        order.total_cost += itemPrice;
        orderMapper.save(order);
        return true;
    }


    //TODO Retrieve the price from the stock microservice
    public boolean removeItemFromOrder(UUID orderId, UUID itemId) {
        int itemPrice = 1;//dummy
        Order order =  findOrderById(orderId);
        if (order == null) {
            return false;
        }

        if (order.items == null) {
            return false;
        }
        order.items.remove(itemId);
        order.total_cost -= itemPrice;
        orderMapper.save(order);
        return true;
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
}
