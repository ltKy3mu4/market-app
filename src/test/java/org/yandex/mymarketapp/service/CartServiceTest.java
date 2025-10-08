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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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

        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.empty());
        when(itemsRepo.getItemById(itemId)).thenReturn(Optional.of(mockItem));
        when(cartRepo.save(any(CartPosition.class))).thenAnswer(invocation -> {
            CartPosition cp = invocation.getArgument(0);
            cp.setId(1L); // Simulate saved entity with ID
            return cp;
        });

        cartService.increaseQuantityInCart(itemId);

        verify(cartRepo).findByItemId(itemId);
        verify(itemsRepo).getItemById(itemId);
        verify(cartRepo).save(argThat(cartPosition ->
                cartPosition.getItem().getId()== itemId && cartPosition.getCount() == 1
        ));
        verify(cartRepo, never()).increaseItemCount(itemId);
    }

    @Test
    void increaseQuantityInCart_WhenItemAlreadyInCart_ShouldIncreaseCount() {
        Long itemId = 1L;
        CartPosition existingCartPosition = new CartPosition();
        existingCartPosition.setId(1L);
        existingCartPosition.setCount(2);

        Item mockItem = new Item();
        mockItem.setId(itemId);
        existingCartPosition.setItem(mockItem);

        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.of(existingCartPosition));

        cartService.increaseQuantityInCart(itemId);

        verify(cartRepo).findByItemId(itemId);
        verify(cartRepo).increaseItemCount(itemId);
        verify(itemsRepo, never()).getItemById(itemId);
        verify(cartRepo, never()).save(any());
    }

    @Test
    void increaseQuantityInCart_WhenItemNotFound_ShouldThrowException() {
        Long itemId = 999L;
        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.empty());
        when(itemsRepo.getItemById(itemId)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> cartService.increaseQuantityInCart(itemId));

        verify(cartRepo).findByItemId(itemId);
        verify(itemsRepo).getItemById(itemId);
        verify(cartRepo, never()).save(any());
        verify(cartRepo, never()).increaseItemCount(any());
    }

    @Test
    void decreaseQuantityInCart_WhenItemNotInCart_ShouldDoNothing() {
        Long itemId = 1L;
        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.empty());

        cartService.decreaseQuantityInCart(itemId);

        verify(cartRepo).findByItemId(itemId);
        verify(cartRepo, never()).decreaseItemCount(any());
        verify(cartRepo, never()).removeItemFromCartByItemId(any());
    }

    @Test
    void decreaseQuantityInCart_WhenCountGreaterThanOne_ShouldDecreaseCount() {
        Long itemId = 1L;
        CartPosition existingCartPosition = new CartPosition();
        existingCartPosition.setId(1L);
        existingCartPosition.setCount(3);

        Item mockItem = new Item();
        mockItem.setId(itemId);
        existingCartPosition.setItem(mockItem);

        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.of(existingCartPosition));

        cartService.decreaseQuantityInCart(itemId);

        verify(cartRepo).findByItemId(itemId);
        verify(cartRepo).decreaseItemCount(itemId);
        verify(cartRepo, never()).removeItemFromCartByItemId(itemId);
    }

    @Test
    void decreaseQuantityInCart_WhenCountIsOne_ShouldRemoveFromCart() {
        Long itemId = 1L;
        CartPosition existingCartPosition = new CartPosition();
        existingCartPosition.setId(1L);
        existingCartPosition.setCount(1); // Count == 1

        Item mockItem = new Item();
        mockItem.setId(itemId);
        existingCartPosition.setItem(mockItem);

        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.of(existingCartPosition));
        when(cartRepo.removeItemFromCartByItemId(itemId)).thenReturn(1);

        cartService.decreaseQuantityInCart(itemId);

        verify(cartRepo).findByItemId(itemId);
        verify(cartRepo).removeItemFromCartByItemId(itemId);
        verify(cartRepo, never()).decreaseItemCount(itemId);
    }

    @Test
    void removeFromCart_WhenItemExists_ShouldRemoveAndLogSuccess() {
        Long itemId = 1L;
        when(cartRepo.removeItemFromCartByItemId(itemId)).thenReturn(1);

        cartService.removeFromCart(itemId);

        verify(cartRepo).removeItemFromCartByItemId(itemId);
    }

    @Test
    void removeFromCart_WhenItemNotExists_ShouldLogWarning() {
        Long itemId = 999L;
        when(cartRepo.removeItemFromCartByItemId(itemId)).thenReturn(0);

        cartService.removeFromCart(itemId);

        verify(cartRepo).removeItemFromCartByItemId(itemId);
    }


    @Test
    void getCartItems_WhenCartIsEmpty_ShouldReturnEmptyList() {
        when(cartRepo.findAll()).thenReturn(Collections.emptyList());

        List<ItemDto> result = cartService.getCartItems();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(cartRepo).findAll();
        verify(itemsRepo, never()).getItemById(any());
    }

    @Test
    void getCartItems_WhenItemNotFound_ShouldThrowException() {
        Item item = new Item();
        item.setId(1L);

        CartPosition cp = new CartPosition();
        cp.setId(1L);
        cp.setItem(item);
        cp.setCount(1);

        when(cartRepo.findAll()).thenReturn(Collections.singletonList(cp));
        when(itemsRepo.getItemById(1L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> cartService.getCartItems());
        verify(cartRepo).findAll();
        verify(itemsRepo).getItemById(1L);
    }

    @Test
    void clearCart_ShouldCallRepositoryClear() {
        cartService.clearCart();
        verify(cartRepo).clearCart();
    }

    @Test
    void decreaseQuantityInCart_WhenMultipleCalls_ShouldHandleCorrectly() {
        Long itemId = 1L;
        CartPosition cartPosition = new CartPosition();
        cartPosition.setId(1L);
        cartPosition.setCount(3);

        Item item = new Item();
        item.setId(itemId);
        cartPosition.setItem(item);

        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.of(cartPosition));

        //First decrease (3 -> 2)
        cartService.decreaseQuantityInCart(itemId);

        verify(cartRepo).decreaseItemCount(itemId);

        reset(cartRepo);
        cartPosition.setCount(1);
        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.of(cartPosition));
        when(cartRepo.removeItemFromCartByItemId(itemId)).thenReturn(1);

        cartService.decreaseQuantityInCart(itemId);

        verify(cartRepo).removeItemFromCartByItemId(itemId);
        verify(cartRepo, never()).decreaseItemCount(itemId);
    }
}