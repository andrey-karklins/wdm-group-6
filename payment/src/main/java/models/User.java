package models;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(name = "orders")
public class User {
    @PartitionKey
    public UUID user_id = UUID.randomUUID();

    public int credit = 0;
}
