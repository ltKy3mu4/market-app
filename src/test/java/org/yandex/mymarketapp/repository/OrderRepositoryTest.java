package org.yandex.mymarketapp.repository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.domain.OrderPosition;
import org.yandex.mymarketapp.repo.OrderRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class OrderRepositoryTest extends PostgresBaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void beforeEach(){
        this.executeSqlScript("sql/init-orders.sql");
    }

    Long userId = 0L;


    @Test
    void getAllWithPositions_ShouldReturnOrdersWithItems() {
        Flux<Order> ordersFlux = orderRepository.getAllWithPositions(userId);

        StepVerifier.create(ordersFlux.collectList())
                .assertNext(orders -> {
                    assertThat(orders).isNotEmpty();
                    assertThat(orders).hasSize(2);

                    Order foundOrder = orders.stream()
                            .filter(order -> order.getId() == 1L)
                            .findFirst()
                            .orElseThrow();

                    assertThat(foundOrder.getTotalSum()).isEqualTo(150.0);

                    assertThat(foundOrder.getItems()).isNotNull();
                    assertThat(foundOrder.getItems()).hasSize(2);

                    assertThat(foundOrder.getItems())
                            .extracting(OrderPosition::getTitle)
                            .containsExactlyInAnyOrder("Test Item 1", "Test Item 2");
                })
                .verifyComplete();
    }

    @Test
    void getByIdAndUserIdWithPositions_WhenOrderExists_ShouldReturnOrderWithItems() {
        Mono<Order> orderMono = orderRepository.getByIdAndUserIdWithPositions(1L, userId);

        StepVerifier.create(orderMono)
                .assertNext(order -> {
                    assertThat(order.getId()).isEqualTo(1L);
                    assertThat(order.getTotalSum()).isEqualTo(150.0);

                    assertThat(order.getItems()).isNotNull();
                    assertThat(order.getItems()).hasSize(2);

                    assertThat(order.getItems())
                            .extracting(OrderPosition::getPrice)
                            .containsExactlyInAnyOrder(50.0, 25.0);
                })
                .verifyComplete();
    }

    @Test
    void getByIdAndUserIdWithPositions_WhenOrderDoesNotExist_ShouldReturnEmpty() {
        Mono<Order> orderMono = orderRepository.getByIdAndUserIdWithPositions(999L, userId);

        StepVerifier.create(orderMono)
                .verifyComplete();
    }

    @Test
    void save_ShouldPersistOrderWithItems() {
        Order newOrder = new Order();
        newOrder.setTotalSum(75.0);

        OrderPosition newPosition = new OrderPosition();
        newPosition.setTitle("New Item");
        newPosition.setDescription("New Description");
        newPosition.setImgPath("/images/new.jpg");
        newPosition.setPrice(75.0);
        newPosition.setCount(1);
        newPosition.setOrderId(null); // Will be set after order save

        newOrder.setItems(Collections.singletonList(newPosition));

        Mono<Order> savedOrderMono = orderRepository.save(newOrder);

        StepVerifier.create(savedOrderMono)
                .assertNext(savedOrder -> {
                    assertThat(savedOrder.getId()).isNotNull();
                    assertThat(savedOrder.getTotalSum()).isEqualTo(75.0);
                    assertThat(savedOrder.getItems()).hasSize(1);
                    assertThat(savedOrder.getItems().get(0).getTitle()).isEqualTo("New Item");

                    // Verify the order position has the correct order ID
                    assertThat(savedOrder.getItems().get(0).getOrderId()).isEqualTo(savedOrder.getId());
                })
                .verifyComplete();
    }

    @Test
    void getAllWithPositions_WhenMultipleOrdersExist_ShouldReturnAllOrders() {
        Flux<Order> ordersFlux = orderRepository.getAllWithPositions(userId);

        StepVerifier.create(ordersFlux.collectList())
                .assertNext(orders -> {
                    assertThat(orders).hasSize(2);
                    assertThat(orders)
                            .extracting(Order::getTotalSum)
                            .containsExactlyInAnyOrder(150.0, 200.0);

                    Order order1 = orders.stream().filter(o -> o.getId() == 1L).findFirst().get();
                    Order order2 = orders.stream().filter(o -> o.getId() == 2L).findFirst().get();

                    assertThat(order1.getItems()).hasSize(2);
                    assertThat(order2.getItems()).hasSize(1);
                })
                .verifyComplete();
    }

    @Test
    void getByIdAndUserIdWithPositions_ShouldNotCauseLazyLoadingException() {
        Mono<Order> orderMono = orderRepository.getByIdAndUserIdWithPositions(1L, userId);

        StepVerifier.create(orderMono)
                .assertNext(order -> {
                    assertThat(order.getItems()).isNotNull();
                    // In reactive stack, there's no lazy loading - items are eagerly fetched
                    assertThat(order.getItems()).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    void getByIdAndUserIdWithPositions_WhenOrderHasNoItems_ShouldReturnOrderWithEmptyItems() {
        Order orderWithoutItems = new Order();
        orderWithoutItems.setTotalSum(0.0);
        orderWithoutItems.setItems(new ArrayList<>());

        Mono<Order> savedOrderMono = orderRepository.save(orderWithoutItems)
                .flatMap(savedOrder -> orderRepository.getByIdAndUserIdWithPositions(savedOrder.getId(), userId));

        StepVerifier.create(savedOrderMono)
                .assertNext(foundOrder -> {
                    assertThat(foundOrder.getItems()).isEmpty();
                    assertThat(foundOrder.getTotalSum()).isEqualTo(0.0);
                })
                .verifyComplete();
    }

    @Test
    void save_WithMultipleItems_ShouldPersistAllItems() {
        Order newOrder = new Order();
        newOrder.setTotalSum(125.0);

        OrderPosition position1 = new OrderPosition();
        position1.setTitle("Item 1");
        position1.setPrice(50.0);
        position1.setCount(1);
        position1.setImgPath("/images/item1.jpg");

        OrderPosition position2 = new OrderPosition();
        position2.setTitle("Item 2");
        position2.setPrice(75.0);
        position2.setCount(1);
        position2.setImgPath("/images/item2.jpg");


        newOrder.setItems(Arrays.asList(position1, position2));

        Mono<Order> savedOrderMono = orderRepository.save(newOrder);

        StepVerifier.create(savedOrderMono)
                .assertNext(savedOrder -> {
                    assertThat(savedOrder.getTotalSum()).isEqualTo(125.0);
                    assertThat(savedOrder.getItems()).hasSize(2);
                    assertThat(savedOrder.getItems())
                            .extracting(OrderPosition::getTitle)
                            .containsExactlyInAnyOrder("Item 1", "Item 2");
                    assertThat(savedOrder.getItems())
                            .extracting(OrderPosition::getOrderId)
                            .allMatch(id -> id.equals(savedOrder.getId()));
                })
                .verifyComplete();
    }
}