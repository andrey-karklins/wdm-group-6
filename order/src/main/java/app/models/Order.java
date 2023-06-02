package app.models;

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

    public Order() {

    }

    public Order(UUID orderId, UUID userId, boolean paid, int totalCost, List<UUID> items) {
        this.order_id = orderId;
        this.user_id = userId;
        this.paid = paid;
        this.total_cost = totalCost;
        this.items = items;
    }
}
