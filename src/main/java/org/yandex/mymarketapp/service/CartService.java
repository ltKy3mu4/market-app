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
    public Mono<Void> increaseQuantityInCart(Long itemId, Long userId) {
        return cartRepo.findByItemIdAndUserId(itemId, userId)
                .hasElement()
                .flatMap(itemExists -> {
                    if (itemExists) {
                        log.info("Count of items with id {} for user {} increased", itemId, userId);
                        return cartRepo.increaseItemCount(itemId, userId).then();
                    } else {
                        return itemsRepo.findById(itemId)
                                .switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found with id " + itemId + " for user "+ userId)))
                                .doOnNext(item -> log.info("Saved item with id {} for user {} increased", item.getId(), userId))
                                .flatMap(item -> cartRepo.save(new CartPosition(item.getId(), userId)))
                                .then();
                    }
                });
    }

    @Transactional
    public Mono<Void> decreaseQuantityInCart(Long itemId, Long userId) {
        return cartRepo.findByItemIdAndUserId(itemId, userId)
                .flatMap(cartPosition -> {
                    if (cartPosition.getCount() <= 1) {
                        log.info("Count of items with id {} is 1 or less, removing from cart", itemId);
                        return removeFromCart(itemId, userId);
                    } else {
                        log.info("Count of items with id {} for user {} decreased", itemId, userId);
                        return cartRepo.decreaseItemCount(itemId, userId).then();
                    }
                })
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.info("there is nothing to remove from cart for id {} for user {}", itemId, userId)
                ));
    }

    @Transactional
    public Mono<Void> removeFromCart(Long itemId, Long userId) {
        return cartRepo.removeItemFromCartByItemId(itemId, userId)
                .doFirst(() -> log.info("removing position for id {} for user {}", itemId, userId))
                .doOnNext(res -> {
                    if (res > 0) {
                        log.info("successfully removed position from cart for id {} for user {}", itemId, userId);
                    } else {
                        log.warn("Positions for item id {} for user {} was not found for cart", itemId, userId);
                    }
                }).then();
    }

    public Flux<ItemDto> getCartItems(Long userId) {
        return cartRepo.findByUserId(userId)
                .flatMap(cp -> itemsRepo.getItemById(cp.getItemId())
                        .switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found: " + cp.getItemId())))
                        .map(item -> itemMapper.toDto(item, cp.getCount())));
    }
}
