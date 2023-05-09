package app.services;

import app.PaymentApp;
import app.models.User;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PaymentService {
    static Cluster cluster = null;
    static Session session = null;
    static MappingManager mapper = null;
    static boolean initialized = false;
    static String keyspace = "payment_keyspace";

    public static void sendEvent(String event, String data) {
        PaymentApp.clients.forEach(client -> client.sendEvent(event, data));
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
                + "user_id uuid PRIMARY KEY,"
                + "credit int);";
        session.execute(query);
    }

    public static void init() throws InterruptedException {
        System.out.println("Initializing connection to Cassandra...");
        while (!initialized) {
            try {
                cluster = Cluster.builder()
                        .addContactPoint("payment-db")
                        .build();
                session = cluster.connect();
                mapper = new MappingManager(session);
                initialized = true;
                createKeyspace(keyspace);
                useKeyspace(keyspace);
                createTable("users");

            } catch (Exception e) {
                System.out.println("Cassandra is not ready yet, retrying in 5 seconds...");
                Thread.sleep(5000);
            }
        }
        System.out.println("Connection to Cassandra initialized.");
    }


    // Function to add funds to user's account
    public static boolean addFunds(UUID user_id, int amount) {
        boolean done = false;
        UserAccessor userAccessor = mapper.createAccessor(UserAccessor.class);
        User user = userAccessor.getUserById(user_id);
        if (user != null) {
            user.credit += amount;
            Mapper<User> mapperUser = mapper.mapper(User.class);
            mapperUser.save(user);
            done = true;
        }
        return done;
    }

    // Function to create a new user with 0 credit
    @NotNull
    public static UUID createUser() {
        UUID user_id = UUID.randomUUID();
        User user = new User();
        user.user_id = user_id;
        user.credit = 0;
        System.out.println(keyspace);
        Mapper<User> mapperUser = mapper.mapper(User.class, keyspace);
        mapperUser.save(user);
        return user_id;
    }

    public static User findUserById(UUID user_id) {
        UserAccessor userAccessor = mapper.createAccessor(UserAccessor.class);
        return userAccessor.getUserById(user_id);
    }

    @Accessor
    public interface UserAccessor {
        @Query("SELECT * FROM users WHERE user_id = :user_id")
        User getUserById(@Param("user_id") UUID user_id);
    }
}
