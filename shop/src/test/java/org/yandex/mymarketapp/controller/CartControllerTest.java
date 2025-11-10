package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.yandex.mymarketapp.model.domain.UserRole;
import org.yandex.mymarketapp.model.dto.CartItemsDto;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.OrderService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

class CartControllerTest extends BaseControllerTest{

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderService orderService;

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showCart_ShouldReturnCartViewWithItemsAndTotal() {
        List<ItemDto> mockCartItems = Arrays.asList(
                new ItemDto(1L, "Item 1", "Description 1", "/img1.jpg", 10.0, 2),
                new ItemDto(2L, "Item 2", "Description 2", "/img2.jpg", 15.0, 1)
        );

        when(cartService.getCartItems(any())).thenReturn(Mono.just(new CartItemsDto(mockCartItems)));
        when(cartService.isMoneyEnoughToBuy(any(), any())).thenReturn(Mono.just(true));


        webTestClient
                .get()
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
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void showCart_WhenCartIsEmpty_ShouldReturnCartViewWithZeroTotal() {
        when(cartService.getCartItems(any())).thenReturn(Mono.just(new CartItemsDto(List.of())));
        when(cartService.isMoneyEnoughToBuy(any(), any())).thenReturn(Mono.just(true));


        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();

        verify(cartService).getCartItems(any());
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void updateCartItem_WithPlusAction_ShouldIncreaseQuantity() {
        Long itemId = 1L;

        when(cartService.increaseQuantityInCart(itemId, USER_ID)).thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=PLUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).increaseQuantityInCart(itemId, USER_ID);
        verifyNoInteractions(orderService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void updateCartItem_WithMinusAction_ShouldDecreaseQuantity() {
        Long itemId = 1L;

        when(cartService.decreaseQuantityInCart(itemId, USER_ID)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=MINUS")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).decreaseQuantityInCart(itemId, USER_ID);
        verifyNoInteractions(orderService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void updateCartItem_WithDeleteAction_ShouldRemoveItem() {
        Long itemId = 1L;

        when(cartService.removeFromCart(itemId, USER_ID)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=DELETE")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).removeFromCart(itemId, USER_ID);
        verifyNoInteractions(orderService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
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
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void updateCartItem_WithFormData_ShouldWorkCorrectly() {
        Long itemId = 1L;

        when(cartService.increaseQuantityInCart(itemId, USER_ID)).thenReturn(Mono.empty());

        // Alternative approach using form data instead of bodyValue
        webTestClient.post()
                .uri("/cart/items")
                .bodyValue("id=" + itemId + "&action=PLUS")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).increaseQuantityInCart(itemId, USER_ID);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void buyItems_ShouldCreateOrder() {
        when(orderService.makeOrder(USER_ID)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/cart/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders");

        verify(orderService).makeOrder(USER_ID);
        verifyNoInteractions(cartService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
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

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void updateCartItem_WhenServiceReturnsError_ShouldHandleGracefully() {
        Long itemId = 1L;

        when(cartService.increaseQuantityInCart(itemId, USER_ID))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("id=" + itemId + "&action=PLUS")
                .exchange()
                .expectStatus().is5xxServerError();

        verify(cartService).increaseQuantityInCart(itemId, USER_ID);
    }
}