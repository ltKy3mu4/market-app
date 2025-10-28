package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.ItemService;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@WebFluxTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @Test
    void showItem_ShouldReturnItemViewWithItemData() {
        Long itemId = 1L;
        Long userId = 0L;
        Item mockItem = new Item(itemId, "Test Item", "Test Description", "/test.jpg", 25.99);

        when(itemService.getItemById(itemId)).thenReturn(Mono.just(mockItem));
        when(cartService.getCountOfItemInCartByUserId(userId, itemId)).thenReturn(Mono.just(2));

        webTestClient.get()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().isOk()
                .expectBody();

        verify(itemService).getItemById(itemId);
        verify(cartService).getCountOfItemInCartByUserId(userId, itemId);
    }

    @Test
    void showItem_WhenItemNotFound_ShouldReturnNotFound() {
        Long itemId = 999L;
        Long userId = 0L;

        when(itemService.getItemById(itemId))
                .thenReturn(Mono.error(new ItemNotFoundException("Item not found with id: " + itemId)));
        when(cartService.getCountOfItemInCartByUserId(userId, itemId)).thenReturn(Mono.just(0));


        webTestClient.get()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().isNotFound();

        verify(itemService).getItemById(itemId);
    }

    @Test
    void updateItemQuantity_WithPlusAction_ShouldIncreaseQuantityAndRedirect() {
        // Given
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId, 0L)).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", "PLUS")
                        .build(itemId))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService).increaseQuantityInCart(itemId, 0L);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithMinusAction_ShouldDecreaseQuantityAndRedirect() {
        // Given
        Long itemId = 1L;
        when(cartService.decreaseQuantityInCart(itemId, 0L)).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", "MINUS")
                        .build(itemId))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService).decreaseQuantityInCart(itemId, 0L);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithInvalidAction_ShouldRedirectWithoutServiceCall() {
        // Given
        Long itemId = 1L;

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", "INVALID_ACTION")
                        .build(itemId))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithMissingAction_ShouldRedirect() {
        Long itemId = 1L;

        webTestClient.post()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithCaseSensitiveActions_ShouldWorkCorrectly() {
        Long itemId = 1L;

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", "plus")
                        .build(itemId))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithEmptyAction_ShouldRedirect() {
        // Given
        Long itemId = 1L;

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", "")
                        .build(itemId))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithPlusActionWhenServiceFails_ShouldPropagateError() {
        // Given
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId, 0L))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", "PLUS")
                        .build(itemId))
                .exchange()
                .expectStatus().is5xxServerError();

        verify(cartService).increaseQuantityInCart(itemId, 0L);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithFormData_ShouldWorkCorrectly() {
        // Given
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId, 0L)).thenReturn(Mono.empty());

        // When & Then - Using form data instead of query param
        webTestClient.post()
                .uri("/items/{id}", itemId)
                .bodyValue("action=PLUS")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService).increaseQuantityInCart(itemId, 0L);
        verifyNoInteractions(itemService);
    }

    @Test
    void showItem_WithServiceReturningEmpty_ShouldReturnNotFound() {
        Long itemId = 999L;
        when(itemService.getItemById(itemId)).thenThrow(ItemNotFoundException.class);

        webTestClient.get()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus()
                        .isNotFound();

        verify(itemService).getItemById(itemId);
        verifyNoInteractions(cartService);
    }
}