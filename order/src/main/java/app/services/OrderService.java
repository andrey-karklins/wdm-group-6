package app.services;

import app.OrderApp;
import app.models.Order;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.ResultSet;




public class OrderService {
    static Cluster cluster = null;
    static Session session = null;
    static MappingManager mapper = null;
    static boolean initialized = false;

    private static PreparedStatement insertOrderStmt;
    private static PreparedStatement deleteOrderStmt;
    private static PreparedStatement findOrderStmt;
    private static PreparedStatement addItemStmt;
    private static PreparedStatement removeItemStmt;
    private static PreparedStatement checkoutStmt;

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

                // Prepared statements initialization
                insertOrderStmt = session.prepare("INSERT INTO orders (order_id, user_id, items, total_cost, paid) VALUES (?, ?, ?, ?, ?)");
                deleteOrderStmt = session.prepare("DELETE FROM orders WHERE order_id = ?");
                findOrderStmt = session.prepare("SELECT * FROM orders WHERE order_id = ?");
                addItemStmt = session.prepare("UPDATE orders SET items = items + ?, total_cost = total_cost + ? WHERE order_id = ?");
                removeItemStmt = session.prepare("UPDATE orders SET items = items - ?, total_cost = total_cost - ? WHERE order_id = ?");
                checkoutStmt = session.prepare("UPDATE orders SET paid = true WHERE order_id = ?");

                initialized = true;
            } catch (Exception e) {
                System.out.println("Cassandra is not ready yet, retrying in 5 seconds...");
                Thread.sleep(5000);
            }
        }
        System.out.println("Connection to Cassandra initialized.");
    }
///
    //////
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
///////
    /////

    public static void sendEvent(String event, String data) {
        OrderApp.clients.forEach(client -> client.sendEvent(event, data));
    }
////
    ///
    // Here implement functions to interact with Cassandra

    public UUID createOrder(UUID userId) {
        UUID orderId = UUID.randomUUID();
        session.execute(insertOrderStmt.bind(orderId, userId, new ArrayList<UUID>(), 0, false));
        return orderId;
    }

    public boolean cancelOrder(UUID orderId) {
        session.execute(deleteOrderStmt.bind(orderId));
        return true;
    }

    public boolean addItemToOrder(UUID orderId, UUID itemId) {
        int price = 1;//dummy
        session.execute(addItemStmt.bind(List.of(itemId), price, orderId));
        return true;
    }

    public boolean removeItemFromOrder(UUID orderId, UUID itemId) {
        int price = 1;//dummy
        session.execute(removeItemStmt.bind(List.of(itemId), price, orderId));
        return true;
    }

    public boolean checkoutOrder(UUID orderId) {
        session.execute(checkoutStmt.bind(orderId));
        return true;
    }

    public Row findOrder(UUID orderId) {
        ResultSet rs = session.execute(findOrderStmt.bind(orderId));
        return rs.one();
    }
}
