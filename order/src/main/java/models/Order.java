package models;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.List;
import java.util.UUID;

@Table(name = "orders")
public class Order {
    @PartitionKey
    public UUID order_id = UUID.randomUUID();

    public UUID user_id;

    public boolean paid = false;

    public int total_cost = 0;

    public List<UUID> items;
}
