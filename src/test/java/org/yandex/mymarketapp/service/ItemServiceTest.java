package org.yandex.mymarketapp.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.mapper.ItemMapperImpl;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        // Given
        Long itemId = 1L;
        ItemDto expectedDto = new ItemDto(itemId, "Test Item", "Description", "/img.jpg", 25.99, 3);

        when(itemRepo.findItemByIdWithCount(itemId)).thenReturn(Mono.just(expectedDto));

        // When
        Mono<ItemDto> result = itemService.getItemById(itemId);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedDto)
                .verifyComplete();

        verify(itemRepo).findItemByIdWithCount(itemId);
    }

    @Test
    void getItemById_WhenItemNotFound_ShouldThrowException() {
        // Given
        Long itemId = 999L;
        when(itemRepo.findItemByIdWithCount(itemId)).thenReturn(Mono.empty());

        // When
        Mono<ItemDto> result = itemService.getItemById(itemId);

        // Then
        StepVerifier.create(result)
                .verifyError(ItemNotFoundException.class);

        verify(itemRepo).findItemByIdWithCount(itemId);
    }

    @Test
    void searchItems_WithMultipleItems_ShouldReturnGroupedRows() {
        // Given
        String searchTerm = "test";
        String sortBy = "NO";
        int pageNumber = 1;
        int pageSize = 9;

        List<ItemDto> mockItems = Arrays.asList(
                createItemDto(1L, "Test Item 1", 2),
                createItemDto(2L, "Test Item 2", 1),
                createItemDto(3L, "Test Item 3", 0),
                createItemDto(4L, "Test Item 4", 0),
                createItemDto(5L, "Test Item 5", 5),
                createItemDto(6L, "Test Item 6", 1),
                createItemDto(7L, "Test Item 7", 3)
        );

        when(itemRepo.findItemsWithCount(searchTerm, sortBy, pageSize, 0))
                .thenReturn(Flux.fromIterable(mockItems));

        Mono<List<List<ItemDto>>> result = itemService.searchItems(searchTerm, sortBy, pageNumber, pageSize);

        StepVerifier.create(result)
                .assertNext(groupedItems -> {
                    assertNotNull(groupedItems);
                    assertEquals(3, groupedItems.size()); // 7 items should be grouped into 3 rows
                    assertEquals(3, groupedItems.get(0).size());
                    assertEquals(3, groupedItems.get(1).size());
                    assertEquals(1, groupedItems.get(2).size());
                })
                .verifyComplete();

        verify(itemRepo).findItemsWithCount(searchTerm, sortBy, pageSize, 0);
    }

    @Test
    void searchItems_WithEmptyResult_ShouldReturnEmptyList() {
        // Given
        String searchTerm = "nonexistent";
        String sortBy = "PRICE";
        int pageNumber = 1;
        int pageSize = 10;

        when(itemRepo.findItemsWithCount(searchTerm, sortBy, pageSize, 0))
                .thenReturn(Flux.empty());

        // When
        Mono<List<List<ItemDto>>> result = itemService.searchItems(searchTerm, sortBy, pageNumber, pageSize);

        // Then
        StepVerifier.create(result)
                .assertNext(groupedItems -> {
                    assertNotNull(groupedItems);
                    assertTrue(groupedItems.isEmpty());
                })
                .verifyComplete();

        verify(itemRepo).findItemsWithCount(searchTerm, sortBy, pageSize, 0);
    }

    @Test
    void searchItems_WithExactMultipleOfThree_ShouldReturnFullRows() {
        // Given
        List<ItemDto> mockItems = Arrays.asList(
                createItemDto(1L, "Item 1", 1),
                createItemDto(2L, "Item 2", 1),
                createItemDto(3L, "Item 3", 1),
                createItemDto(4L, "Item 4", 1),
                createItemDto(5L, "Item 5", 1),
                createItemDto(6L, "Item 6", 1)
        );

        when(itemRepo.findItemsWithCount(null, "ALPHA", 10, 0))
                .thenReturn(Flux.fromIterable(mockItems));

        // When
        Mono<List<List<ItemDto>>> result = itemService.searchItems(null, "ALPHA", 1, 10);

        // Then
        StepVerifier.create(result)
                .assertNext(groupedItems -> {
                    assertNotNull(groupedItems);
                    assertEquals(2, groupedItems.size()); // 6 items = 2 full rows of 3
                    assertEquals(3, groupedItems.get(0).size());
                    assertEquals(3, groupedItems.get(1).size());
                })
                .verifyComplete();
    }

    @Test
    void searchItems_WithPageNumberGreaterThanOne_ShouldCalculateCorrectOffset() {
        // Given
        int pageNumber = 2;
        int pageSize = 5;
        int expectedOffset = 5;

        when(itemRepo.findItemsWithCount("test", "NO", pageSize, expectedOffset))
                .thenReturn(Flux.empty());

        // When
        Mono<List<List<ItemDto>>> result = itemService.searchItems("test", "NO", pageNumber, pageSize);

        // Then
        StepVerifier.create(result)
                .assertNext(groupedItems -> assertTrue(groupedItems.isEmpty()))
                .verifyComplete();

        verify(itemRepo).findItemsWithCount("test", "NO", pageSize, expectedOffset);
    }

    @Test
    void getPageInfo_WithExactDivision_ShouldCalculateCorrectly() {
        // Given
        int totalItems = 20;
        int pageSize = 5;
        int pageNumber = 2;

        when(itemRepo.getTotalItemsCount()).thenReturn(Mono.just(totalItems));

        // When
        Mono<Paging> result = itemService.getPageInfo(pageSize, pageNumber);

        // Then
        StepVerifier.create(result)
                .assertNext(paging -> {
                    assertNotNull(paging);
                    assertEquals(pageNumber, paging.pageNumber());
                    assertEquals(pageSize, paging.pageSize());
                    assertTrue(paging.hasNext()); // 20/5=4 pages, page 2 has next
                    assertTrue(paging.hasPrevious()); // page 2 has previous
                })
                .verifyComplete();

        verify(itemRepo).getTotalItemsCount();
    }

    @Test
    void getPageInfo_WithRemainder_ShouldCalculateCorrectly() {
        // Given
        int totalItems = 22;
        int pageSize = 5;
        int pageNumber = 5;

        when(itemRepo.getTotalItemsCount()).thenReturn(Mono.just(totalItems));

        // When
        Mono<Paging> result = itemService.getPageInfo(pageSize, pageNumber);

        // Then
        StepVerifier.create(result)
                .assertNext(paging -> {
                    assertNotNull(paging);
                    assertEquals(5, paging.pageNumber());
                    assertEquals(5, paging.pageSize());
                    assertFalse(paging.hasNext()); // 22/5=4.4 -> 5 pages, last page has no next
                    assertTrue(paging.hasPrevious()); // has previous
                })
                .verifyComplete();
    }

    @Test
    void getPageInfo_WithFirstPage_ShouldHaveNoPrevious() {
        // Given
        int totalItems = 10;
        int pageSize = 5;
        int pageNumber = 1;

        when(itemRepo.getTotalItemsCount()).thenReturn(Mono.just(totalItems));

        // When
        Mono<Paging> result = itemService.getPageInfo(pageSize, pageNumber);

        // Then
        StepVerifier.create(result)
                .assertNext(paging -> {
                    assertNotNull(paging);
                    assertEquals(1, paging.pageNumber());
                    assertTrue(paging.hasNext()); // has next page
                    assertFalse(paging.hasPrevious()); // first page, no previous
                })
                .verifyComplete();
    }

    @Test
    void getPageInfo_WithLastPage_ShouldHaveNoNext() {
        // Given
        int totalItems = 10;
        int pageSize = 5;
        int pageNumber = 2;

        when(itemRepo.getTotalItemsCount()).thenReturn(Mono.just(totalItems));

        // When
        Mono<Paging> result = itemService.getPageInfo(pageSize, pageNumber);

        // Then
        StepVerifier.create(result)
                .assertNext(paging -> {
                    assertNotNull(paging);
                    assertEquals(2, paging.pageNumber());
                    assertFalse(paging.hasNext()); // last page, no next
                    assertTrue(paging.hasPrevious()); // has previous
                })
                .verifyComplete();
    }

    @Test
    void getPageInfo_WithNoItems_ShouldReturnFirstPageWithoutNextOrPrevious() {
        // Given
        int totalItems = 0;
        int pageSize = 10;
        int pageNumber = 1;

        when(itemRepo.getTotalItemsCount()).thenReturn(Mono.just(totalItems));

        // When
        Mono<Paging> result = itemService.getPageInfo(pageSize, pageNumber);

        // Then
        StepVerifier.create(result)
                .assertNext(paging -> {
                    assertNotNull(paging);
                    assertEquals(1, paging.pageNumber());
                    assertFalse(paging.hasNext());
                    assertFalse(paging.hasPrevious());
                })
                .verifyComplete();
    }

    @Test
    void getPageInfo_WithSingleItem_ShouldCalculateSinglePage() {
        // Given
        int totalItems = 1;
        int pageSize = 10;
        int pageNumber = 1;

        when(itemRepo.getTotalItemsCount()).thenReturn(Mono.just(totalItems));

        // When
        Mono<Paging> result = itemService.getPageInfo(pageSize, pageNumber);

        // Then
        StepVerifier.create(result)
                .assertNext(paging -> {
                    assertNotNull(paging);
                    assertEquals(1, paging.pageNumber());
                    assertFalse(paging.hasNext());
                    assertFalse(paging.hasPrevious());
                })
                .verifyComplete();
    }

    @Test
    void searchItems_WithNullSearchTerm_ShouldPassNullToRepository() {
        // Given
        when(itemRepo.findItemsWithCount(null, "NO", 10, 0))
                .thenReturn(Flux.empty());

        // When
        Mono<List<List<ItemDto>>> result = itemService.searchItems(null, "NO", 1, 10);

        // Then
        StepVerifier.create(result)
                .assertNext(groupedItems -> assertTrue(groupedItems.isEmpty()))
                .verifyComplete();

        verify(itemRepo).findItemsWithCount(null, "NO", 10, 0);
    }

    @Test
    void searchItems_WithEmptySearchTerm_ShouldPassEmptyString() {
        // Given
        when(itemRepo.findItemsWithCount("", "ALPHA", 5, 0))
                .thenReturn(Flux.empty());

        // When
        Mono<List<List<ItemDto>>> result = itemService.searchItems("", "ALPHA", 1, 5);

        // Then
        StepVerifier.create(result)
                .assertNext(groupedItems -> assertTrue(groupedItems.isEmpty()))
                .verifyComplete();

        verify(itemRepo).findItemsWithCount("", "ALPHA", 5, 0);
    }

    @Test
    void getPageInfo_WhenRepositoryReturnsEmpty_ShouldHandleZeroItems() {
        // Given
        int pageSize = 10;
        int pageNumber = 1;

        when(itemRepo.getTotalItemsCount()).thenReturn(Mono.empty());

        // When
        Mono<Paging> result = itemService.getPageInfo(pageSize, pageNumber);

        // Then
        StepVerifier.create(result)
                .assertNext(paging -> {
                    assertNotNull(paging);
                    assertEquals(1, paging.pageNumber());
                    assertFalse(paging.hasNext());
                    assertFalse(paging.hasPrevious());
                })
                .verifyComplete();
    }

    @Test
    void searchItems_WithSingleItem_ShouldReturnSingleGroup() {
        // Given
        ItemDto singleItem = createItemDto(1L, "Single Item", 1);

        when(itemRepo.findItemsWithCount("single", "NO", 10, 0))
                .thenReturn(Flux.just(singleItem));

        // When
        Mono<List<List<ItemDto>>> result = itemService.searchItems("single", "NO", 1, 10);

        // Then
        StepVerifier.create(result)
                .assertNext(groupedItems -> {
                    assertNotNull(groupedItems);
                    assertEquals(1, groupedItems.size());
                    assertEquals(1, groupedItems.get(0).size());
                    assertEquals(singleItem, groupedItems.get(0).get(0));
                })
                .verifyComplete();
    }

    private ItemDto createItemDto(Long id, String title, int count) {
        return new ItemDto(id, title, "Description " + id, "/images/" + id + ".jpg", 10.0, count);
    }
}