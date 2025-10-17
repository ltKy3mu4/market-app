package org.yandex.mymarketapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.OrderDto;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;
import org.yandex.mymarketapp.model.mapper.OrderMapper;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.OrderRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CartPositionsRepository cartRepo;
    private final OrderMapper orderMapper;

    @Transactional
    public Mono<Void> makeOrder() {
        log.info("Making order");
        return cartRepo.getAllCartPositions()
                .collectList()
                .flatMap(items -> {
                    Order order = new Order();
                    var orderItems = orderMapper.toEntities(items);
                    order.setItems(orderItems);
                    order.setTotalSum(orderItems.stream().mapToDouble(e -> e.getPrice()*e.getCount()).sum());
                    return orderRepo.save(order);
                })
                .flatMap(o -> cartRepo.clearCart())
                .then();
    }

    public Flux<OrderDto> getAllOrders() {
        return orderRepo.getAllWithPositions()
                .map(orderMapper::toDto);
    }

    public Mono<OrderDto> getOrderById(long id) {
        return orderRepo.getByIdWithPositions(id)
                .switchIfEmpty(Mono.error(() -> new OrderNotFoundException("order with id " + id + " not found!")))
                .map(orderMapper::toDto);
    }
}
