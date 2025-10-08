package org.yandex.mymarketapp.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.domain.OrderPosition;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.OrderDto;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;
import org.yandex.mymarketapp.model.mapper.ItemMapperImpl;
import org.yandex.mymarketapp.model.mapper.OrderMapper;
import org.yandex.mymarketapp.model.mapper.OrderMapperImpl;
import org.yandex.mymarketapp.repo.OrderRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {OrderService.class, OrderMapperImpl.class})
class OrderServiceTest {

    @MockitoBean
    private OrderRepository orderRepo;

    @Autowired
    private OrderService orderService;

    @Test
    void makeOrder_WithMultipleItems_ShouldCreateOrderWithCorrectTotal() {
        List<ItemDto> itemDtos = Arrays.asList(
                new ItemDto(1L, "Item 1", "Description 1", "/img1.jpg", 10.0, 2),
                new ItemDto(2L, "Item 2", "Description 2", "/img2.jpg", 15.0, 3),
                new ItemDto(3L, "Item 3", "Description 3", "/img3.jpg", 5.0, 1)
        );

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        orderService.makeOrder(itemDtos);

        verify(orderRepo).save(argThat(order -> {
            assertNotNull(order.getItems());
            assertEquals(3, order.getItems().size());
            assertEquals(70.0, order.getTotalSum(), 0.001);

            return order.getItems().stream().allMatch(op -> op.getOrder() == order);
        }));
    }

    @Test
    void makeOrder_WithSingleItem_ShouldCreateOrderWithSingleItem() {
        List<ItemDto> itemDtos = Collections.singletonList(
                new ItemDto(1L, "Single Item", "Description", "/img.jpg", 25.0, 1)
        );

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        orderService.makeOrder(itemDtos);

        verify(orderRepo).save(argThat(order -> {
            assertEquals(1, order.getItems().size());
            assertEquals(25.0, order.getTotalSum(), 0.001);
            return true;
        }));
    }

    @Test
    void makeOrder_WithEmptyItems_ShouldCreateEmptyOrder() {
        List<ItemDto> itemDtos = Collections.emptyList();
        List<OrderPosition> orderPositions = Collections.emptyList();

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        orderService.makeOrder(itemDtos);

        verify(orderRepo).save(argThat(order -> {
            assertTrue(order.getItems().isEmpty());
            assertEquals(0.0, order.getTotalSum(), 0.001);
            return true;
        }));
    }



    @Test
    void getAllOrders_WhenNoOrdersExist_ShouldReturnEmptyList() {
        List<Order> mockOrders = Collections.emptyList();

        when(orderRepo.getAllWithPositions()).thenReturn(mockOrders);

        List<OrderDto> result = orderService.getAllOrders();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderRepo).getAllWithPositions();
    }

    @Test
    void getOrderById_WhenOrderNotExists_ShouldThrowException() {
        Long orderId = 999L;
        when(orderRepo.getByIdWithPositions(orderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(orderId));

        assertEquals("order with id 999 not found!", exception.getMessage());
        verify(orderRepo).getByIdWithPositions(orderId);
    }

    @Test
    void getOrderById_WithZeroId_ShouldThrowException() {
        Long orderId = 0L;
        when(orderRepo.getByIdWithPositions(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(orderId));

        verify(orderRepo).getByIdWithPositions(orderId);
    }

    @Test
    void getOrderById_WithNegativeId_ShouldThrowException() {
        Long orderId = -1L;
        when(orderRepo.getByIdWithPositions(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(orderId));

        verify(orderRepo).getByIdWithPositions(orderId);
    }

    @Test
    void makeOrder_ShouldSetOrderReferenceForAllPositions() {
        List<ItemDto> itemDtos = Arrays.asList(
                new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 10.0, 1),
                new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 20.0, 1)
        );

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        orderService.makeOrder(itemDtos);

        verify(orderRepo).save(argThat(order -> order.getItems().stream().allMatch(op -> op.getOrder() == order)));
    }

    @Test
    void makeOrder_WithLargeQuantities_ShouldCalculateCorrectTotal() {
        List<ItemDto> itemDtos = Arrays.asList(
                new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 0.99, 100),  // 0.99 * 100 = 99
                new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 999.99, 2)   // 999.99 * 2 = 1999.98
        );
        // Total: 99 + 1999.98 = 2098.98

        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        orderService.makeOrder(itemDtos);

        verify(orderRepo).save(argThat(order -> Math.abs(2098.98 - order.getTotalSum()) < 0.001));
    }

    private Order createOrder(Long id, double totalSum, int itemCount) {
        Order order = new Order();
        order.setId(id);
        order.setTotalSum(totalSum);

        List<OrderPosition> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            OrderPosition op = new OrderPosition();
            op.setId((long) (i + 1));
            op.setOrder(order);
            items.add(op);
        }
        order.setItems(items);

        return order;
    }

    private OrderPosition createOrderPosition(Long id, String title, double price, int count) {
        OrderPosition op = new OrderPosition();
        op.setId(id);
        op.setTitle(title);
        op.setPrice(price);
        op.setCount(count);
        return op;
    }
}