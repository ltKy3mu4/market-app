package org.yandex.mymarketapp.service;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.model.mapper.ItemMapperImpl;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {ItemService.class, ItemMapperImpl.class})
class ItemServiceTest {

    @MockitoBean
    private ItemRepository itemRepo;

    @MockitoBean
    private CartPositionsRepository cartRepo;

    @Autowired
    private ItemService itemService;

    @Test
    void getItemById_WhenItemExists_ShouldReturnItemDtoWithCartCount() {

        Long itemId = 1L;
        Item mockItem = new Item();
        mockItem.setId(itemId);
        mockItem.setTitle("Test Item");
        mockItem.setPrice(25.99);
        mockItem.setDescription("Description");
        mockItem.setImgPath("/img.jpg");

        CartPosition mockCartPosition = new CartPosition();
        mockCartPosition.setCount(3);

        ItemDto expectedDto = new ItemDto(itemId, "Test Item", "Description", "/img.jpg", 25.99, 3);

        when(itemRepo.getItemById(itemId)).thenReturn(Optional.of(mockItem));
        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.of(mockCartPosition));

        
        ItemDto result = itemService.getItemById(itemId);

        
        assertNotNull(result);
        assertEquals(expectedDto, result);
        verify(itemRepo).getItemById(itemId);
        verify(cartRepo).findByItemId(itemId);
    }

    @Test
    void getItemById_WhenItemNotInCart_ShouldReturnItemDtoWithZeroCount() {

        Long itemId = 1L;
        Item mockItem = new Item();
        mockItem.setId(itemId);

        when(itemRepo.getItemById(itemId)).thenReturn(Optional.of(mockItem));
        when(cartRepo.findByItemId(itemId)).thenReturn(Optional.empty());
        
        ItemDto result = itemService.getItemById(itemId);
        
        assertNotNull(result);
        assertEquals(0, result.count());
        verify(itemRepo).getItemById(itemId);
        verify(cartRepo).findByItemId(itemId);
    }

    @Test
    void getItemById_WhenItemNotFound_ShouldThrowException() {

        Long itemId = 999L;
        when(itemRepo.getItemById(itemId)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () ->
                itemService.getItemById(itemId)
        );

        verify(itemRepo).getItemById(itemId);
        verify(cartRepo, never()).findByItemId(any());
    }

    @Test
    void searchItems_WithMultipleItems_ShouldReturnGroupedRows() {

        String searchTerm = "test";
        String sortBy = "NO";
        int pageNumber = 1;
        int pageSize = 9;

        List<Item> mockItems = Arrays.asList(
                createItem(1L, "Test Item 1"),
                createItem(2L, "Test Item 2"),
                createItem(3L, "Test Item 3"),
                createItem(4L, "Test Item 4"),
                createItem(5L, "Test Item 5"),
                createItem(6L, "Test Item 6"),
                createItem(7L, "Test Item 7")
        );

        Page<Item> mockPage = new PageImpl<>(mockItems);

        when(itemRepo.findItems(searchTerm, sortBy, PageRequest.of(0, pageSize))).thenReturn(mockPage);

        when(cartRepo.findByItemId(1L)).thenReturn(Optional.of(createCartPosition(2)));
        when(cartRepo.findByItemId(2L)).thenReturn(Optional.of(createCartPosition(1)));
        when(cartRepo.findByItemId(3L)).thenReturn(Optional.of(createCartPosition(0)));
        when(cartRepo.findByItemId(4L)).thenReturn(Optional.empty());
        when(cartRepo.findByItemId(5L)).thenReturn(Optional.of(createCartPosition(5)));
        when(cartRepo.findByItemId(6L)).thenReturn(Optional.of(createCartPosition(1)));
        when(cartRepo.findByItemId(7L)).thenReturn(Optional.of(createCartPosition(3)));


        
        List<List<ItemDto>> result = itemService.searchItems(searchTerm, sortBy, pageNumber, pageSize);

        
        assertNotNull(result);
        assertEquals(3, result.size()); // 7 items should be grouped into 3 rows

        // First row should have 3 items
        assertEquals(3, result.get(0).size());
        // Second row should have 3 items
        assertEquals(3, result.get(1).size());
        // Third row should have 1 item
        assertEquals(1, result.get(2).size());

        verify(itemRepo).findItems(searchTerm, sortBy, PageRequest.of(0, pageSize));
        verify(cartRepo, times(7)).findByItemId(anyLong());
    }

    @Test
    void searchItems_WithEmptyResult_ShouldReturnEmptyList() {

        String searchTerm = "nonexistent";
        String sortBy = "PRICE";
        int pageNumber = 1;
        int pageSize = 10;

        Page<Item> mockPage = new PageImpl<>(Collections.emptyList());
        when(itemRepo.findItems(searchTerm, sortBy, PageRequest.of(0, pageSize))).thenReturn(mockPage);

        
        List<List<ItemDto>> result = itemService.searchItems(searchTerm, sortBy, pageNumber, pageSize);

        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(itemRepo).findItems(searchTerm, sortBy, PageRequest.of(0, pageSize));
        verify(cartRepo, never()).findByItemId(anyLong());
    }

    @Test
    void searchItems_WithExactMultipleOfThree_ShouldReturnFullRows() {

        List<Item> mockItems = Arrays.asList(
                createItem(1L, "Item 1"),
                createItem(2L, "Item 2"),
                createItem(3L, "Item 3"),
                createItem(4L, "Item 4"),
                createItem(5L, "Item 5"),
                createItem(6L, "Item 6")
        );

        Page<Item> mockPage = new PageImpl<>(mockItems);
        when(itemRepo.findItems(null, "ALPHA", PageRequest.of(0, 10))).thenReturn(mockPage);
        when(cartRepo.findByItemId(anyLong())).thenReturn(Optional.of(createCartPosition(1)));

        
        List<List<ItemDto>> result = itemService.searchItems(null, "ALPHA", 1, 10);

        
        assertNotNull(result);
        assertEquals(2, result.size()); // 6 items = 2 full rows of 3
        assertEquals(3, result.get(0).size());
        assertEquals(3, result.get(1).size());
    }

    @Test
    void searchItems_WithPageNumberGreaterThanOne_ShouldCalculateCorrectOffset() {

        int pageNumber = 2;
        int pageSize = 5;

        Page<Item> mockPage = new PageImpl<>(Collections.emptyList());
        when(itemRepo.findItems("test", "NO", PageRequest.of(1, pageSize))).thenReturn(mockPage);

        
        itemService.searchItems("test", "NO", pageNumber, pageSize);

        
        verify(itemRepo).findItems("test", "NO", PageRequest.of(1, pageSize));
    }

    @Test
    void getPageInfo_WithExactDivision_ShouldCalculateCorrectly() {

        int totalItems = 20;
        int pageSize = 5;
        int pageNumber = 2;

        when(itemRepo.getTotalItemsCount()).thenReturn(totalItems);

        
        Paging result = itemService.getPageInfo(pageSize, pageNumber);

        
        assertNotNull(result);
        assertEquals(pageNumber, result.pageNumber());
        assertEquals(pageSize, result.pageSize());
        assertTrue(result.hasNext()); // 20/5=4 pages, page 2 has next
        assertTrue(result.hasPrevious()); // page 2 has previous
        verify(itemRepo).getTotalItemsCount();
    }

    @Test
    void getPageInfo_WithRemainder_ShouldCalculateCorrectly() {

        int totalItems = 22;
        int pageSize = 5;
        int pageNumber = 5;

        when(itemRepo.getTotalItemsCount()).thenReturn(totalItems);

        
        Paging result = itemService.getPageInfo(pageSize, pageNumber);

        
        assertNotNull(result);
        assertEquals(5, result.pageNumber());
        assertEquals(5, result.pageSize());
        assertFalse(result.hasNext()); // 22/5=4.4 -> 5 pages, last page has no next
        assertTrue(result.hasPrevious()); // has previous
    }

    @Test
    void getPageInfo_WithFirstPage_ShouldHaveNoPrevious() {

        int totalItems = 10;
        int pageSize = 5;
        int pageNumber = 1;

        when(itemRepo.getTotalItemsCount()).thenReturn(totalItems);

        
        Paging result = itemService.getPageInfo(pageSize, pageNumber);

        
        assertNotNull(result);
        assertEquals(1, result.pageNumber());
        assertTrue(result.hasNext()); // has next page
        assertFalse(result.hasPrevious()); // first page, no previous
    }

    @Test
    void getPageInfo_WithLastPage_ShouldHaveNoNext() {

        int totalItems = 10;
        int pageSize = 5;
        int pageNumber = 2;

        when(itemRepo.getTotalItemsCount()).thenReturn(totalItems);

        
        Paging result = itemService.getPageInfo(pageSize, pageNumber);

        
        assertNotNull(result);
        assertEquals(2, result.pageNumber());
        assertFalse(result.hasNext()); // last page, no next
        assertTrue(result.hasPrevious()); // has previous
    }

    @Test
    void getPageInfo_WithNoItems_ShouldReturnFirstPageWithoutNextOrPrevious() {

        int totalItems = 0;
        int pageSize = 10;
        int pageNumber = 1;

        when(itemRepo.getTotalItemsCount()).thenReturn(totalItems);

        
        Paging result = itemService.getPageInfo(pageSize, pageNumber);

        
        assertNotNull(result);
        assertEquals(1, result.pageNumber());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());
    }

    @Test
    void getPageInfo_WithSingleItem_ShouldCalculateSinglePage() {

        int totalItems = 1;
        int pageSize = 10;
        int pageNumber = 1;

        when(itemRepo.getTotalItemsCount()).thenReturn(totalItems);

        
        Paging result = itemService.getPageInfo(pageSize, pageNumber);

        
        assertNotNull(result);
        assertEquals(1, result.pageNumber());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());
    }

    @Test
    void searchItems_WithNullSearchTerm_ShouldPassNullToRepository() {

        Page<Item> mockPage = new PageImpl<>(Collections.emptyList());
        when(itemRepo.findItems(null, "NO", PageRequest.of(0, 10))).thenReturn(mockPage);

        
        itemService.searchItems(null, "NO", 1, 10);

        
        verify(itemRepo).findItems(null, "NO", PageRequest.of(0, 10));
    }

    @Test
    void searchItems_WithEmptySearchTerm_ShouldPassEmptyString() {

        Page<Item> mockPage = new PageImpl<>(Collections.emptyList());
        when(itemRepo.findItems("", "ALPHA", PageRequest.of(0, 5))).thenReturn(mockPage);

        
        itemService.searchItems("", "ALPHA", 1, 5);

        
        verify(itemRepo).findItems("", "ALPHA", PageRequest.of(0, 5));
    }

    private Item createItem(Long id, String title) {
        Item item = new Item();
        item.setId(id);
        item.setTitle(title);
        item.setPrice(10.0);
        return item;
    }

    private CartPosition createCartPosition(int count) {
        CartPosition cp = new CartPosition();
        cp.setCount(count);
        return cp;
    }
}