package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.OrderService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;


@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderService orderService;

    @Test
    void showCart_ShouldReturnCartViewWithItemsAndTotal() throws Exception {
        
        List<ItemDto> mockCartItems = Arrays.asList(
                new ItemDto(1L, "Item 1", "Description 1", "/img1.jpg", 10.0, 2),
                new ItemDto(2L, "Item 2", "Description 2", "/img2.jpg", 15.0, 1)
        );

        when(cartService.getCartItems()).thenReturn(mockCartItems);

        
        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", mockCartItems))
                .andExpect(model().attribute("total", 35.0)); 

        verify(cartService).getCartItems();
    }

    @Test
    void showCart_WhenCartIsEmpty_ShouldReturnCartViewWithZeroTotal() throws Exception {
        when(cartService.getCartItems()).thenReturn(Collections.emptyList());

        
        mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", Collections.emptyList()))
                .andExpect(model().attribute("total", 0.0));

        verify(cartService).getCartItems();
    }

    @Test
    void updateCartItem_WithPlusAction_ShouldIncreaseQuantity() throws Exception {
        Long itemId = 1L;

        mockMvc.perform(post("/cart/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService).increaseQuantityInCart(itemId);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithMinusAction_ShouldDecreaseQuantity() throws Exception {
        
        Long itemId = 1L;

        
        mockMvc.perform(post("/cart/items")
                        .param("id", itemId.toString())
                        .param("action", "MINUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService).decreaseQuantityInCart(itemId);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithDeleteAction_ShouldRemoveItem() throws Exception {
        
        Long itemId = 1L;

        
        mockMvc.perform(post("/cart/items")
                        .param("id", itemId.toString())
                        .param("action", "DELETE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService).removeFromCart(itemId);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithInvalidAction_ShouldStillRedirect() throws Exception {
        Long itemId = 1L;
        
        mockMvc.perform(post("/cart/items")
                        .param("id", itemId.toString())
                        .param("action", "INVALID_ACTION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verifyNoInteractions(cartService);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithMissingId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .param("action", "PLUS"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
        verifyNoInteractions(orderService);
    }

    @Test
    void updateCartItem_WithMissingAction_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .param("id", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
        verifyNoInteractions(orderService);
    }

    @Test
    void buyItems_ShouldClearCartAndCreateOrder() throws Exception {
        List<ItemDto> mockCartItems = Arrays.asList(
                new ItemDto(1L, "Item 1", "Description 1", "/img1.jpg", 10.0, 2),
                new ItemDto(2L, "Item 2", "Description 2", "/img2.jpg", 15.0, 1)
        );

        when(cartService.getCartItems()).thenReturn(mockCartItems);

        
        mockMvc.perform(post("/cart/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(cartService).getCartItems();
        verify(cartService).clearCart();
        verify(orderService).makeOrder(mockCartItems);
    }

    @Test
    void buyItems_WhenCartIsEmpty_ShouldStillRedirectToOrders() throws Exception {
        
        when(cartService.getCartItems()).thenReturn(Collections.emptyList());

        
        mockMvc.perform(post("/cart/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(cartService).getCartItems();
        verify(cartService).clearCart();
        verify(orderService).makeOrder(Collections.emptyList());
    }

    @Test
    void updateCartItem_WithCaseSensitiveActions_ShouldWorkCorrectly() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .param("id", "1")
                        .param("action", "plus")) // lowercase should not match
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verifyNoInteractions(cartService);
        verifyNoInteractions(orderService);
    }
}