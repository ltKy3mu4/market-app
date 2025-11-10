package org.yandex.mymarketapp.service;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.domain.OrderPosition;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.OrderDto;
import org.yandex.mymarketapp.model.dto.OrdersDto;
import org.yandex.mymarketapp.model.exception.OrderCreateException;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;
import org.yandex.mymarketapp.model.mapper.OrderMapperImpl;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.OrderRepository;
import org.yandex.payment.model.UserBalance;
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

    @MockitoBean
    private org.openapitools.client.api.PaymentsApi payApi;

    @Autowired
    private OrderService orderService;
    
    private Long userId = 0L;

    @Test
    void makeOrder_WithMultipleItems_ShouldCreateOrderWithCorrectTotal() {
        List<ItemDto> cartItems = Arrays.asList(
                new ItemDto(1L, "Item 1", "Description 1", "/img1.jpg", 10.0, 2),
                new ItemDto(2L, "Item 2", "Description 2", "/img2.jpg", 15.0, 3),
                new ItemDto(3L, "Item 3", "Description 3", "/img3.jpg", 5.0, 1)
        );

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setTotalSum(70.0); // (10*2) + (15*3) + (5*1) = 20 + 45 + 5 = 70

        when(cartRepo.getAllCartPositions(userId)).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(cartRepo.clearCart(userId)).thenReturn(Mono.just(1));
        when(payApi.processPayment(any(), any(org.yandex.payment.model.PaymentRequest.class))).thenReturn(Mono.just(new UserBalance().id(userId).balance(100.0f)));


        Mono<Void> result = orderService.makeOrder(userId);

        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).getAllCartPositions(userId);
        verify(orderRepo).save(any(Order.class));
        verify(cartRepo).clearCart(userId);
    }

    @Test
    void makeOrder_WithSingleItem_ShouldCreateOrderWithSingleItem() {
        List<ItemDto> cartItems = Collections.singletonList(
                new ItemDto(1L, "Single Item", "Description", "/img.jpg", 25.0, 1)
        );

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setTotalSum(25.0);

        when(cartRepo.getAllCartPositions(userId)).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(cartRepo.clearCart(userId)).thenReturn(Mono.just(1));
        when(payApi.processPayment(any(), any(org.yandex.payment.model.PaymentRequest.class))).thenReturn(Mono.just(new UserBalance().id(userId).balance(100.0f)));


        Mono<Void> result = orderService.makeOrder(userId);

        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).getAllCartPositions(userId);
        verify(orderRepo).save(argThat(order ->
                order.getTotalSum() == 25.0
        ));
        verify(cartRepo).clearCart(userId);
    }

    @Test
    void makeOrder_WithEmptyCart_ShouldCreateEmptyOrder() {
        when(cartRepo.getAllCartPositions(userId)).thenReturn(Flux.empty());

        Mono<Void> result = orderService.makeOrder(userId);

        StepVerifier.create(result)
                .verifyError(OrderCreateException.class);
    }

    @Test
    void getAllOrders_WhenNoOrdersExist_ShouldReturnEmptyFlux() {
        when(orderRepo.getAllWithPositions(userId)).thenReturn(Flux.empty());

        Mono<OrdersDto> result = orderService.getAllOrders(userId);

        StepVerifier.create(result)
                .expectNext(new OrdersDto(List.of()));

        verify(orderRepo).getAllWithPositions(userId);
    }

    @Test
    void getAllOrders_WhenOrdersExist_ShouldReturnOrderDtos() {
        Order order1 = createOrder(1L, 50.0, 2);
        Order order2 = createOrder(2L, 75.0, 1);

        when(orderRepo.getAllWithPositions(userId)).thenReturn(Flux.just(order1, order2));

        Mono<OrdersDto> result = orderService.getAllOrders(userId);

        StepVerifier.create(result)
                .expectNextCount(1)
                .expectNextMatches(dto -> dto.orders().size() == 2);

        verify(orderRepo).getAllWithPositions(userId);
    }

    @Test
    void getOrderById_WhenOrderNotExists_ShouldThrowException() {
        // Given
        Long orderId = 999L;
        when(orderRepo.getByIdAndUserIdWithPositions(orderId, userId)).thenReturn(Mono.empty());

        // When
        Mono<OrderDto> result = orderService.getOrderById(orderId, userId);

        // Then
        StepVerifier.create(result)
                .verifyErrorMatches(throwable ->
                        throwable instanceof OrderNotFoundException &&
                                throwable.getMessage().equals("order with id 999 not found!")
                );

        verify(orderRepo).getByIdAndUserIdWithPositions(orderId, userId);
    }

    @Test
    void getOrderById_WhenOrderExists_ShouldReturnOrderDto() {
        // Given
        Long orderId = 1L;
        Order order = createOrder(orderId, 100.0, 2);
        when(orderRepo.getByIdAndUserIdWithPositions(orderId, userId)).thenReturn(Mono.just(order));

        // When
        Mono<OrderDto> result = orderService.getOrderById(orderId, userId);

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(orderRepo).getByIdAndUserIdWithPositions(orderId, userId);
    }

    @Test
    void getOrderById_WithZeroId_ShouldThrowException() {
        // Given
        Long orderId = 0L;
        when(orderRepo.getByIdAndUserIdWithPositions(orderId, userId)).thenReturn(Mono.empty());

        // When
        Mono<OrderDto> result = orderService.getOrderById(orderId, userId);

        // Then
        StepVerifier.create(result)
                .verifyError(OrderNotFoundException.class);

        verify(orderRepo).getByIdAndUserIdWithPositions(orderId, userId);
    }

    @Test
    void getOrderById_WithNegativeId_ShouldThrowException() {
        // Given
        Long orderId = -1L;
        when(orderRepo.getByIdAndUserIdWithPositions(orderId, userId)).thenReturn(Mono.empty());

        // When
        Mono<OrderDto> result = orderService.getOrderById(orderId, userId);

        // Then
        StepVerifier.create(result)
                .verifyError(OrderNotFoundException.class);

        verify(orderRepo).getByIdAndUserIdWithPositions(orderId, userId);
    }

    @Test
    void makeOrder_WithLargeQuantities_ShouldCalculateCorrectTotal() {
        List<ItemDto> cartItems = Arrays.asList(
                new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 0.99, 100),  // 0.99 * 100 = 99
                new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 999.99, 2)   // 999.99 * 2 = 1999.98
        );

        when(cartRepo.getAllCartPositions(userId)).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return Mono.just(order);
        });
        when(cartRepo.clearCart(userId)).thenReturn(Mono.just(1));
        when(payApi.processPayment(any(), any(org.yandex.payment.model.PaymentRequest.class))).thenReturn(Mono.just(new UserBalance().id(userId).balance(100.0f)));


        Mono<Void> result = orderService.makeOrder(userId);

        StepVerifier.create(result)
                .verifyComplete();

        verify(orderRepo).save(argThat(order ->
                Math.abs(2098.98 - order.getTotalSum()) < 0.001
        ));
        verify(cartRepo).clearCart(userId);
    }

    @Test
    void makeOrder_WhenClearCartFails_ShouldPropagateError() {
        // Given
        List<ItemDto> cartItems = Collections.singletonList(
                new ItemDto(1L, "Item", "Desc", "/img.jpg", 10.0, 1)
        );

        Order savedOrder = new Order();
        savedOrder.setTotalSum(20.0);
        savedOrder.setId(1L);

        when(cartRepo.getAllCartPositions(userId)).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(cartRepo.clearCart(userId)).thenReturn(Mono.error(new RuntimeException("Clear cart failed")));
        when(payApi.processPayment(any(), any(org.yandex.payment.model.PaymentRequest.class))).thenReturn(Mono.just(new UserBalance().id(userId).balance(100.0f)));


        // When
        Mono<Void> result = orderService.makeOrder(userId);

        // Then
        StepVerifier.create(result)
                .verifyError(RuntimeException.class);

        verify(cartRepo).getAllCartPositions(userId);
        verify(orderRepo).save(any(Order.class));
        verify(payApi).processPayment(any(), any(org.yandex.payment.model.PaymentRequest.class));
        verify(cartRepo).clearCart(userId);
    }

    @Test
    void makeOrder_WhenSaveOrderFails_ShouldNotclearCart() {
        // Given
        List<ItemDto> cartItems = Collections.singletonList(
                new ItemDto(1L, "Item", "Desc", "/img.jpg", 10.0, 1)
        );

        when(cartRepo.getAllCartPositions(userId)).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.error(new RuntimeException("Save failed")));
        when(payApi.processPayment(any(), any(org.yandex.payment.model.PaymentRequest.class))).thenReturn(Mono.just(new UserBalance().id(userId).balance(100.0f)));

        // When
        Mono<Void> result = orderService.makeOrder(userId);

        // Then
        StepVerifier.create(result)
                .verifyError(RuntimeException.class);

        verify(cartRepo).getAllCartPositions(userId);
        verify(orderRepo).save(any(Order.class));
        verify(cartRepo, never()).clearCart(userId);
    }

    @Test
    void makeOrder_WhenPayOrderFails_ShouldNotclearCart() {
        // Given
        List<ItemDto> cartItems = Collections.singletonList(
                new ItemDto(1L, "Item", "Desc", "/img.jpg", 10.0, 1)
        );

        when(cartRepo.getAllCartPositions(userId)).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return Mono.just(order);
        });
        when(payApi.processPayment(any(), any(org.yandex.payment.model.PaymentRequest.class))).thenThrow(WebClientResponseException.class);

        Mono<Void> result = orderService.makeOrder(userId);

        StepVerifier.create(result)
                .verifyError(RuntimeException.class);

        verify(cartRepo).getAllCartPositions(userId);
        verify(orderRepo).save(any(Order.class));
        verify(cartRepo, never()).clearCart(userId);
    }

    @Test
    void makeOrder_ShouldCallMethodsInCorrectOrder() {
        List<ItemDto> cartItems = Collections.singletonList(
                new ItemDto(1L, "Item", "Desc", "/img.jpg", 10.0, 1)
        );

        Order savedOrder = new Order();
        savedOrder.setTotalSum(20.0);
        savedOrder.setId(1L);

        when(cartRepo.getAllCartPositions(userId)).thenReturn(Flux.fromIterable(cartItems));
        when(orderRepo.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(cartRepo.clearCart(userId)).thenReturn(Mono.just(1));
        when(payApi.processPayment(any(), any(org.yandex.payment.model.PaymentRequest.class))).thenReturn(Mono.just(new UserBalance().id(userId).balance(100.0f)));


        Mono<Void> result = orderService.makeOrder(userId);

        StepVerifier.create(result)
                .verifyComplete();

        InOrder inOrder = inOrder(cartRepo, orderRepo, payApi, cartRepo);
        inOrder.verify(cartRepo).getAllCartPositions(userId);
        inOrder.verify(orderRepo).save(any(Order.class));
        inOrder.verify(payApi).processPayment(any(), any());
        inOrder.verify(cartRepo).clearCart(userId);
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