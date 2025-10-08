package org.yandex.mymarketapp.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.repo.ItemRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Sql(scripts = "/sql/init-items.sql")
class ItemRepositoryTest extends PostgresBaseIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void getItemById_WhenItemExists_ShouldReturnItem() {
        Optional<Item> foundItem = itemRepository.getItemById(1L);

        assertTrue(foundItem.isPresent());
        Item item = foundItem.get();
        assertEquals(1L, item.getId());
        assertEquals("Test Item 1", item.getTitle());
        assertEquals(50.0, item.getPrice());
    }

    @Test
    void getItemById_WhenItemDoesNotExist_ShouldReturnEmptyOptional() {
        Optional<Item> foundItem = itemRepository.getItemById(999L);
        assertTrue(foundItem.isEmpty());
    }

    @Test
    void findItems_WithNoSearchTermAndNoSort_ShouldReturnAllItems() {
        Page<Item> itemsPage = itemRepository.findItems(null, "NO", PageRequest.of(0, 10));

        assertNotNull(itemsPage);
        assertEquals(3, itemsPage.getContent().size());
        assertEquals(3, itemsPage.getTotalElements());
    }

    @Test
    void findItems_WithSearchTerm_ShouldReturnMatchingItems() {
        Page<Item> itemsPage = itemRepository.findItems("Test", "NO", PageRequest.of(0, 10));

        assertNotNull(itemsPage);
        assertFalse(itemsPage.getContent().isEmpty());
        assertTrue(itemsPage.getContent()
                .stream()
                .map(Item::getTitle)
                .allMatch(title -> title.toLowerCase().contains("test")));
    }

    @Test
    void findItems_WithSearchTermInDescription_ShouldReturnMatchingItems() {
        Page<Item> itemsPage = itemRepository.findItems("Description", "NO", PageRequest.of(0, 10));

        assertNotNull(itemsPage);
        assertFalse(itemsPage.getContent().isEmpty());
    }

    @Test
    void findItems_WithNonMatchingSearchTerm_ShouldReturnEmptyPage() {
        Page<Item> itemsPage = itemRepository.findItems("NonExistentItem", "NO", PageRequest.of(0, 10));

        assertNotNull(itemsPage);
        assertTrue(itemsPage.getContent().isEmpty());
    }

    @Test
    void findItems_WithSortByPriceAsc_ShouldReturnItemsSortedByPrice() {
        Page<Item> itemsPage = itemRepository.findItems(null, "PRICE", PageRequest.of(0, 10));

        assertNotNull(itemsPage);
        List<Item> items = itemsPage.getContent();
        assertEquals(3, items.size());

        // Verify ascending price order
        for (int i = 0; i < items.size() - 1; i++) {
            assertTrue(items.get(i).getPrice() <= items.get(i + 1).getPrice());
        }
    }

    @Test
    void findItems_WithSortByAlphaAsc_ShouldReturnItemsSortedByTitle() {
        Page<Item> itemsPage = itemRepository.findItems(null, "ALPHA", PageRequest.of(0, 10));

        assertNotNull(itemsPage);
        List<Item> items = itemsPage.getContent();
        assertEquals(3, items.size());

        // Verify ascending alphabetical order
        for (int i = 0; i < items.size() - 1; i++) {
            assertTrue(items.get(i).getTitle().compareTo(items.get(i + 1).getTitle()) <= 0);
        }
    }

    @Test
    void findItems_WithSortByNo_ShouldReturnItemsSortedById() {
        Page<Item> itemsPage = itemRepository.findItems(null, "NO", PageRequest.of(0, 10));

        assertNotNull(itemsPage);
        List<Item> items = itemsPage.getContent();
        assertEquals(3, items.size());

        // Verify ascending ID order
        for (int i = 0; i < items.size() - 1; i++) {
            assertTrue(items.get(i).getId() < items.get(i + 1).getId());
        }
    }

    @Test
    void findItems_WithPagination_ShouldReturnCorrectPage() {
        // Given - assuming we have 3 items total
        int pageSize = 2;

        // When - first page
        Page<Item> firstPage = itemRepository.findItems(null, "NO", PageRequest.of(0, pageSize));

        // Then
        assertNotNull(firstPage);
        assertEquals(2, firstPage.getContent().size());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(3, firstPage.getTotalElements());
        assertTrue(firstPage.isFirst());
        assertFalse(firstPage.isLast());

        // When - second page
        Page<Item> secondPage = itemRepository.findItems(null, "NO", PageRequest.of(1, pageSize));

        // Then
        assertNotNull(secondPage);
        assertEquals(1, secondPage.getContent().size());
        assertFalse(secondPage.isFirst());
        assertTrue(secondPage.isLast());
    }

    @Test
    void findItems_WithEmptySearchTerm_ShouldReturnAllItems() {
        Page<Item> itemsPage = itemRepository.findItems("", "NO", PageRequest.of(0, 10));

        assertNotNull(itemsPage);
        assertEquals(3, itemsPage.getContent().size());
    }

    @Test
    void findItems_WithCaseInsensitiveSearch_ShouldReturnMatchingItems() {
        Page<Item> itemsPage = itemRepository.findItems("TEST", "NO", PageRequest.of(0, 10));

        assertNotNull(itemsPage);
        assertFalse(itemsPage.getContent().isEmpty());
        assertTrue(itemsPage.getContent()
                .stream()
                .map(Item::getTitle)
                .allMatch(title -> title.toLowerCase().contains("test")));
    }

    @Test
    void getTotalItemsCount_ShouldReturnCorrectCount() {
        int totalCount = itemRepository.getTotalItemsCount();

        assertEquals(3, totalCount); // Assuming 3 items in init.sql
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
        Item savedItem = itemRepository.save(newItem);

        // Then
        assertNotNull(savedItem.getId());

        Optional<Item> retrievedItem = itemRepository.getItemById(savedItem.getId());
        assertTrue(retrievedItem.isPresent());
        assertEquals("New Test Item", retrievedItem.get().getTitle());
        assertEquals(99.99, retrievedItem.get().getPrice());
    }

    @Test
    void update_ShouldModifyExistingItem() {
        // Given
        Item existingItem = itemRepository.getItemById(1L).orElseThrow();
        existingItem.setTitle("Updated Title");
        existingItem.setPrice(75.0);

        // When
        Item updatedItem = itemRepository.save(existingItem);

        // Then
        assertEquals(1L, updatedItem.getId());
        assertEquals("Updated Title", updatedItem.getTitle());
        assertEquals(75.0, updatedItem.getPrice());
    }

    @Test
    void delete_ShouldRemoveItem() {
        // Given
        Long itemId = 1L;

        // When
        itemRepository.deleteById(itemId);

        // Then
        Optional<Item> deletedItem = itemRepository.getItemById(itemId);
        assertTrue(deletedItem.isEmpty());
    }
}