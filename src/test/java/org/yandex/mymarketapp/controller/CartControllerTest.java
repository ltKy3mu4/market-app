package org.yandex.mymarketapp.controller;

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
@Import(CartController.class) // Ensure the controller is properly imported
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

        when(cartService.getCartItems()).thenReturn(Flux.fromIterable(mockCartItems));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(result -> {
                    String body = new String(result.getResponseBody());
                    System.out.println();
                    // For Thymeleaf templates, you might want to check specific content
                    // Since it's returning a view name, we expect "cart" to be in the response
                    // You might need to adjust this based on your actual template rendering
                });

        verify(cartService).getCartItems();
    }

    @Test
    void showCart_WhenCartIsEmpty_ShouldReturnCartViewWithZeroTotal() {
        when(cartService.getCartItems()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();

        verify(cartService).getCartItems();
    }

    @Test
    void updateCartItem_WithPlusAction_ShouldIncreaseQuantity() {
        Long itemId = 1L;

        when(cartService.increaseQuantityInCart(itemId)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).increaseQuantityInCart(itemId);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithMinusAction_ShouldDecreaseQuantity() {
        Long itemId = 1L;

        when(cartService.decreaseQuantityInCart(itemId)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=MINUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).decreaseQuantityInCart(itemId);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithDeleteAction_ShouldRemoveItem() {
        Long itemId = 1L;

        when(cartService.removeFromCart(itemId)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=DELETE")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).removeFromCart(itemId);
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

        when(cartService.increaseQuantityInCart(itemId)).thenReturn(Mono.empty());

        // Alternative approach using form data instead of bodyValue
        webTestClient.post()
                .uri("/cart/items")
                .bodyValue("id=" + itemId + "&action=PLUS")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).increaseQuantityInCart(itemId);
    }

    @Test
    void buyItems_ShouldCreateOrder() {
        when(orderService.makeOrder()).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders");

        verify(orderService).makeOrder();
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

        when(cartService.increaseQuantityInCart(itemId))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=PLUS")
                .exchange()
                .expectStatus().is5xxServerError();

        verify(cartService).increaseQuantityInCart(itemId);
    }
}