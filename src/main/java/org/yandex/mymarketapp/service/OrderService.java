package org.yandex.mymarketapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.OrderDto;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;
import org.yandex.mymarketapp.model.mapper.OrderMapper;
import org.yandex.mymarketapp.repo.OrderRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderMapper orderMapper;

    @Transactional
    public void makeOrder(List<ItemDto> items) {
        Order order = new Order();
        var orderItems = orderMapper.toEntities(items);
        orderItems.forEach(e-> e.setOrder(order));
        order.setItems(orderItems);
        order.setTotalSum(orderItems.stream().mapToDouble(e -> e.getPrice()*e.getCount()).sum());
        orderRepo.save(order);
    }

    public List<OrderDto> getAllOrders() {
        List<Order> orders = orderRepo.getAllWithPositions();
        return orderMapper.toDtos(orders);
    }

    public OrderDto getOrderById(Long orderId) {
        return orderMapper.toDto(orderRepo.getByIdWithPositions(orderId).orElseThrow(() -> new OrderNotFoundException("order with id " + orderId + " not found!")));
    }

}
