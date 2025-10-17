package org.yandex.mymarketapp.service;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.domain.OrderPosition;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.OrderDto;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;
import org.yandex.mymarketapp.model.mapper.OrderMapperImpl;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.OrderRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {OrderService.class, OrderMapperImpl.class})
class OrderServiceTest {

    @MockitoBean
    private OrderRepository orderRepo;

    @MockitoBean
    private CartPositionsRepository cartRepo;

    @Autowired
    private OrderService orderService;

    @Test
    void makeOrder_WithMultipleItems_ShouldCreateOrderWithCorrectTotal() {
        // Given
        List<ItemDto> cartItems = Arrays.asList(
                new ItemDto(1L, "Item 1", "Description 1", "/img1.jpg", 10.0, 2),
                new ItemDto(2L, "Item 2", "Description 2", "/img2.jpg", 15.0, 3),
                new ItemDto(3L, "Item 3", "Description 3", "/img3.jpg", 5.0, 1)
        );

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setTotalSum(70.0); // (10*2) + (15*3) + (5*1) = 20 + 45 + 5 = 70

        when(cartRepo.getAllCartPositions()).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(cartRepo.clearCart()).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = orderService.makeOrder();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).getAllCartPositions();
        verify(orderRepo).save(any(Order.class));
        verify(cartRepo).clearCart();
    }

    @Test
    void makeOrder_WithSingleItem_ShouldCreateOrderWithSingleItem() {
        // Given
        List<ItemDto> cartItems = Collections.singletonList(
                new ItemDto(1L, "Single Item", "Description", "/img.jpg", 25.0, 1)
        );

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setTotalSum(25.0);

        when(cartRepo.getAllCartPositions()).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(cartRepo.clearCart()).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = orderService.makeOrder();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).getAllCartPositions();
        verify(orderRepo).save(argThat(order ->
                order.getTotalSum() == 25.0
        ));
        verify(cartRepo).clearCart();
    }

    @Test
    void makeOrder_WithEmptyCart_ShouldCreateEmptyOrder() {
        // Given
        when(cartRepo.getAllCartPositions()).thenReturn(Flux.empty());
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return Mono.just(order);
        });
        when(cartRepo.clearCart()).thenReturn(Mono.just(0));

        // When
        Mono<Void> result = orderService.makeOrder();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).getAllCartPositions();
        verify(orderRepo).save(argThat(order -> {
            assertTrue(order.getItems().isEmpty());
            assertEquals(0.0, order.getTotalSum(), 0.001);
            return true;
        }));
        verify(cartRepo).clearCart();
    }

    @Test
    void getAllOrders_WhenNoOrdersExist_ShouldReturnEmptyFlux() {
        // Given
        when(orderRepo.getAllWithPositions()).thenReturn(Flux.empty());

        // When
        Flux<OrderDto> result = orderService.getAllOrders();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(orderRepo).getAllWithPositions();
    }

    @Test
    void getAllOrders_WhenOrdersExist_ShouldReturnOrderDtos() {
        // Given
        Order order1 = createOrder(1L, 50.0, 2);
        Order order2 = createOrder(2L, 75.0, 1);

        when(orderRepo.getAllWithPositions()).thenReturn(Flux.just(order1, order2));

        // When
        Flux<OrderDto> result = orderService.getAllOrders();

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(orderRepo).getAllWithPositions();
    }

    @Test
    void getOrderById_WhenOrderNotExists_ShouldThrowException() {
        // Given
        Long orderId = 999L;
        when(orderRepo.getByIdWithPositions(orderId)).thenReturn(Mono.empty());

        // When
        Mono<OrderDto> result = orderService.getOrderById(orderId);

        // Then
        StepVerifier.create(result)
                .verifyErrorMatches(throwable ->
                        throwable instanceof OrderNotFoundException &&
                                throwable.getMessage().equals("order with id 999 not found!")
                );

        verify(orderRepo).getByIdWithPositions(orderId);
    }

    @Test
    void getOrderById_WhenOrderExists_ShouldReturnOrderDto() {
        // Given
        Long orderId = 1L;
        Order order = createOrder(orderId, 100.0, 2);
        when(orderRepo.getByIdWithPositions(orderId)).thenReturn(Mono.just(order));

        // When
        Mono<OrderDto> result = orderService.getOrderById(orderId);

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(orderRepo).getByIdWithPositions(orderId);
    }

    @Test
    void getOrderById_WithZeroId_ShouldThrowException() {
        // Given
        Long orderId = 0L;
        when(orderRepo.getByIdWithPositions(orderId)).thenReturn(Mono.empty());

        // When
        Mono<OrderDto> result = orderService.getOrderById(orderId);

        // Then
        StepVerifier.create(result)
                .verifyError(OrderNotFoundException.class);

        verify(orderRepo).getByIdWithPositions(orderId);
    }

    @Test
    void getOrderById_WithNegativeId_ShouldThrowException() {
        // Given
        Long orderId = -1L;
        when(orderRepo.getByIdWithPositions(orderId)).thenReturn(Mono.empty());

        // When
        Mono<OrderDto> result = orderService.getOrderById(orderId);

        // Then
        StepVerifier.create(result)
                .verifyError(OrderNotFoundException.class);

        verify(orderRepo).getByIdWithPositions(orderId);
    }

    @Test
    void makeOrder_WithLargeQuantities_ShouldCalculateCorrectTotal() {
        // Given
        List<ItemDto> cartItems = Arrays.asList(
                new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 0.99, 100),  // 0.99 * 100 = 99
                new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 999.99, 2)   // 999.99 * 2 = 1999.98
        );
        // Total: 99 + 1999.98 = 2098.98

        when(cartRepo.getAllCartPositions()).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return Mono.just(order);
        });
        when(cartRepo.clearCart()).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = orderService.makeOrder();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(orderRepo).save(argThat(order ->
                Math.abs(2098.98 - order.getTotalSum()) < 0.001
        ));
        verify(cartRepo).clearCart();
    }

    @Test
    void makeOrder_WhenClearCartFails_ShouldPropagateError() {
        // Given
        List<ItemDto> cartItems = Collections.singletonList(
                new ItemDto(1L, "Item", "Desc", "/img.jpg", 10.0, 1)
        );

        Order savedOrder = new Order();
        savedOrder.setId(1L);

        when(cartRepo.getAllCartPositions()).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(cartRepo.clearCart()).thenReturn(Mono.error(new RuntimeException("Clear cart failed")));

        // When
        Mono<Void> result = orderService.makeOrder();

        // Then
        StepVerifier.create(result)
                .verifyError(RuntimeException.class);

        verify(cartRepo).getAllCartPositions();
        verify(orderRepo).save(any(Order.class));
        verify(cartRepo).clearCart();
    }

    @Test
    void makeOrder_WhenSaveOrderFails_ShouldNotClearCart() {
        // Given
        List<ItemDto> cartItems = Collections.singletonList(
                new ItemDto(1L, "Item", "Desc", "/img.jpg", 10.0, 1)
        );

        when(cartRepo.getAllCartPositions()).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.error(new RuntimeException("Save failed")));

        // When
        Mono<Void> result = orderService.makeOrder();

        // Then
        StepVerifier.create(result)
                .verifyError(RuntimeException.class);

        verify(cartRepo).getAllCartPositions();
        verify(orderRepo).save(any(Order.class));
        verify(cartRepo, never()).clearCart();
    }

    @Test
    void makeOrder_ShouldCallMethodsInCorrectOrder() {
        // Given
        List<ItemDto> cartItems = Collections.singletonList(
                new ItemDto(1L, "Item", "Desc", "/img.jpg", 10.0, 1)
        );

        Order savedOrder = new Order();
        savedOrder.setId(1L);

        when(cartRepo.getAllCartPositions()).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(cartRepo.clearCart()).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = orderService.makeOrder();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        InOrder inOrder = inOrder(cartRepo, orderRepo, cartRepo);
        inOrder.verify(cartRepo).getAllCartPositions();
        inOrder.verify(orderRepo).save(any(Order.class));
        inOrder.verify(cartRepo).clearCart();
    }

    private Order createOrder(Long id, double totalSum, int itemCount) {
        Order order = new Order();
        order.setId(id);
        order.setTotalSum(totalSum);

        List<OrderPosition> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            OrderPosition op = new OrderPosition();
            op.setId((long) (i + 1));
            op.setOrderId(id);
            op.setTitle("Item " + (i + 1));
            op.setPrice(10.0);
            op.setCount(1);
            items.add(op);
        }
        order.setItems(items);

        return order;
    }
}