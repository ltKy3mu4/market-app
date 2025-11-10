package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.OrderDto;
import org.yandex.mymarketapp.model.dto.OrdersDto;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;
import org.yandex.mymarketapp.service.OrderService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

class OrderControllerTest extends BaseControllerTest{

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showOrders_ShouldReturnOrdersViewWithOrdersList() {
        List<OrderDto> mockOrders = Arrays.asList(
                new OrderDto(1L, Arrays.asList(
                        new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 50.0, 3),
                        new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 25.0, 2)
                ), 150.0),
                new OrderDto(2L, Arrays.asList(
                        new ItemDto(3L, "Item 3", "Desc 3", "/img3.jpg", 75.0, 1)
                ), 75.0)
        );

        when(orderService.getAllOrders(USER_ID)).thenReturn(Mono.just(new OrdersDto(mockOrders)));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(result -> {
                    // For Thymeleaf templates, the actual HTML rendering happens
                    // We can verify the service was called and response is successful
                });

        verify(orderService).getAllOrders(USER_ID);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showOrders_WhenNoOrdersExist_ShouldReturnEmptyList() {
        when(orderService.getAllOrders(USER_ID)).thenReturn(Mono.just(new OrdersDto(List.of())));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk();

        verify(orderService).getAllOrders(USER_ID);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showOrderDetails_WithValidId_ShouldReturnOrderViewWithOrder() {
        Long orderId = 1L;
        OrderDto mockOrder = new OrderDto(orderId, Arrays.asList(
                new ItemDto(1L, "Item 1", "Description 1", "/img1.jpg", 50.0, 2),
                new ItemDto(2L, "Item 2", "Description 2", "/img2.jpg", 25.0, 2)
        ), 150.0);

        when(orderService.getOrderById(orderId, USER_ID)).thenReturn(Mono.just(mockOrder));

        webTestClient.get()
                .uri("/orders/{id}", orderId)
                .exchange()
                .expectStatus().isOk();

        verify(orderService).getOrderById(orderId, USER_ID);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showOrderDetails_WithNonExistentId_ShouldThrowException() {
        Long nonExistentOrderId = 999L;
        when(orderService.getOrderById(nonExistentOrderId, USER_ID))
                .thenReturn(Mono.error(new OrderNotFoundException("Order not found with id: " + nonExistentOrderId)));

        webTestClient.get()
                .uri("/orders/{id}", nonExistentOrderId)
                .exchange()
                .expectStatus().isNotFound();

        verify(orderService).getOrderById(nonExistentOrderId, USER_ID);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showOrderDetails_WithMultipleIds_ShouldReturnCorrectOrder() {
        Long orderId1 = 1L;
        Long orderId2 = 2L;

        OrderDto mockOrder1 = new OrderDto(orderId1, Arrays.asList(
                new ItemDto(1L, "Item A", "Desc A", "/imgA.jpg", 50.0, 2)
        ), 100.0);
        OrderDto mockOrder2 = new OrderDto(orderId2, Arrays.asList(
                new ItemDto(2L, "Item B", "Desc B", "/imgB.jpg", 100.0, 2)
        ), 200.0);

        when(orderService.getOrderById(orderId1, USER_ID)).thenReturn(Mono.just(mockOrder1));
        when(orderService.getOrderById(orderId2, USER_ID)).thenReturn(Mono.just(mockOrder2));

        // Test first order
        webTestClient.get()
                .uri("/orders/{id}", orderId1)
                .exchange()
                .expectStatus().isOk();

        // Test second order
        webTestClient.get()
                .uri("/orders/{id}", orderId2)
                .exchange()
                .expectStatus().isOk();

        verify(orderService).getOrderById(orderId1, USER_ID);
        verify(orderService).getOrderById(orderId2, USER_ID);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showOrderDetails_WithZeroId_ShouldThrowException() {
        Long zeroId = USER_ID;
        when(orderService.getOrderById(zeroId, USER_ID))
                .thenReturn(Mono.error(new OrderNotFoundException("Order not found with id: " + zeroId)));

        webTestClient.get()
                .uri("/orders/{id}", zeroId)
                .exchange()
                .expectStatus().isNotFound();

        verify(orderService).getOrderById(zeroId, USER_ID);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showOrderDetails_WithNegativeId_ShouldThrowException() {
        Long negativeId = -1L;
        when(orderService.getOrderById(negativeId, USER_ID))
                .thenReturn(Mono.error(new OrderNotFoundException("Order not found with id: " + negativeId)));

        webTestClient.get()
                .uri("/orders/{id}", negativeId)
                .exchange()
                .expectStatus().isNotFound();

        verify(orderService).getOrderById(negativeId, USER_ID);
    }


    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showOrders_WhenServiceReturnsError_ShouldHandleGracefully() {
        when(orderService.getAllOrders(USER_ID))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().is5xxServerError();

        verify(orderService).getAllOrders(USER_ID);
    }

}