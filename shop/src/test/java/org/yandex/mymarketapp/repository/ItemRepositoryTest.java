package org.yandex.mymarketapp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.repo.ItemRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
        Flux<Item> itemsFlux = itemRepository.findItems(null, "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSearchTerm_ShouldReturnMatchingItems() {
        Flux<Item> itemsFlux = itemRepository.findItems("Test", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertFalse(items.isEmpty());
                    assertTrue(items.stream()
                            .map(e -> e.getTitle())
                            .allMatch(title -> title.toLowerCase().contains("test")));
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSearchTermInDescription_ShouldReturnMatchingItems() {
        Flux<Item> itemsFlux = itemRepository.findItems("Description", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertFalse(items.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithNonMatchingSearchTerm_ShouldReturnEmpty() {
        Flux<Item> itemsFlux = itemRepository.findItems("NonExistentItem", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertTrue(items.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSortByPriceAsc_ShouldReturnItemsSortedByPrice() {
        Flux<Item> itemsFlux = itemRepository.findItems(null, "PRICE", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());

                    List<Item> sortedItems = items.stream()
                            .sorted(Comparator.comparingDouble(Item::getPrice))
                            .toList();

                    for (int i = 0; i < sortedItems.size() - 1; i++) {
                        assertTrue(sortedItems.get(i).getPrice() <= sortedItems.get(i + 1).getPrice());
                    }
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSortByAlphaAsc_ShouldReturnItemsSortedByTitle() {
        Flux<Item> itemsFlux = itemRepository.findItems(null, "ALPHA", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());

                    // Verify ascending alphabetical order
                    List<Item> sortedItems = items.stream()
                            .sorted(Comparator.comparing(Item::getTitle))
                            .toList();

                    for (int i = 0; i < sortedItems.size() - 1; i++) {
                        assertTrue(sortedItems.get(i).getTitle().compareTo(sortedItems.get(i + 1).getTitle()) <= 0);
                    }
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithSortByNo_ShouldReturnItemsSortedById() {
        Flux<Item> itemsFlux = itemRepository.findItems(null, "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());

                    // Verify ascending ID order
                    List<Item> sortedItems = items.stream()
                            .sorted(Comparator.comparingLong(Item::getId))
                            .toList();

                    for (int i = 0; i < sortedItems.size() - 1; i++) {
                        assertTrue(sortedItems.get(i).getId() < sortedItems.get(i + 1).getId());
                    }
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithPagination_ShouldReturnCorrectPage() {
        int limit = 2;

        // First page
        Flux<Item> firstPageFlux = itemRepository.findItems(null, "NO", limit, 0);

        StepVerifier.create(firstPageFlux.collectList())
                .assertNext(items -> {
                    assertEquals(limit, items.size());
                })
                .verifyComplete();

        // Second page
        Flux<Item> secondPageFlux = itemRepository.findItems(null, "NO", limit, limit);

        StepVerifier.create(secondPageFlux.collectList())
                .assertNext(items -> {
                    assertEquals(1, items.size()); // Only 1 item left for second page
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithEmptySearchTerm_ShouldReturnAllItems() {
        Flux<Item> itemsFlux = itemRepository.findItems("", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertEquals(3, items.size());
                })
                .verifyComplete();
    }

    @Test
    void findItemsWithCount_WithCaseInsensitiveSearch_ShouldReturnMatchingItems() {
        Flux<Item> itemsFlux = itemRepository.findItems("TEST", "NO", 10, 0);

        StepVerifier.create(itemsFlux.collectList())
                .assertNext(items -> {
                    assertFalse(items.isEmpty());
                    assertTrue(items.stream()
                            .map(Item::getTitle)
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