package services;

import com.datastax.driver.core.*;
import com.datastax.driver.mapping.MappingManager;
import models.Item;

import java.util.UUID;

import static com.datastax.driver.core.DataType.uuid;

public class StockService {
    static Cluster cluster = null;
    static Session session = null;
    static MappingManager mapper = null;
    static boolean initialized = false;

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
        query = "INSERT INTO items (item_id, stock, price) VALUES (9552eace-06a7-4a5e-a90d-9200063ed94a,100,6)";
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
                initialized = true;
            } catch (Exception e) {
                System.out.println("Cassandra is not ready yet, retrying in 5 seconds...");
                Thread.sleep(5000);
            }
        }
        System.out.println("Connection to Cassandra_stock initialized.");
    }

    // Here implement functions to interact with Cassandra
    public Row findItemByID(UUID itemID){
        session.execute("USE stock_keyspace");
//        String m2="SELECT * FROM items WHERE item_id = "+itemID;
//        System.out.println(m2);
//        ResultSet resultSet = session.execute("SELECT * FROM items WHERE item_id = ?", itemID);
        String query = "SELECT * FROM items WHERE item_id = ?";
        PreparedStatement preparedStatement = session.prepare(query);
        BoundStatement boundStatement = preparedStatement.bind(itemID);
        ResultSet resultSet=session.execute(boundStatement);
//        System.out.println(m2);
        System.out.println("Item ID data type: " + itemID.getClass().getName());
        return resultSet.one();
    }
    public UUID CreateItem(int price) {
//        PreparedStatement preparedStatement = session.prepare(query);
//         Bind the values to the query
//        BoundStatement boundStatement = preparedStatement.bind(id, 0, price);
//        session.execute(boundStatement);
        UUID id= UUID.randomUUID();
        System.out.println(id);
        String query = "INSERT INTO items (item_id, stock, price) VALUES ("+id+",0,"+price+")";
        System.out.println(query);
        session.execute(query);

        return id;
    }
    public Row AddStock(UUID itemID,int amount){
        Row row = findItemByID(itemID);
        int original_stock = row.getInt("stock");
        int new_stock = original_stock+amount;
        String query = "UPDATE items SET stock = ? WHERE item_id = ?";
        PreparedStatement preparedStatement = session.prepare(query);
        BoundStatement boundStatement = preparedStatement.bind(new_stock, itemID);
        session.execute(boundStatement);
        return findItemByID(itemID);
    }
    public Row SubStock(UUID itemID,int amount){
        Row row = findItemByID(itemID);
        int original_stock = row.getInt("stock");
        int new_stock = original_stock-amount;
        String query = "UPDATE items SET stock = ? WHERE item_id = ?";
        PreparedStatement preparedStatement = session.prepare(query);
        BoundStatement boundStatement = preparedStatement.bind(new_stock, itemID);
        session.execute(boundStatement);
        return findItemByID(itemID);
    }
}
