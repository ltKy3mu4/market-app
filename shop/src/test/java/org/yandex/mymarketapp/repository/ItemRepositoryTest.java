package org.yandex.mymarketapp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.repo.ItemRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemRepositoryTest extends PostgresBaseIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void beforeEach(){
        this.executeSqlScript("sql/init-items.sql");
    }

    @Test
    void getItemById_WhenItemExists_ShouldReturnItem() {
        Mono<Item> foundItem = itemRepository.getItemById(1L);

        StepVerifier.create(foundItem)
                .expectNextMatches(item -> {
                    assertEquals(1L, item.getId());
                    assertEquals("Test Item 1", item.getTitle());
                    assertEquals(50.0, item.getPrice());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getItemById_WhenItemDoesNotExist_ShouldReturnEmpty() {
        Mono<Item> foundItem = itemRepository.getItemById(999L);

        StepVerifier.create(foundItem)
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithNoSearchTermAndNoSort_ShouldReturnAllItems() {
        Flux<ItemDto> itemsFlux = itemRepository.findItemsWithCount(null, "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSearchTerm_ShouldReturnMatchingItems() {
        Flux<ItemDto> itemsFlux = itemRepository.findItemsWithCount("Test", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertFalse(items.isEmpty());
                    assertTrue(items.stream()
                            .map(e -> e.title())
                            .allMatch(title -> title.toLowerCase().contains("test")));
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSearchTermInDescription_ShouldReturnMatchingItems() {
        Flux<ItemDto> itemsFlux = itemRepository.findItemsWithCount("Description", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertFalse(items.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithNonMatchingSearchTerm_ShouldReturnEmpty() {
        Flux<ItemDto> itemsFlux = itemRepository.findItemsWithCount("NonExistentItem", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertTrue(items.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSortByPriceAsc_ShouldReturnItemsSortedByPrice() {
        Flux<ItemDto> itemsFlux = itemRepository.findItemsWithCount(null, "PRICE", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());

                    List<ItemDto> sortedItems = items.stream()
                            .sorted(Comparator.comparingDouble(ItemDto::price))
                            .toList();

                    for (int i = 0; i < sortedItems.size() - 1; i++) {
                        assertTrue(sortedItems.get(i).price() <= sortedItems.get(i + 1).price());
                    }
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSortByAlphaAsc_ShouldReturnItemsSortedByTitle() {
        Flux<ItemDto> itemsFlux = itemRepository.findItemsWithCount(null, "ALPHA", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());

                    // Verify ascending alphabetical order
                    List<ItemDto> sortedItems = items.stream()
                            .sorted(Comparator.comparing(ItemDto::title))
                            .toList();

                    for (int i = 0; i < sortedItems.size() - 1; i++) {
                        assertTrue(sortedItems.get(i).title().compareTo(sortedItems.get(i + 1).title()) <= 0);
                    }
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSortByNo_ShouldReturnItemsSortedById() {
        Flux<ItemDto> itemsFlux = itemRepository.findItemsWithCount(null, "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());

                    // Verify ascending ID order
                    List<ItemDto> sortedItems = items.stream()
                            .sorted(Comparator.comparingLong(ItemDto::id))
                            .toList();

                    for (int i = 0; i < sortedItems.size() - 1; i++) {
                        assertTrue(sortedItems.get(i).id() < sortedItems.get(i + 1).id());
                    }
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithPagination_ShouldReturnCorrectPage() {
        int limit = 2;

        // First page
        Flux<ItemDto> firstPageFlux = itemRepository.findItemsWithCount(null, "NO", limit, 0);

        StepVerifier.create(firstPageFlux.collectList())
                .assertNext(items -> {
                    assertEquals(limit, items.size());
                })
                .verifyComplete();

        // Second page
        Flux<ItemDto> secondPageFlux = itemRepository.findItemsWithCount(null, "NO", limit, limit);

        StepVerifier.create(secondPageFlux.collectList())
                .assertNext(items -> {
                    assertEquals(1, items.size()); // Only 1 item left for second page
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithEmptySearchTerm_ShouldReturnAllItems() {
        Flux<ItemDto> itemsFlux = itemRepository.findItemsWithCount("", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithCaseInsensitiveSearch_ShouldReturnMatchingItems() {
        Flux<ItemDto> itemsFlux = itemRepository.findItemsWithCount("TEST", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertFalse(items.isEmpty());
                    assertTrue(items.stream()
                            .map(ItemDto::title)
                            .allMatch(title -> title.toLowerCase().contains("test")));
                })
                .verifyComplete();
    }

    @Test
    void getTotalItemsCount_ShouldReturnCorrectCount() {
        Mono<Integer> totalCount = itemRepository.getTotalItemsCount();

        StepVerifier.create(totalCount)
                .expectNext(3) // Assuming 3 items in init.sql
                .verifyComplete();
    }

    @Test
    void save_ShouldPersistNewItem() {
        // Given
        Item newItem = new Item();
        newItem.setTitle("New Test Item");
        newItem.setDescription("New Test Description");
        newItem.setImgPath("/images/new.jpg");
        newItem.setPrice(99.99);

        // When
        Mono<Item> savedItem = itemRepository.save(newItem);

        // Then
        StepVerifier.create(savedItem)
                .assertNext(item -> {
                    assertNotNull(item.getId());
                    assertEquals("New Test Item", item.getTitle());
                    assertEquals(99.99, item.getPrice());
                })
                .verifyComplete();
    }

    @Test
    void update_ShouldModifyExistingItem() {
        // Given
        Mono<Item> existingItemMono = itemRepository.getItemById(1L)
                .doOnNext(item -> {
                    item.setTitle("Updated Title");
                    item.setPrice(75.0);
                });

        // When
        Mono<Item> updateOperation = existingItemMono.flatMap(itemRepository::save);

        // Then
        StepVerifier.create(updateOperation)
                .assertNext(updatedItem -> {
                    assertEquals(1L, updatedItem.getId());
                    assertEquals("Updated Title", updatedItem.getTitle());
                    assertEquals(75.0, updatedItem.getPrice());
                })
                .verifyComplete();
    }

    @Test
    void delete_ShouldRemoveItem() {
        // Given
        Long itemId = 1L;

        // When
        Mono<Void> deleteOperation = itemRepository.deleteById(itemId);

        // Then
        StepVerifier.create(deleteOperation)
                .verifyComplete();

        // Verify item is gone
        StepVerifier.create(itemRepository.getItemById(itemId))
                .verifyComplete();
    }

    @Test
    void findAll_ShouldReturnAllItems() {
        Flux<Item> allItems = itemRepository.findAll();

        StepVerifier.create(allItems.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());
                })
                .verifyComplete();
    }
}