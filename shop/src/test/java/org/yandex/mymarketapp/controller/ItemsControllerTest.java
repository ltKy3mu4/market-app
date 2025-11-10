package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.yandex.mymarketapp.model.dto.CartItemsDto;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.model.dto.ViewPage;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.ItemService;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

class ItemsControllerTest extends BaseControllerTest {

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @Test
    @WithMockUser
    void getItemsPage_WithDefaultParameters_ShouldReturnItemsView() {
        List<List<ItemDto>> mockItems = Arrays.asList(
                Arrays.asList(
                        new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 10.0, 0),
                        new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 20.0, 0),
                        new ItemDto(3L, "Item 3", "Desc 3", "/img3.jpg", 30.0, 0),
                        new ItemDto(4L, "Item 4", "Desc 4", "/img4.jpg", 40.0, 0)
                )
        );

        List<ItemDto> cartItems = Arrays.asList(
                        new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 10.0, 1),
                        new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 20.0, 2)
        );

        Paging mockPaging = new Paging(1, 10, true, false);

        when(itemService.searchItems("", "NO", 1, 10)).thenReturn(Mono.just(new ViewPage(mockItems)));
        when(itemService.getPageInfo(10, 1)).thenReturn(Mono.just(mockPaging));
        when(cartService.getCartItems(any())).thenReturn(Mono.just(new CartItemsDto(cartItems)));

        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk();

        verify(itemService).searchItems("", "NO", 1, 10);
        verify(itemService).getPageInfo(10, 1);
    }

    @Test
    @WithMockUser
    void getItemsPage_WithItemsPath_ShouldReturnItemsView() {
        List<List<ItemDto>> mockItems = Collections.emptyList();
        Paging mockPaging = new Paging(1, 10, false, false);

        when(itemService.searchItems("", "NO", 1, 10)).thenReturn(Mono.just(new ViewPage(mockItems)));
        when(itemService.getPageInfo(10, 1)).thenReturn(Mono.just(mockPaging));
        when(cartService.getCartItems(any())).thenReturn(Mono.just(new CartItemsDto(List.of())));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk();

        verify(itemService).searchItems("", "NO", 1, 10);
        verify(itemService).getPageInfo(10, 1);
    }

    @Test
    @WithMockUser
    void getItemsPage_WithSearchParameters_ShouldReturnFilteredItems() {
        List<List<ItemDto>> mockItems = Arrays.asList(
                Arrays.asList(new ItemDto(1L, "Test Item", "Desc", "/img.jpg", 15.0, 0))
        );
        Paging mockPaging = new Paging(2, 5, true, true);

        when(itemService.searchItems("test", "PRICE", 2, 5)).thenReturn(Mono.just(new ViewPage(mockItems)));
        when(itemService.getPageInfo(5, 2)).thenReturn(Mono.just(mockPaging));
        when(cartService.getCartItems(any())).thenReturn(Mono.just(new CartItemsDto(List.of())));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/")
                        .queryParam("search", "test")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "2")
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(itemService).searchItems("test", "PRICE", 2, 5);
        verify(itemService).getPageInfo(5, 2);
    }

    @Test
    @WithMockUser
    void getItemsPage_WithEmptySearch_ShouldReturnAllItems() {
        List<List<ItemDto>> mockItems = Arrays.asList(
                Arrays.asList(
                        new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 10.0, 0),
                        new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 20.0, 0),
                        new ItemDto(3L, "Item 3", "Desc 3", "/img3.jpg", 30.0, 0)
                )
        );
        Paging mockPaging = new Paging(1, 20, true, false);

        when(itemService.searchItems("", "ALPHA", 1, 20)).thenReturn(Mono.just(new ViewPage(mockItems)));
        when(itemService.getPageInfo(20, 1)).thenReturn(Mono.just(mockPaging));
        when(cartService.getCartItems(any())).thenReturn(Mono.just(new CartItemsDto(List.of())));

        // When & Then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/")
                        .queryParam("search", "")
                        .queryParam("sort", "ALPHA")
                        .queryParam("pageSize", "20")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().isOk();

        verify(itemService).searchItems("", "ALPHA", 1, 20);
        verify(itemService).getPageInfo(20, 1);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithPlusAction_ShouldIncreaseQuantityAndRedirect() {
        // Given
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId, USER_ID)).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "PLUS")
                        .queryParam("search", "test")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "2")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=test&sort=PRICE&pageSize=10&pageNumber=2");

        verify(cartService).increaseQuantityInCart(itemId, USER_ID);
        verifyNoInteractions(itemService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithMinusAction_ShouldDecreaseQuantityAndRedirect() {
        // Given
        Long itemId = 1L;
        when(cartService.decreaseQuantityInCart(itemId, USER_ID)).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "MINUS")
                        .queryParam("search", "item")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=item&sort=NO&pageSize=5&pageNumber=1");

        verify(cartService).decreaseQuantityInCart(itemId, USER_ID);
        verifyNoInteractions(itemService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithInvalidAction_ShouldRedirectWithoutServiceCall() {
        // Given
        Long itemId = 1L;

        // When & Then
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "INVALID_ACTION")
                        .queryParam("search", "test")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=test&sort=NO&pageSize=10&pageNumber=1");

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithMissingId_ShouldRedirectWithDefaultParams() {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("action", "PLUS")
                        .queryParam("search", "test")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=test&sort=NO&pageSize=10&pageNumber=1");

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithMissingAction_ShouldRedirectWithDefaultParams() {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", "1")
                        .queryParam("search", "test")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=test&sort=NO&pageSize=10&pageNumber=1");

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithDefaultPagination_ShouldUseDefaultsInRedirect() {
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId, USER_ID)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "PLUS")
                        .queryParam("search", "test")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=test&sort=NO&pageSize=10&pageNumber=1");

        verify(cartService).increaseQuantityInCart(itemId, USER_ID);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithSpecialCharactersInSearch_ShouldEncodeInRedirect() {
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId, USER_ID)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "PLUS")
                        .queryParam("search", "test%20item")
                        .queryParam("sort", "ALPHA")
                        .queryParam("pageSize", "15")
                        .queryParam("pageNumber", "3")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=test%20item&sort=ALPHA&pageSize=15&pageNumber=3");

        verify(cartService).increaseQuantityInCart(itemId, USER_ID);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithCaseSensitiveActions_ShouldWorkCorrectly() {
        Long itemId = 1L;

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "plus")
                        .queryParam("search", "test")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=test&sort=NO&pageSize=10&pageNumber=1");

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithNullAction_ShouldRedirectWithoutServiceCall() {
        Long itemId = 1L;

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", (String) null)
                        .queryParam("search", "test")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=test&sort=NO&pageSize=10&pageNumber=1");

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WithFormData_ShouldWorkCorrectly() {
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId, USER_ID)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/items")
                .bodyValue("id=1&action=PLUS&search=test&sort=PRICE&pageSize=10&pageNumber=2")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?search=test&sort=PRICE&pageSize=10&pageNumber=2");

        verify(cartService).increaseQuantityInCart(itemId, USER_ID);
        verifyNoInteractions(itemService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void handleItemAction_WhenServiceFails_ShouldPropagateError() {
        Long itemId = 1L;
        when(cartService.increaseQuantityInCart(itemId, USER_ID))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "PLUS")
                        .queryParam("search", "test")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is5xxServerError();

        verify(cartService).increaseQuantityInCart(itemId, USER_ID);
        verifyNoInteractions(itemService);
    }

    @Test
    @WithUserDetails(value = USER_USERNAME, userDetailsServiceBeanName = "inMemoryUserDetailsService")
    void getItemsPage_WhenServiceReturnsEmpty_ShouldHandleGracefully() {
        List<List<ItemDto>> mockItems = Collections.emptyList();
        Paging mockPaging = new Paging(1, 10, false, false);

        when(itemService.searchItems("", "NO", 1, 10)).thenReturn(Mono.just(new ViewPage(mockItems)));
        when(itemService.getPageInfo(10, 1)).thenReturn(Mono.just(mockPaging));
        when(cartService.getCartItems(any())).thenReturn(Mono.just(new CartItemsDto(List.of())));

        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk();

        verify(itemService).searchItems("", "NO", 1, 10);
        verify(itemService).getPageInfo(10, 1);
    }
}