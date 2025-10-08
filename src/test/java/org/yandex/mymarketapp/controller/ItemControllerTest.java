package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.ItemService;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @Test
    void showItem_ShouldReturnItemViewWithItemData() throws Exception {
        Long itemId = 1L;
        ItemDto mockItem = new ItemDto(itemId, "Test Item", "Test Description", "/test.jpg", 25.99, 2);

        when(itemService.getItemById(itemId)).thenReturn(mockItem);

        mockMvc.perform(get("/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", mockItem));

        verify(itemService).getItemById(itemId);
        verifyNoInteractions(cartService);
    }

    @Test
    void showItem_WhenItemNotFound_ShouldThrowException() throws Exception {
        Long itemId = 999L;
        when(itemService.getItemById(itemId))
                .thenThrow(new ItemNotFoundException("Item not found with id: " + itemId));

        mockMvc.perform(get("/items/{id}", itemId))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ItemNotFoundException));

        verify(itemService).getItemById(itemId);
        verifyNoInteractions(cartService);
    }

    @Test
    void updateItemQuantity_WithPlusAction_ShouldIncreaseQuantityAndRedirect() throws Exception {
        
        Long itemId = 1L;

        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemId));

        verify(cartService).increaseQuantityInCart(itemId);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithMinusAction_ShouldDecreaseQuantityAndRedirect() throws Exception {
        
        Long itemId = 1L;

        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", "MINUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemId));

        verify(cartService).decreaseQuantityInCart(itemId);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithInvalidAction_ShouldRedirectWithoutServiceCall() throws Exception {
        
        Long itemId = 1L;

        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", "INVALID_ACTION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemId));

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithMissingAction_ShouldReturnBadRequest() throws Exception {
        
        Long itemId = 1L;

        mockMvc.perform(post("/items/{id}", itemId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithCaseSensitiveActions_ShouldWorkCorrectly() throws Exception {
        
        Long itemId = 1L;

        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", "plus")) // lowercase should not match
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemId));

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void updateItemQuantity_WithEmptyAction_ShouldReturnBadRequest() throws Exception {
        
        Long itemId = 1L;

        mockMvc.perform(post("/items/{id}", itemId)
                        .param("action", ""))
                .andExpect(status().is3xxRedirection());

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }


    @Test
    void showItem_WithMultipleItems_ShouldReturnCorrectItem() throws Exception {
        
        Long itemId1 = 1L;
        Long itemId2 = 2L;

        ItemDto mockItem1 = new ItemDto(itemId1, "Item 1", "Desc 1", "/img1.jpg", 10.0, 1);
        ItemDto mockItem2 = new ItemDto(itemId2, "Item 2", "Desc 2", "/img2.jpg", 20.0, 2);

        when(itemService.getItemById(itemId1)).thenReturn(mockItem1);
        when(itemService.getItemById(itemId2)).thenReturn(mockItem2);
        
        mockMvc.perform(get("/items/{id}", itemId1))
                .andExpect(status().isOk())
                .andExpect(model().attribute("item", mockItem1));

        mockMvc.perform(get("/items/{id}", itemId2))
                .andExpect(status().isOk())
                .andExpect(model().attribute("item", mockItem2));

        verify(itemService).getItemById(itemId1);
        verify(itemService).getItemById(itemId2);
        verifyNoInteractions(cartService);
    }
}