package org.yandex.mymarketapp.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.CartItemsDto;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.mapper.ItemMapperImpl;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;
import org.yandex.payment.model.UserBalance;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CartService.class, ItemMapperImpl.class})
class CartServiceTest {

    @MockitoBean
    private ItemRepository itemsRepo;

    @MockitoBean
    private CartPositionsRepository cartRepo;

    @MockitoBean
    private org.openapitools.client.api.BalanceApi balanceApi;

    @Autowired
    private CartService cartService;

    private Long userId =0L; 
    
    @Test
    void increaseQuantityInCart_WhenItemNotInCart_ShouldCreateNewCartPosition() {
        // Given
        Long itemId = 1L;
        Item mockItem = new Item();
        mockItem.setId(itemId);
        mockItem.setTitle("Test Item");
        mockItem.setPrice(10.0);

        CartPosition newCartPosition = new CartPosition(itemId, userId);
        newCartPosition.setId(1L);

        when(cartRepo.findByItemIdAndUserId(itemId, userId)).thenReturn(Mono.empty());
        when(itemsRepo.findById(itemId)).thenReturn(Mono.just(mockItem));
        when(cartRepo.save(any(CartPosition.class))).thenReturn(Mono.just(newCartPosition));

        // When
        Mono<Void> result = cartService.increaseQuantityInCart(itemId, userId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemIdAndUserId(itemId, userId);
        verify(itemsRepo).findById(itemId);
        verify(cartRepo).save(argThat(cartPosition ->
                cartPosition.getItemId().equals(itemId) && cartPosition.getCount() == 1
        ));
        verify(cartRepo, never()).increaseItemCount(itemId, userId);
    }

    @Test
    void increaseQuantityInCart_WhenItemAlreadyInCart_ShouldIncreaseCount() {
        // Given
        Long itemId = 1L;
        CartPosition existingCartPosition = new CartPosition(itemId, userId);
        existingCartPosition.setId(1L);
        existingCartPosition.setCount(2);

        when(cartRepo.findByItemIdAndUserId(itemId, userId)).thenReturn(Mono.just(existingCartPosition));
        when(cartRepo.increaseItemCount(itemId, userId)).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = cartService.increaseQuantityInCart(itemId, userId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemIdAndUserId(itemId, userId);
        verify(cartRepo).increaseItemCount(itemId, userId);
        verify(itemsRepo, never()).findById(itemId);  // This should not be called
        verify(cartRepo, never()).save(any());
    }

    @Test
    void increaseQuantityInCart_WhenItemNotFound_ShouldThrowException() {
        // Given
        Long itemId = 999L;
        when(cartRepo.findByItemIdAndUserId(itemId, userId)).thenReturn(Mono.empty());
        when(itemsRepo.findById(itemId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = cartService.increaseQuantityInCart(itemId, userId);

        // Then
        StepVerifier.create(result)
                .verifyError(ItemNotFoundException.class);

        verify(cartRepo).findByItemIdAndUserId(itemId, userId);
        verify(itemsRepo).findById(itemId);
        verify(cartRepo, never()).save(any());
        verify(cartRepo, never()).increaseItemCount(any(), any());
    }

    @Test
    void decreaseQuantityInCart_WhenItemNotInCart_ShouldDoNothing() {
        // Given
        Long itemId = 1L;
        when(cartRepo.findByItemIdAndUserId(itemId, userId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = cartService.decreaseQuantityInCart(itemId, userId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemIdAndUserId(itemId, userId);
        verify(cartRepo, never()).decreaseItemCount(any(), any());
        verify(cartRepo, never()).removeItemFromCartByItemId(any(), any());
    }

    @Test
    void decreaseQuantityInCart_WhenCountGreaterThanOne_ShouldDecreaseCount() {
        // Given
        Long itemId = 1L;
        CartPosition existingCartPosition = new CartPosition(itemId, userId);
        existingCartPosition.setId(1L);
        existingCartPosition.setCount(3);

        when(cartRepo.findByItemIdAndUserId(itemId, userId)).thenReturn(Mono.just(existingCartPosition));
        when(cartRepo.decreaseItemCount(itemId, userId)).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = cartService.decreaseQuantityInCart(itemId, userId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemIdAndUserId(itemId, userId);
        verify(cartRepo).decreaseItemCount(itemId, userId);
        verify(cartRepo, never()).removeItemFromCartByItemId(itemId, userId);
    }

    @Test
    void decreaseQuantityInCart_WhenCountIsOne_ShouldRemoveFromCart() {
        // Given
        Long itemId = 1L;
        CartPosition existingCartPosition = new CartPosition(itemId, userId);
        existingCartPosition.setId(1L);
        existingCartPosition.setCount(1);

        when(cartRepo.findByItemIdAndUserId(itemId, userId)).thenReturn(Mono.just(existingCartPosition));
        when(cartRepo.removeItemFromCartByItemId(itemId, userId)).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = cartService.decreaseQuantityInCart(itemId, userId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).findByItemIdAndUserId(itemId, userId);
        verify(cartRepo).removeItemFromCartByItemId(itemId, userId);
        verify(cartRepo, never()).decreaseItemCount(itemId, userId);
    }

    @Test
    void removeFromCart_WhenItemExists_ShouldRemoveAndLogSuccess() {
        // Given
        Long itemId = 1L;
        when(cartRepo.removeItemFromCartByItemId(itemId, userId)).thenReturn(Mono.just(1));

        // When
        Mono<Void> result = cartService.removeFromCart(itemId, userId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).removeItemFromCartByItemId(itemId, userId);
    }

    @Test
    void removeFromCart_WhenItemNotExists_ShouldLogWarning() {
        // Given
        Long itemId = 999L;
        when(cartRepo.removeItemFromCartByItemId(itemId, userId)).thenReturn(Mono.just(0));

        // When
        Mono<Void> result = cartService.removeFromCart(itemId, userId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepo).removeItemFromCartByItemId(itemId, userId);
    }

    @Test
    void getCartItems_WhenCartIsEmpty_ShouldReturnEmptyList() {
        when(cartRepo.findByUserId(userId)).thenReturn(Flux.empty());

        Mono<CartItemsDto> result = cartService.getCartItems(userId);

        StepVerifier.create(result)
                .expectNextMatches(e-> e.items().isEmpty());

        verify(cartRepo).findByUserId(userId);
        verify(itemsRepo, never()).getItemById(any());
    }

    @Test
    void getCartItems_WhenItemNotFound_ShouldThrowException() {
        // Given
        Long itemId = 1L;
        CartPosition cp = new CartPosition(itemId, userId);
        cp.setId(1L);
        cp.setCount(1);

        when(cartRepo.findByUserId(userId)).thenReturn(Flux.just(cp));
        when(itemsRepo.getItemById(itemId)).thenReturn(Mono.empty());

        // When
        Mono<CartItemsDto> result = cartService.getCartItems(userId);

        // Then
        StepVerifier.create(result)
                .verifyError(ItemNotFoundException.class);

        verify(cartRepo).findByUserId(userId);
        verify(itemsRepo).getItemById(itemId);
    }

    @Test
    void getCartItems_WhenItemsExist_ShouldReturnItemDtos() {
        Long itemId = 1L;
        CartPosition cp = new CartPosition(itemId, userId);
        cp.setId(1L);
        cp.setCount(2);

        Item item = new Item();
        item.setId(itemId);
        item.setTitle("Test Item");
        item.setPrice(10.0);

        when(cartRepo.findByUserId(userId)).thenReturn(Flux.just(cp));
        when(itemsRepo.getItemById(itemId)).thenReturn(Mono.just(item));

        Mono<CartItemsDto> result = cartService.getCartItems(userId);

        StepVerifier.create(result)
                .expectNext(new CartItemsDto(List.of(new ItemDto(itemId, "Test Item", null, null, 10.0, 2))))
                .verifyComplete();

        verify(cartRepo).findByUserId(userId);
        verify(itemsRepo).getItemById(itemId);
    }

    @Test
    void decreaseQuantityInCart_WhenMultipleCalls_ShouldHandleCorrectly() {
        // Given - First call: count > 1
        Long itemId = 1L;
        CartPosition cartPosition = new CartPosition(itemId, userId);
        cartPosition.setId(1L);
        cartPosition.setCount(3);

        when(cartRepo.findByItemIdAndUserId(itemId, userId)).thenReturn(Mono.just(cartPosition));
        when(cartRepo.decreaseItemCount(itemId, userId)).thenReturn(Mono.just(1));

        // When - First decrease (3 -> 2)
        Mono<Void> firstCall = cartService.decreaseQuantityInCart(itemId, userId);

        // Then
        StepVerifier.create(firstCall)
                .verifyComplete();

        verify(cartRepo).decreaseItemCount(itemId, userId);

        // Reset mocks for second call
        reset(cartRepo);

        // Given - Second call: count == 1
        cartPosition.setCount(1);
        when(cartRepo.findByItemIdAndUserId(itemId, userId)).thenReturn(Mono.just(cartPosition));
        when(cartRepo.removeItemFromCartByItemId(itemId, userId)).thenReturn(Mono.just(1));

        // When - Second decrease (1 -> remove)
        Mono<Void> secondCall = cartService.decreaseQuantityInCart(itemId, userId);

        // Then
        StepVerifier.create(secondCall)
                .verifyComplete();

        verify(cartRepo).removeItemFromCartByItemId(itemId, userId);
        verify(cartRepo, never()).decreaseItemCount(itemId, userId);
    }

    @Test
    void increaseQuantityInCart_WhenSaveFails_ShouldPropagateError() {
        // Given
        Long itemId = 1L;
        Item mockItem = new Item();
        mockItem.setId(itemId);

        when(cartRepo.findByItemIdAndUserId(itemId, userId)).thenReturn(Mono.empty());
        when(itemsRepo.findById(itemId)).thenReturn(Mono.just(mockItem));
        when(cartRepo.save(any(CartPosition.class))).thenReturn(Mono.error(new RuntimeException("DB error")));

        Mono<Void> result = cartService.increaseQuantityInCart(itemId, userId);

        StepVerifier.create(result)
                .verifyError(RuntimeException.class);

        verify(cartRepo).findByItemIdAndUserId(itemId, userId);
        verify(itemsRepo).findById(itemId);
        verify(cartRepo).save(any(CartPosition.class));
    }

    @Test
    void isMoneyEnoughToBuy_WhenTrue() {
        Long userId = 1L;
        when(balanceApi.getUserBalance(1L)).thenReturn(Mono.just(new UserBalance().id(1L).balance(10.0f)));

        Mono<Boolean> result = cartService.isMoneyEnoughToBuy(4.0, userId);

        StepVerifier.create(result).expectNext(true).verifyComplete();
    }

    @Test
    void isMoneyEnoughToBuy_WhenFalse() {
        Long userId = 1L;
        when(balanceApi.getUserBalance(1L)).thenReturn(Mono.just(new UserBalance().id(1L).balance(10.0f)));

        Mono<Boolean> result = cartService.isMoneyEnoughToBuy(40.0, userId);

        StepVerifier.create(result).expectNext(false).verifyComplete();
    }

}