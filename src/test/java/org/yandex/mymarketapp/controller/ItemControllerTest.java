package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
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
        // Given
        Long itemId = 1L;
        ItemDto mockItem = new ItemDto(itemId, "Test Item", "Test Description", "/test.jpg", 25.99, 2);

        when(itemService.getItemById(itemId)).thenReturn(Mono.just(mockItem));

        // When & Then
        webTestClient.get()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().isOk()
                .expectBody();

        verify(itemService).getItemById(itemId);
        verifyNoInteractions(cartService);
    }

    @Test
    void showItem_WhenItemNotFound_ShouldReturnNotFound() {
        // Given
        Long itemId = 999L;
        when(itemService.getItemById(itemId))
                .thenReturn(Mono.error(new ItemNotFoundException("Item not found with id: " + itemId)));

        // When & Then
        webTestClient.get()
                .uri("/items/{id}", itemId)
                .exchange()
                .expectStatus().isNotFound();

        verify(itemService).getItemById(itemId);
        verifyNoInteractions(cartService);
    }

    @Test
    void updateItemQuantity_WithPlusAction_ShouldIncreaseQuantityAndRedirect() {
        // Given
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId)).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", "PLUS")
                        .build(itemId))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService).increaseQuantityInCart(itemId);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithMinusAction_ShouldDecreaseQuantityAndRedirect() {
        // Given
        Long itemId = 1L;
        when(cartService.decreaseQuantityInCart(itemId)).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", "MINUS")
                        .build(itemId))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService).decreaseQuantityInCart(itemId);
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
        when(cartService.increaseQuantityInCart(itemId))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/{id}")
                        .queryParam("action", "PLUS")
                        .build(itemId))
                .exchange()
                .expectStatus().is5xxServerError();

        verify(cartService).increaseQuantityInCart(itemId);
        verifyNoInteractions(itemService);
    }

    @Test
    void showItem_WithMultipleItems_ShouldReturnCorrectItem() {
        // Given
        Long itemId1 = 1L;
        Long itemId2 = 2L;

        ItemDto mockItem1 = new ItemDto(itemId1, "Item 1", "Desc 1", "/img1.jpg", 10.0, 1);
        ItemDto mockItem2 = new ItemDto(itemId2, "Item 2", "Desc 2", "/img2.jpg", 20.0, 2);

        when(itemService.getItemById(itemId1)).thenReturn(Mono.just(mockItem1));
        when(itemService.getItemById(itemId2)).thenReturn(Mono.just(mockItem2));

        // When & Then - First item
        webTestClient.get()
                .uri("/items/{id}", itemId1)
                .exchange()
                .expectStatus().isOk();

        // When & Then - Second item
        webTestClient.get()
                .uri("/items/{id}", itemId2)
                .exchange()
                .expectStatus().isOk();

        verify(itemService).getItemById(itemId1);
        verify(itemService).getItemById(itemId2);
        verifyNoInteractions(cartService);
    }

    @Test
    void updateItemQuantity_WithFormData_ShouldWorkCorrectly() {
        // Given
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId)).thenReturn(Mono.empty());

        // When & Then - Using form data instead of query param
        webTestClient.post()
                .uri("/items/{id}", itemId)
                .bodyValue("action=PLUS")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/" + itemId);

        verify(cartService).increaseQuantityInCart(itemId);
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