package org.yandex.mymarketapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.mapper.ItemMapper;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final ItemRepository itemsRepo;
    private final CartPositionsRepository cartRepo;
    private final ItemMapper itemMapper;

    @Transactional
    public Mono<Void> increaseQuantityInCart(Long itemId) {
        return cartRepo.findByItemId(itemId)
                .hasElement()
                .flatMap(itemExists -> {
                    if (itemExists) {
                        log.info("Count of items with id {} increased", itemId);
                        return cartRepo.increaseItemCount(itemId).then();
                    } else {
                        return itemsRepo.findById(itemId)
                                .switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found with id: " + itemId)))
                                .doOnNext(item -> log.info("Saved item with id {} increased", item.getId()))
                                .flatMap(item -> cartRepo.save(new CartPosition(item.getId())))
                                .then();
                    }
                });
    }

    @Transactional
    public Mono<Void> decreaseQuantityInCart(Long itemId) {
        return cartRepo.findByItemId(itemId)
                .flatMap(cartPosition -> {
                    if (cartPosition.getCount() <= 1) {
                        log.info("Count of items with id {} is 1 or less, removing from cart", itemId);
                        return removeFromCart(itemId);
                    } else {
                        log.info("Count of items with id {} decreased", itemId);
                        return cartRepo.decreaseItemCount(itemId).then();
                    }
                })
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.info("there is nothing to remove from cart for id {}", itemId)
                ));
    }

    @Transactional
    public Mono<Void> removeFromCart(Long itemId) {
        return cartRepo.removeItemFromCartByItemId(itemId)
                .doFirst(() -> log.info("removing position for id {}", itemId))
                .doOnNext(res -> {
                    if (res > 0) {
                        log.info("successfully removed position from cart for id {}", itemId);
                    } else {
                        log.warn("Positions for item id {} was not found for cart", itemId);
                    }
                }).then();
    }

    public Flux<ItemDto> getCartItems() {
        return cartRepo.findAll()
                .flatMap(cp -> itemsRepo.getItemById(cp.getItemId())
                        .switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found: " + cp.getItemId())))
                        .map(item -> itemMapper.toDto(item, cp.getCount())));
    }
}
