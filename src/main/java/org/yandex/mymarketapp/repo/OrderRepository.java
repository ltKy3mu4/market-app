package org.yandex.mymarketapp.repo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.domain.OrderPosition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final DatabaseClient databaseClient;
    private final R2dbcEntityTemplate template;

    public Mono<Order> save(Order order) {
        return template.insert(Order.class)
                .using(order)
                .flatMap(savedOrder -> {
                    order.getItems().forEach(e-> e.setOrderId(savedOrder.getId()));
                    Mono<List<OrderPosition>> savedPos= Flux.fromIterable(order.getItems())
                            .flatMap(position -> template.insert(OrderPosition.class).using(position))
                            .collectList();
                    return Mono.zip(savedPos, Mono.just(savedOrder));
                })
                .map(tuple -> {
                    tuple.getT2().setItems(tuple.getT1());
                    return tuple.getT2();
                });
    }


    public Flux<Order> getAllWithPositions() {
        String sql = """
            SELECT o.id, o.total_sum, op.id as position_id, op.order_id, op.title, op.description, op.img_path, op.price, op.count
            FROM orders o
            LEFT JOIN order_positions op ON o.id = op.order_id
            ORDER BY o.id
            """;

        return databaseClient.sql(sql)
                .fetch()
                .all()
                .bufferUntilChanged(result -> result.get("id"))
                .map(e -> mapToOrderWithPositions(e));
    }

    public Mono<Order> getByIdWithPositions(long orderId) {
        String sql = """
            SELECT o.*, op.id as position_id, op.order_id, op.title, op.description, op.img_path, op.price, op.count
            FROM orders o 
            LEFT JOIN order_positions op ON o.id = op.order_id
            WHERE o.id = :id
            """;

        return databaseClient.sql(sql)
                .bind("id", orderId)
                .fetch()
                .all()
                .collectList()
                .flatMap(rows -> {
                    Order order = mapToOrderWithPositions(rows);
                    return order != null ? Mono.just(order) : Mono.empty();
                });
    }

    private Order mapToOrderWithPositions(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return null;

        Map<String, Object> firstRow = rows.get(0);
        Order order = new Order();
        order.setId((Long) firstRow.get("id"));
        order.setTotalSum((Double) firstRow.get("total_sum"));

        List<OrderPosition> positions = rows.stream()
                .filter(row -> row.get("position_id") != null)
                .map(this::mapToOrderPosition)
                .collect(Collectors.toList());

        order.setItems(positions);
        return order;
    }

    private OrderPosition mapToOrderPosition(Map<String, Object> row) {
        OrderPosition position = new OrderPosition();
        position.setId((Long) row.get("position_id"));
        position.setOrderId((Long) row.get("id"));
        position.setTitle((String) row.get("title"));
        position.setDescription((String) row.get("description"));
        position.setImgPath((String) row.get("image_path"));
        position.setPrice((Double) row.get("price"));
        position.setCount((Integer) row.get("count"));
        return position;
    }
}

