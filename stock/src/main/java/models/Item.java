package models;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(name = "items")
public class Item {
    @PartitionKey
    public UUID item_id = UUID.randomUUID();

    public int stock;

    public int price;
}
