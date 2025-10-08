package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.OrderDto;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.ItemService;
import org.yandex.mymarketapp.service.OrderService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void showOrders_ShouldReturnOrdersViewWithOrdersList() throws Exception {
        List<OrderDto> mockOrders = Arrays.asList(
                new OrderDto(1L,  Arrays.asList(
                        new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 50.0, 3),
                        new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 25.0, 2)
                ),150.0),
                new OrderDto(2L, Arrays.asList(
                        new ItemDto(3L, "Item 3", "Desc 3", "/img3.jpg", 75.0, 1)
                ),75.0)
        );

        when(orderService.getAllOrders()).thenReturn(mockOrders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", mockOrders));

        verify(orderService).getAllOrders();
    }

    @Test
    void showOrders_WhenNoOrdersExist_ShouldReturnEmptyList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attribute("orders", Collections.emptyList()));

        verify(orderService).getAllOrders();
    }

    @Test
    void showOrderDetails_WithValidId_ShouldReturnOrderViewWithOrder() throws Exception {
        Long orderId = 1L;
        OrderDto mockOrder = new OrderDto(orderId,  Arrays.asList(
                new ItemDto(1L, "Item 1", "Description 1", "/img1.jpg", 50.0, 2),
                new ItemDto(2L, "Item 2", "Description 2", "/img2.jpg", 25.0, 2)
        ), 150.0);

        when(orderService.getOrderById(orderId)).thenReturn(mockOrder);

        // When & Then
        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("order", mockOrder));

        verify(orderService).getOrderById(orderId);
    }

    @Test
    void showOrderDetails_WithNonExistentId_ShouldThrowException() throws Exception {
        // Given
        Long nonExistentOrderId = 999L;
        when(orderService.getOrderById(nonExistentOrderId))
                .thenThrow(new OrderNotFoundException("Order not found with id: " + nonExistentOrderId));

        // When & Then
        mockMvc.perform(get("/orders/{id}", nonExistentOrderId))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof OrderNotFoundException));

        verify(orderService).getOrderById(nonExistentOrderId);
    }

    @Test
    void showOrderDetails_WithMultipleIds_ShouldReturnCorrectOrder() throws Exception {
        // Given
        Long orderId1 = 1L;
        Long orderId2 = 2L;

        OrderDto mockOrder1 = new OrderDto(orderId1,  Arrays.asList(
                new ItemDto(1L, "Item A", "Desc A", "/imgA.jpg", 50.0, 2)
        ),100.0);
        OrderDto mockOrder2 = new OrderDto(orderId2, Arrays.asList(
                new ItemDto(2L, "Item B", "Desc B", "/imgB.jpg", 100.0, 2)
        ), 200.0);

        when(orderService.getOrderById(orderId1)).thenReturn(mockOrder1);
        when(orderService.getOrderById(orderId2)).thenReturn(mockOrder2);

        // When & Then - Test first order
        mockMvc.perform(get("/orders/{id}", orderId1))
                .andExpect(status().isOk())
                .andExpect(model().attribute("order", mockOrder1));

        // When & Then - Test second order
        mockMvc.perform(get("/orders/{id}", orderId2))
                .andExpect(status().isOk())
                .andExpect(model().attribute("order", mockOrder2));

        verify(orderService).getOrderById(orderId1);
        verify(orderService).getOrderById(orderId2);
    }

    @Test
    void showOrderDetails_WithZeroId_ShouldThrowException() throws Exception {
        // Given
        Long zeroId = 0L;
        when(orderService.getOrderById(zeroId))
                .thenThrow(new OrderNotFoundException("Order not found with id: " + zeroId));

        // When & Then
        mockMvc.perform(get("/orders/{id}", zeroId))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(zeroId);
    }

    @Test
    void showOrderDetails_WithNegativeId_ShouldThrowException() throws Exception {
        // Given
        Long negativeId = -1L;
        when(orderService.getOrderById(negativeId))
                .thenThrow(new OrderNotFoundException("Order not found with id: " + negativeId));

        // When & Then
        mockMvc.perform(get("/orders/{id}", negativeId))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(negativeId);
    }

    @Test
    void showOrders_ShouldBeAccessibleViaGetMethodOnly() throws Exception {
        // When & Then - POST to /orders should return 405 Method Not Allowed
        mockMvc.perform(post("/orders"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(orderService);
    }

    @Test
    void showOrderDetails_ShouldBeAccessibleViaGetMethodOnly() throws Exception {
        // When & Then - POST to /orders/{id} should return 405 Method Not Allowed
        mockMvc.perform(post("/orders/1"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(orderService);
    }

}