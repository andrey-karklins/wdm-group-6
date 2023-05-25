package app.models;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(name = "items")
public class Item {
    @PartitionKey
    public UUID item_id = UUID.randomUUID();

    public int stock;

    public float price;
    public Item(String itemId, int stock, float price) {
        this.item_id = UUID.fromString(itemId);
        this.stock = stock;
        this.price = price;
    }
}
