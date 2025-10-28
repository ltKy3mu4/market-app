package org.yandex.mymarketapp.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.model.dto.ViewPage;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.mapper.ItemMapperImpl;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
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
    void searchItems_WithMultipleItems_ShouldReturnGroupedRows() {
        // Given
        String searchTerm = "test";
        String sortBy = "NO";
        int pageNumber = 1;
        int pageSize = 9;

        List<Item> mockItems = Arrays.asList(
                createItemDto(1L, "Test Item 1"),
                createItemDto(2L, "Test Item 2"),
                createItemDto(3L, "Test Item 3"),
                createItemDto(4L, "Test Item 4"),
                createItemDto(5L, "Test Item 5"),
                createItemDto(6L, "Test Item 6"),
                createItemDto(7L, "Test Item 7")
        );

        when(itemRepo.findItems(searchTerm, sortBy, pageSize, 0))
                .thenReturn(Flux.fromIterable(mockItems));

        Mono<ViewPage> result = itemService.searchItems(searchTerm, sortBy, pageNumber, pageSize);

        StepVerifier.create(result)
                .assertNext(groupedItems -> {
                    assertEquals(3, groupedItems.items().size()); // 7 items should be grouped into 3 rows
                    assertEquals(3, groupedItems.items().get(0).size());
                    assertEquals(3, groupedItems.items().get(1).size());
                    assertEquals(1, groupedItems.items().get(2).size());
                })
                .verifyComplete();

        verify(itemRepo).findItems(searchTerm, sortBy, pageSize, 0);
    }

    @Test
    void searchItems_WithEmptyResult_ShouldReturnEmptyList() {
        // Given
        String searchTerm = "nonexistent";
        String sortBy = "PRICE";
        int pageNumber = 1;
        int pageSize = 10;

        when(itemRepo.findItems(searchTerm, sortBy, pageSize, 0))
                .thenReturn(Flux.empty());

        Mono<ViewPage> result = itemService.searchItems(searchTerm, sortBy, pageNumber, pageSize);

        StepVerifier.create(result)
                .assertNext(groupedItems -> {
                    assertNotNull(groupedItems);
                    assertTrue(groupedItems.items().isEmpty());
                })
                .verifyComplete();

        verify(itemRepo).findItems(searchTerm, sortBy, pageSize, 0);
    }

    @Test
    void searchItems_WithExactMultipleOfThree_ShouldReturnFullRows() {
        List<Item> mockItems = Arrays.asList(
                createItemDto(1L, "Item 1"),
                createItemDto(2L, "Item 2"),
                createItemDto(3L, "Item 3"),
                createItemDto(4L, "Item 4"),
                createItemDto(5L, "Item 5"),
                createItemDto(6L, "Item 6")
        );

        when(itemRepo.findItems(null, "ALPHA", 10, 0))
                .thenReturn(Flux.fromIterable(mockItems));

        Mono<ViewPage>  result = itemService.searchItems(null, "ALPHA", 1, 10);

        StepVerifier.create(result)
                .assertNext(groupedItems -> {
                    Assertions.assertNotNull(groupedItems);
                    Assertions.assertEquals(2, groupedItems.items().size());
                    Assertions.assertEquals(3, groupedItems.items().get(0).size());
                    Assertions.assertEquals(3, groupedItems.items().get(1).size());
                })
                .verifyComplete();
    }

    @Test
    void searchItems_WithPageNumberGreaterThanOne_ShouldCalculateCorrectOffset() {
        // Given
        int pageNumber = 2;
        int pageSize = 5;
        int expectedOffset = 5;

        when(itemRepo.findItems("test", "NO", pageSize, expectedOffset))
                .thenReturn(Flux.empty());

        Mono<ViewPage> result = itemService.searchItems("test", "NO", pageNumber, pageSize);

        StepVerifier.create(result)
                .assertNext(groupedItems -> Assertions.assertTrue(groupedItems.items().isEmpty()))
                .verifyComplete();

        verify(itemRepo).findItems("test", "NO", pageSize, expectedOffset);
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
        when(itemRepo.findItems(null, "NO", 10, 0))
                .thenReturn(Flux.empty());

        // When
        Mono<ViewPage> result = itemService.searchItems(null, "NO", 1, 10);

        // Then
        StepVerifier.create(result)
                .assertNext(groupedItems -> assertTrue(groupedItems.items().isEmpty()))
                .verifyComplete();

        verify(itemRepo).findItems(null, "NO", 10, 0);
    }

    @Test
    void searchItems_WithEmptySearchTerm_ShouldPassEmptyString() {
        // Given
        when(itemRepo.findItems("", "ALPHA", 5, 0))
                .thenReturn(Flux.empty());

        // When
        Mono<ViewPage> result =  itemService.searchItems("", "ALPHA", 1, 5);

        // Then
        StepVerifier.create(result)
                .assertNext(groupedItems -> assertTrue(groupedItems.items().isEmpty()))
                .verifyComplete();

        verify(itemRepo).findItems("", "ALPHA", 5, 0);
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
        Item singleItem = createItemDto(1L, "Single Item");

        when(itemRepo.findItems("single", "NO", 10, 0))
                .thenReturn(Flux.just(singleItem));

        Mono<ViewPage> result = itemService.searchItems("single", "NO", 1, 10);

        StepVerifier.create(result)
                .assertNext(groupedItems -> {
                    assertNotNull(groupedItems);
                    assertEquals(1, groupedItems.items().size());
                    assertEquals(1, groupedItems.items().get(0).size());
                })
                .verifyComplete();
    }

    private Item createItemDto(Long id, String title) {
        return new Item(id, title, "Description " + id, "/images/" + id + ".jpg", 10.0);
    }
}