package app.services;

import app.StockApp;
import app.StockError;
import app.models.Item;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

import java.util.UUID;

public class StockService {
    static Cluster cluster = null;
    static Session session = null;
    static MappingManager mapper = null;
    static Mapper<Item> itemMapper;
    static ItemAccessor itemAccessor = null;
    static boolean initialized = false;

    public static void sendEvent(String event, String data) {
        StockApp.clients.forEach(client -> client.sendEvent(event, data));
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
                + "item_id uuid PRIMARY KEY,"
                + "stock int,"
                + "price int);";
        session.execute(query);
        query = "INSERT INTO items (item_id, stock, price) VALUES (9552eace-06a7-4a5e-a90d-9200063ed94a,5,6)";
        session.execute(query);

    }

    public static void init() throws InterruptedException {
        System.out.println("Initializing connection to Cassandra...");
        while (!initialized) {
            try {
                cluster = Cluster.builder()
                        .addContactPoint("stock-db")
                        .build();
                session = cluster.connect();
                createKeyspace("stock_keyspace");
                useKeyspace("stock_keyspace");
                createTable("items");
//                Item newItem = new Item("9552eace-06a7-4a5e-a90d-9200063ed94a", 10, 6);
//                insertItem(newItem);
                mapper = new MappingManager(session);
                itemMapper = mapper.mapper(Item.class, "stock_keyspace");
                itemAccessor = mapper.createAccessor(ItemAccessor.class);
                initialized = true;
            } catch (Exception e) {
                System.out.println("Cassandra is not ready yet, retrying in 5 seconds...");
                Thread.sleep(5000);
            }
        }
        System.out.println("Connection to Cassandra_stock initialized.");
    }

    // Here implement functions to interact with Cassandra
    public Item findItemByID(UUID itemID) {
        Item item = itemAccessor.getItemById(itemID);
        if (item == null) {
            throw new StockError("can't find such item in stock");
        }
        return item;
    }

    public UUID CreateItem(int price) {
        UUID id = UUID.randomUUID();
        Item item = new Item(id.toString(), 0, price);
        itemMapper.save(item);
        return id;
    }

    public boolean AddStock(UUID itemID, int amount) {
        Item item = findItemByID(itemID);
        item.stock += amount;
        itemMapper.save(item);
        return true;
    }

    public boolean SubStock(UUID itemID, int amount) {
        Item item = findItemByID(itemID);
        int original_stock = item.stock;
        if (original_stock < amount) {
            throw new StockError("Not enough stock");
        }
        item.stock -= amount;
        itemMapper.save(item);
        return true;
    }

    @Accessor
    public interface ItemAccessor {
        @Query("SELECT * FROM items WHERE item_id = :item_id")
        Item getItemById(@Param("item_id") UUID item_id);
    }
}
