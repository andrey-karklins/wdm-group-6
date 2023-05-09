package app.models;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(name = "users")
public class User {
    @PartitionKey
    public UUID user_id = UUID.randomUUID();

    public int credit = 0;
}
