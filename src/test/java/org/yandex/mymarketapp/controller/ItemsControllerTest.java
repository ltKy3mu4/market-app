package org.yandex.mymarketapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.ItemService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(ItemsController.class)
class ItemsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @Test
    void getItemsPage_WithDefaultParameters_ShouldReturnItemsView() throws Exception {
        List<List<ItemDto>> mockItems = Arrays.asList(
                Arrays.asList(
                        new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 10.0, 1),
                        new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 20.0, 2)
                )
        );
        Paging mockPaging = new Paging(1, 10, true, false);

        when(itemService.searchItems("", "NO", 1, 10)).thenReturn(mockItems);
        when(itemService.getPageInfo(10, 1)).thenReturn(mockPaging);

        
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("search", "sort", "paging", "items"))
                .andExpect(model().attribute("search", ""))
                .andExpect(model().attribute("sort", "NO"))
                .andExpect(model().attribute("paging", mockPaging))
                .andExpect(model().attribute("items", mockItems));

        verify(itemService).searchItems("", "NO", 1, 10);
        verify(itemService).getPageInfo(10, 1);
        verifyNoInteractions(cartService);
    }

    @Test
    void getItemsPage_WithItemsPath_ShouldReturnItemsView() throws Exception {
        
        List<List<ItemDto>> mockItems = Collections.emptyList();
        Paging mockPaging = new Paging(1, 10, false, false);

        when(itemService.searchItems("", "NO", 1, 10)).thenReturn(mockItems);
        when(itemService.getPageInfo(10, 1)).thenReturn(mockPaging);

        
        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attribute("items", mockItems));

        verify(itemService).searchItems("", "NO", 1, 10);
        verify(itemService).getPageInfo(10, 1);
    }

    @Test
    void getItemsPage_WithSearchParameters_ShouldReturnFilteredItems() throws Exception {
        
        List<List<ItemDto>> mockItems = Arrays.asList(
                Arrays.asList(new ItemDto(1L, "Test Item", "Desc", "/img.jpg", 15.0, 1))
        );
        Paging mockPaging = new Paging(2, 5, true, true);

        when(itemService.searchItems("test", "PRICE", 2, 5)).thenReturn(mockItems);
        when(itemService.getPageInfo(5, 2)).thenReturn(mockPaging);

        
        mockMvc.perform(get("/")
                        .param("search", "test")
                        .param("sort", "PRICE")
                        .param("pageSize", "5")
                        .param("pageNumber", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "test"))
                .andExpect(model().attribute("sort", "PRICE"))
                .andExpect(model().attribute("paging", mockPaging))
                .andExpect(model().attribute("items", mockItems));

        verify(itemService).searchItems("test", "PRICE", 2, 5);
        verify(itemService).getPageInfo(5, 2);
    }

    @Test
    void getItemsPage_WithEmptySearch_ShouldReturnAllItems() throws Exception {
        
        List<List<ItemDto>> mockItems = Arrays.asList(
                Arrays.asList(
                        new ItemDto(1L, "Item 1", "Desc 1", "/img1.jpg", 10.0, 1),
                        new ItemDto(2L, "Item 2", "Desc 2", "/img2.jpg", 20.0, 2),
                        new ItemDto(3L, "Item 3", "Desc 3", "/img3.jpg", 30.0, 3)
                )
        );
        Paging mockPaging = new Paging(1, 10, true, false);

        when(itemService.searchItems("", "ALPHA", 1, 20)).thenReturn(mockItems);
        when(itemService.getPageInfo(20, 1)).thenReturn(mockPaging);

        
        mockMvc.perform(get("/")
                        .param("search", "")
                        .param("sort", "ALPHA")
                        .param("pageSize", "20")
                        .param("pageNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", ""))
                .andExpect(model().attribute("sort", "ALPHA"))
                .andExpect(model().attribute("items", mockItems));

        verify(itemService).searchItems("", "ALPHA", 1, 20);
        verify(itemService).getPageInfo(20, 1);
    }

    @Test
    void handleItemAction_WithPlusAction_ShouldIncreaseQuantityAndRedirect() throws Exception {
        
        Long itemId = 1L;
        String search = "test";
        String sort = "PRICE";
        int pageSize = 10;
        int pageNumber = 2;

        
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .param("search", search)
                        .param("sort", sort)
                        .param("pageSize", String.valueOf(pageSize))
                        .param("pageNumber", String.valueOf(pageNumber)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?search=test&sort=PRICE&pageSize=10&pageNumber=2"));

        verify(cartService).increaseQuantityInCart(itemId);
        verifyNoInteractions(itemService);
    }

    @Test
    void handleItemAction_WithMinusAction_ShouldDecreaseQuantityAndRedirect() throws Exception {
        
        Long itemId = 1L;
        String search = "item";
        String sort = "NO";
        int pageSize = 5;
        int pageNumber = 1;

        
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "MINUS")
                        .param("search", search)
                        .param("sort", sort)
                        .param("pageSize", String.valueOf(pageSize))
                        .param("pageNumber", String.valueOf(pageNumber)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?search=item&sort=NO&pageSize=5&pageNumber=1"));

        verify(cartService).decreaseQuantityInCart(itemId);
        verifyNoInteractions(itemService);
    }

    @Test
    void handleItemAction_WithInvalidAction_ShouldRedirectWithoutServiceCall() throws Exception {
        
        Long itemId = 1L;

        
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "INVALID_ACTION")
                        .param("search", "test")
                        .param("sort", "NO")
                        .param("pageSize", "10")
                        .param("pageNumber", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?search=test&sort=NO&pageSize=10&pageNumber=1"));

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void handleItemAction_WithMissingId_ShouldReturnBadRequest() throws Exception {
        
        mockMvc.perform(post("/items")
                        .param("action", "PLUS")
                        .param("search", "test")
                        .param("sort", "NO")
                        .param("pageSize", "10")
                        .param("pageNumber", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void handleItemAction_WithMissingAction_ShouldReturnBadRequest() throws Exception {
        
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("search", "test")
                        .param("sort", "NO")
                        .param("pageSize", "10")
                        .param("pageNumber", "1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }

    @Test
    void handleItemAction_WithDefaultPagination_ShouldUseDefaultsInRedirect() throws Exception {
        
        Long itemId = 1L;

        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .param("search", "test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?search=test&sort=NO&pageSize=10&pageNumber=1"));

        verify(cartService).increaseQuantityInCart(itemId);
    }

    @Test
    void handleItemAction_WithSpecialCharactersInSearch_ShouldEncodeInRedirect() throws Exception {
        
        Long itemId = 1L;
        String searchWithSpaces = "test item";
        String searchWithSpecialChars = "test&item";

        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .param("search", searchWithSpaces)
                        .param("sort", "ALPHA")
                        .param("pageSize", "15")
                        .param("pageNumber", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?search=test item&sort=ALPHA&pageSize=15&pageNumber=3"));

        verify(cartService).increaseQuantityInCart(itemId);
    }

    @Test
    void handleItemAction_WithCaseSensitiveActions_ShouldWorkCorrectly() throws Exception {
        
        Long itemId = 1L;

        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "plus") // lowercase should not match
                        .param("search", "test")
                        .param("sort", "NO")
                        .param("pageSize", "10")
                        .param("pageNumber", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?search=test&sort=NO&pageSize=10&pageNumber=1"));

        // Verify no service calls for non-matching action
        verifyNoInteractions(cartService);
        verifyNoInteractions(itemService);
    }
}