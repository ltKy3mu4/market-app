package org.yandex.mymarketapp.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.mapper.ItemMapperImpl;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CartService.class, ItemMapperImpl.class})
class CartServiceTest {

    @MockitoBean
    private ItemRepository itemsRepo;

    @MockitoBean
    private CartPositionsRepository cartRepo;

    @Autowired
    private CartService cartService;

    @Test
    void increaseQuantityInCart_WhenItemNotInCart_ShouldCreateNewCartPosition() {
        // Given
        Long itemId = 1L;
        Item mockItem = new Item();
        mockItem.setId(itemId);
        mockItem.setTitle("Test Item");
        mockItem.setPrice(10.0);

        CartPosition newCartPosition = new CartPosition(itemId);
        newCartPosition.setId(1L);

        when(cartRepo.findByItemId(itemId)).thenReturn(Mono.empty());
        when(itemsRepo.findById(itemId)).thenReturn(Mono.just(mockItem));
        when(cartRepo.save(any(CartPosition.class))).thenReturn(Mono.just(newCartPosition));

        // When
        Mono<Void> result = cartService.increaseQuantityInCart(itemId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemId(itemId);
        verify(itemsRepo).findById(itemId);
        verify(cartRepo).save(argThat(cartPosition ->
                cartPosition.getItemId().equals(itemId) && cartPosition.getCount() == 1
        ));
        verify(cartRepo, never()).increaseItemCount(itemId);
    }

    @Test
    void increaseQuantityInCart_WhenItemAlreadyInCart_ShouldIncreaseCount() {
        // Given
        Long itemId = 1L;
        CartPosition existingCartPosition = new CartPosition(itemId);
        existingCartPosition.setId(1L);
        existingCartPosition.setCount(2);

        when(cartRepo.findByItemId(itemId)).thenReturn(Mono.just(existingCartPosition));
        when(cartRepo.increaseItemCount(itemId)).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = cartService.increaseQuantityInCart(itemId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemId(itemId);
        verify(cartRepo).increaseItemCount(itemId);
        verify(itemsRepo, never()).findById(itemId);  // This should not be called
        verify(cartRepo, never()).save(any());
    }

    @Test
    void increaseQuantityInCart_WhenItemNotFound_ShouldThrowException() {
        // Given
        Long itemId = 999L;
        when(cartRepo.findByItemId(itemId)).thenReturn(Mono.empty());
        when(itemsRepo.findById(itemId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = cartService.increaseQuantityInCart(itemId);

        // Then
        StepVerifier.create(result)
                .verifyError(ItemNotFoundException.class);

        verify(cartRepo).findByItemId(itemId);
        verify(itemsRepo).findById(itemId);
        verify(cartRepo, never()).save(any());
        verify(cartRepo, never()).increaseItemCount(any());
    }

    @Test
    void decreaseQuantityInCart_WhenItemNotInCart_ShouldDoNothing() {
        // Given
        Long itemId = 1L;
        when(cartRepo.findByItemId(itemId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = cartService.decreaseQuantityInCart(itemId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemId(itemId);
        verify(cartRepo, never()).decreaseItemCount(any());
        verify(cartRepo, never()).removeItemFromCartByItemId(any());
    }

    @Test
    void decreaseQuantityInCart_WhenCountGreaterThanOne_ShouldDecreaseCount() {
        // Given
        Long itemId = 1L;
        CartPosition existingCartPosition = new CartPosition(itemId);
        existingCartPosition.setId(1L);
        existingCartPosition.setCount(3);

        when(cartRepo.findByItemId(itemId)).thenReturn(Mono.just(existingCartPosition));
        when(cartRepo.decreaseItemCount(itemId)).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = cartService.decreaseQuantityInCart(itemId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemId(itemId);
        verify(cartRepo).decreaseItemCount(itemId);
        verify(cartRepo, never()).removeItemFromCartByItemId(itemId);
    }

    @Test
    void decreaseQuantityInCart_WhenCountIsOne_ShouldRemoveFromCart() {
        // Given
        Long itemId = 1L;
        CartPosition existingCartPosition = new CartPosition(itemId);
        existingCartPosition.setId(1L);
        existingCartPosition.setCount(1);

        when(cartRepo.findByItemId(itemId)).thenReturn(Mono.just(existingCartPosition));
        when(cartRepo.removeItemFromCartByItemId(itemId)).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = cartService.decreaseQuantityInCart(itemId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemId(itemId);
        verify(cartRepo).removeItemFromCartByItemId(itemId);
        verify(cartRepo, never()).decreaseItemCount(itemId);
    }

    @Test
    void removeFromCart_WhenItemExists_ShouldRemoveAndLogSuccess() {
        // Given
        Long itemId = 1L;
        when(cartRepo.removeItemFromCartByItemId(itemId)).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = cartService.removeFromCart(itemId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).removeItemFromCartByItemId(itemId);
    }

    @Test
    void removeFromCart_WhenItemNotExists_ShouldLogWarning() {
        // Given
        Long itemId = 999L;
        when(cartRepo.removeItemFromCartByItemId(itemId)).thenReturn(Mono.just(0));

        // When
        Mono<Void> result = cartService.removeFromCart(itemId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).removeItemFromCartByItemId(itemId);
    }

    @Test
    void getCartItems_WhenCartIsEmpty_ShouldReturnEmptyList() {
        // Given
        when(cartRepo.findAll()).thenReturn(Flux.empty());

        // When
        Flux<ItemDto> result = cartService.getCartItems();

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findAll();
        verify(itemsRepo, never()).getItemById(any());
    }

    @Test
    void getCartItems_WhenItemNotFound_ShouldThrowException() {
        // Given
        Long itemId = 1L;
        CartPosition cp = new CartPosition(itemId);
        cp.setId(1L);
        cp.setCount(1);

        when(cartRepo.findAll()).thenReturn(Flux.just(cp));
        when(itemsRepo.getItemById(itemId)).thenReturn(Mono.empty());

        // When
        Flux<ItemDto> result = cartService.getCartItems();

        // Then
        StepVerifier.create(result)
                .verifyError(ItemNotFoundException.class);

        verify(cartRepo).findAll();
        verify(itemsRepo).getItemById(itemId);
    }

    @Test
    void getCartItems_WhenItemsExist_ShouldReturnItemDtos() {
        // Given
        Long itemId = 1L;
        CartPosition cp = new CartPosition(itemId);
        cp.setId(1L);
        cp.setCount(2);

        Item item = new Item();
        item.setId(itemId);
        item.setTitle("Test Item");
        item.setPrice(10.0);

        ItemDto expectedDto = new ItemDto(itemId, "Test Item", "Description", "/img.jpg", 10.0, 2);

        when(cartRepo.findAll()).thenReturn(Flux.just(cp));
        when(itemsRepo.getItemById(itemId)).thenReturn(Mono.just(item));
        // Mock the mapper behavior
        when(itemsRepo.getItemById(itemId)).thenReturn(Mono.just(item));

        // When
        Flux<ItemDto> result = cartService.getCartItems();

        // Then
        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.id() == itemId && dto.count() == 2)
                .verifyComplete();

        verify(cartRepo).findAll();
        verify(itemsRepo).getItemById(itemId);
    }

    @Test
    void decreaseQuantityInCart_WhenMultipleCalls_ShouldHandleCorrectly() {
        // Given - First call: count > 1
        Long itemId = 1L;
        CartPosition cartPosition = new CartPosition(itemId);
        cartPosition.setId(1L);
        cartPosition.setCount(3);

        when(cartRepo.findByItemId(itemId)).thenReturn(Mono.just(cartPosition));
        when(cartRepo.decreaseItemCount(itemId)).thenReturn(Mono.just(1));

        // When - First decrease (3 -> 2)
        Mono<Void> firstCall = cartService.decreaseQuantityInCart(itemId);

        // Then
        StepVerifier.create(firstCall)
                .verifyComplete();

        verify(cartRepo).decreaseItemCount(itemId);

        // Reset mocks for second call
        reset(cartRepo);

        // Given - Second call: count == 1
        cartPosition.setCount(1);
        when(cartRepo.findByItemId(itemId)).thenReturn(Mono.just(cartPosition));
        when(cartRepo.removeItemFromCartByItemId(itemId)).thenReturn(Mono.just(1));

        // When - Second decrease (1 -> remove)
        Mono<Void> secondCall = cartService.decreaseQuantityInCart(itemId);

        // Then
        StepVerifier.create(secondCall)
                .verifyComplete();

        verify(cartRepo).removeItemFromCartByItemId(itemId);
        verify(cartRepo, never()).decreaseItemCount(itemId);
    }

    @Test
    void increaseQuantityInCart_WhenSaveFails_ShouldPropagateError() {
        // Given
        Long itemId = 1L;
        Item mockItem = new Item();
        mockItem.setId(itemId);

        when(cartRepo.findByItemId(itemId)).thenReturn(Mono.empty());
        when(itemsRepo.findById(itemId)).thenReturn(Mono.just(mockItem));
        when(cartRepo.save(any(CartPosition.class))).thenReturn(Mono.error(new RuntimeException("DB error")));

        // When
        Mono<Void> result = cartService.increaseQuantityInCart(itemId);

        // Then
        StepVerifier.create(result)
                .verifyError(RuntimeException.class);

        verify(cartRepo).findByItemId(itemId);
        verify(itemsRepo).findById(itemId);
        verify(cartRepo).save(any(CartPosition.class));
    }

    @Test
    void getCartItems_WithMultipleItems_ShouldReturnAllItems() {
        // Given
        Long itemId1 = 1L;
        Long itemId2 = 2L;

        CartPosition cp1 = new CartPosition(itemId1);
        cp1.setId(1L);
        cp1.setCount(2);

        CartPosition cp2 = new CartPosition(itemId2);
        cp2.setId(2L);
        cp2.setCount(1);

        Item item1 = new Item();
        item1.setId(itemId1);
        item1.setTitle("Item 1");

        Item item2 = new Item();
        item2.setId(itemId2);
        item2.setTitle("Item 2");

        when(cartRepo.findAll()).thenReturn(Flux.just(cp1, cp2));
        when(itemsRepo.getItemById(itemId1)).thenReturn(Mono.just(item1));
        when(itemsRepo.getItemById(itemId2)).thenReturn(Mono.just(item2));

        // When
        Flux<ItemDto> result = cartService.getCartItems();

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(cartRepo).findAll();
        verify(itemsRepo).getItemById(itemId1);
        verify(itemsRepo).getItemById(itemId2);
    }
}