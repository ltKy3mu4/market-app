package org.yandex.mymarketapp.repository;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.domain.OrderPosition;
import org.yandex.mymarketapp.repo.OrderRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Sql(scripts = "/sql/init-orders.sql")
class OrderRepositoryTest extends PostgresBaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;


    @Test
    void getAllWithPositions_ShouldReturnOrdersWithItems() {
        List<Order> orders = orderRepository.getAllWithPositions();

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
    }

    @Test
    void getByIdWithPositions_WhenOrderExists_ShouldReturnOrderWithItems() {
        Optional<Order> foundOrder = orderRepository.getByIdWithPositions(1L);

        assertThat(foundOrder).isPresent();

        Order order = foundOrder.get();
        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getTotalSum()).isEqualTo(150.0);

        assertThat(order.getItems()).isNotNull();
        assertThat(order.getItems()).hasSize(2);

        assertThat(order.getItems())
                .extracting(OrderPosition::getPrice)
                .containsExactlyInAnyOrder(50.0, 25.0);
    }

    @Test
    void getByIdWithPositions_WhenOrderDoesNotExist_ShouldReturnEmptyOptional() {
        Optional<Order> foundOrder = orderRepository.getByIdWithPositions(999L);
        assertThat(foundOrder).isEmpty();
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
        newPosition.setOrder(newOrder);

        newOrder.setItems(Collections.singletonList(newPosition));

        Order saved = orderRepository.save(newOrder);

        Optional<Order> retrievedOrder = orderRepository.getByIdWithPositions(saved.getId());
        assertThat(retrievedOrder).isPresent();

        Order order = retrievedOrder.get();
        assertThat(order.getTotalSum()).isEqualTo(75.0);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getTitle()).isEqualTo("New Item");
    }

    @Test
    void getAllWithPositions_WhenMultipleOrdersExist_ShouldReturnAllOrders() {
        List<Order> orders = orderRepository.getAllWithPositions();

        assertThat(orders).hasSize(2);
        assertThat(orders)
                .extracting(Order::getTotalSum)
                .containsExactlyInAnyOrder(150.0, 200.0);

        Order order1 = orders.stream().filter(o -> o.getId() == 1L).findFirst().get();
        Order order2 = orders.stream().filter(o -> o.getId() == 2L).findFirst().get();

        assertThat(order1.getItems()).hasSize(2);
        assertThat(order2.getItems()).hasSize(1);
    }

    @Test
    void getByIdWithPositions_ShouldNotCauseLazyLoadingException() {
        Optional<Order> foundOrder = orderRepository.getByIdWithPositions(1L);

        assertThat(foundOrder).isPresent();

        Order order = foundOrder.get();
        assertThatCode(() -> {
            List<OrderPosition> items = order.getItems();
            assertThat(items).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void findByNonExistentId_ShouldReturnEmptyOptional() {
        Optional<Order> result = orderRepository.findById(999L);
        assertThat(result).isEmpty();
    }

    @Test
    void delete_ShouldRemoveOrder() {
        Long orderId = 1L;

        orderRepository.deleteById(orderId);

        Optional<Order> deletedOrder = orderRepository.findById(orderId);
        assertThat(deletedOrder).isEmpty();
    }


    @Test
    void getByIdWithPositions_WhenOrderHasNoItems_ShouldReturnOrderWithEmptyItems() {
        Order orderWithoutItems = new Order();
        orderWithoutItems.setTotalSum(0.0);
        orderWithoutItems.setItems(new ArrayList<>());

        Order saved = orderRepository.save(orderWithoutItems);

        Optional<Order> foundOrder = orderRepository.getByIdWithPositions(saved.getId());

        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getItems()).isEmpty();
    }
}