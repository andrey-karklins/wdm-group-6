package app.services;

import app.OrderApp;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;

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
                mapper = new MappingManager(session);
                initialized = true;
            } catch (Exception e) {
                System.out.println("Cassandra is not ready yet, retrying in 5 seconds...");
                Thread.sleep(5000);
            }
        }
        System.out.println("Connection to Cassandra initialized.");
    }

    public static void sendEvent(String event, String data) {
        OrderApp.clients.forEach(client -> client.sendEvent(event, data));
    }

    // Here implement functions to interact with Cassandra
}
