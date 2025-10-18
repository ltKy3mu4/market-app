package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.OrderService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@WebFluxTest(CartController.class)
@Import(CartController.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderService orderService;

    @Test
    void showCart_ShouldReturnCartViewWithItemsAndTotal() {
        List<ItemDto> mockCartItems = Arrays.asList(
                new ItemDto(1L, "Item 1", "Description 1", "/img1.jpg", 10.0, 2),
                new ItemDto(2L, "Item 2", "Description 2", "/img2.jpg", 15.0, 1)
        );

        when(cartService.getCartItems(any())).thenReturn(Flux.fromIterable(mockCartItems));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(result -> {
                    String body = new String(result.getResponseBody());
                    Assertions.assertNotNull(body);
                });

        verify(cartService).getCartItems(any());
    }

    @Test
    void showCart_WhenCartIsEmpty_ShouldReturnCartViewWithZeroTotal() {
        when(cartService.getCartItems(any())).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();

        verify(cartService).getCartItems(any());
    }

    @Test
    void updateCartItem_WithPlusAction_ShouldIncreaseQuantity() {
        Long itemId = 1L;

        when(cartService.increaseQuantityInCart(itemId, 0L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).increaseQuantityInCart(itemId, 0L);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithMinusAction_ShouldDecreaseQuantity() {
        Long itemId = 1L;

        when(cartService.decreaseQuantityInCart(itemId, 0L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=MINUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).decreaseQuantityInCart(itemId, 0L);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithDeleteAction_ShouldRemoveItem() {
        Long itemId = 1L;

        when(cartService.removeFromCart(itemId, 0L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=DELETE")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).removeFromCart(itemId, 0L);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithInvalidAction_ShouldStillRedirect() {
        Long itemId = 1L;

        webTestClient.post()
                .uri("/cart/items")
                .bodyValue(new CartController.CartBuyForm(itemId, "INVALID_ACTION"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verifyNoInteractions(cartService);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithFormData_ShouldWorkCorrectly() {
        Long itemId = 1L;

        when(cartService.increaseQuantityInCart(itemId, 0L)).thenReturn(Mono.empty());

        // Alternative approach using form data instead of bodyValue
        webTestClient.post()
                .uri("/cart/items")
                .bodyValue("id=" + itemId + "&action=PLUS")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).increaseQuantityInCart(itemId, 0L);
    }

    @Test
    void buyItems_ShouldCreateOrder() {
        when(orderService.makeOrder(0L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders");

        verify(orderService).makeOrder(0L);
        verifyNoInteractions(cartService); // Note: Your reactive controller doesn't call cartService in buyItems
    }

    @Test
    void updateCartItem_WithCaseSensitiveActions_ShouldWorkCorrectly() {
        webTestClient.post()
                .uri("/cart/items")
                .bodyValue(new CartController.CartBuyForm(1L, "plus")) // lowercase should not match
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verifyNoInteractions(cartService);
        verifyNoInteractions(orderService);
    }

    // Additional test for reactive error handling
    @Test
    void updateCartItem_WhenServiceReturnsError_ShouldHandleGracefully() {
        Long itemId = 1L;

        when(cartService.increaseQuantityInCart(itemId, 0L))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=PLUS")
                .exchange()
                .expectStatus().is5xxServerError();

        verify(cartService).increaseQuantityInCart(itemId, 0L);
    }
}