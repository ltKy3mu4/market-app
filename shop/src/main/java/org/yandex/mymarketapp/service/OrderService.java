package org.yandex.mymarketapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.dto.OrderDto;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;
import org.yandex.mymarketapp.model.mapper.OrderMapper;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.OrderRepository;
import org.yandex.payment.model.PaymentRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CartPositionsRepository cartRepo;
    private final OrderMapper orderMapper;
    private final org.yandex.payment.api.PaymentsApi payApi;

    @Transactional
    public Mono<Void> makeOrder(Long userId) {
        log.info("Making order");
        return cartRepo.getAllCartPositions(userId)
                .collectList()
                .flatMap(items -> {
                    Order order = new Order();
                    var orderItems = orderMapper.toEntities(items);
                    order.setItems(orderItems);
                    order.setTotalSum(orderItems.stream().mapToDouble(e -> e.getPrice()*e.getCount()).sum());
                    return orderRepo.save(order);
                })
                .flatMap(o -> payApi.processPayment(userId, new PaymentRequest().amount(o.getTotalSum().floatValue())))
                .doOnError(throwable -> log.error("Failed to pay order", throwable))
                .doOnNext(b -> log.info("Payment processed for user {}, balance {}", userId, b.getBalance()))
                .flatMap(o -> cartRepo.clearCart(userId))
                .then();
    }

    public Flux<OrderDto> getAllOrders(Long userId) {
        return orderRepo.getAllWithPositions(userId)
                .map(orderMapper::toDto);
    }

    public Mono<OrderDto> getOrderById(Long id, Long userId) {
        return orderRepo.getByIdAndUserIdWithPositions(id, userId)
                .switchIfEmpty(Mono.error(() -> new OrderNotFoundException("order with id " + id + " not found!")))
                .map(orderMapper::toDto);
    }
}
