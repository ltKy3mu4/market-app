package org.yandex.mymarketapp.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Sql(scripts = "/sql/init-cartpositions.sql")
class CartPositionsRepositoryTest extends PostgresBaseIntegrationTest {

    @Autowired
    private CartPositionsRepository cartPositionsRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByItemId_WhenItemExistsInCart_ShouldReturnCartPosition() {

        Optional<CartPosition> foundCartPosition = cartPositionsRepository.findByItemId(1L);

        assertTrue(foundCartPosition.isPresent());
        CartPosition cartPosition = foundCartPosition.get();
        assertEquals(1L, cartPosition.getItem().getId());
        assertTrue(cartPosition.getCount() > 0);
    }

    @Test
    void findByItemId_WhenItemNotInCart_ShouldReturnEmptyOptional() {
        Optional<CartPosition> foundCartPosition = cartPositionsRepository.findByItemId(999L);
        assertTrue(foundCartPosition.isEmpty());
    }

    @Test
    @Transactional
    void increaseItemCount_ShouldIncrementCountByOne() {
        Long itemId = 1L;
        Optional<CartPosition> initialCartPosition = cartPositionsRepository.findByItemId(itemId);
        assertTrue(initialCartPosition.isPresent());
        int initialCount = initialCartPosition.get().getCount();

        cartPositionsRepository.increaseItemCount(itemId);

        entityManager.flush();
        entityManager.clear();

        Optional<CartPosition> updatedCartPosition = cartPositionsRepository.findByItemId(itemId);
        assertTrue(updatedCartPosition.isPresent());
        assertEquals(initialCount + 1, updatedCartPosition.get().getCount());
    }

    @Test
    @Transactional
    void decreaseItemCount_ShouldDecrementCountByOne() {
        Long itemId = 1L;
        Optional<CartPosition> initialCartPosition = cartPositionsRepository.findByItemId(itemId);
        assertTrue(initialCartPosition.isPresent());
        int initialCount = initialCartPosition.get().getCount();
        assertTrue(initialCount > 1); // Ensure we can decrement

        cartPositionsRepository.decreaseItemCount(itemId);

        entityManager.flush();
        entityManager.clear();

        Optional<CartPosition> updatedCartPosition = cartPositionsRepository.findByItemId(itemId);
        assertTrue(updatedCartPosition.isPresent());
        assertEquals(initialCount - 1, updatedCartPosition.get().getCount());
    }

    @Test
    void removeItemFromCartByItemId_WhenItemExists_ShouldRemoveCartPosition() {
        Long itemId = 1L;
        Optional<CartPosition> initialCartPosition = cartPositionsRepository.findByItemId(itemId);
        assertTrue(initialCartPosition.isPresent());

        int removedCount = cartPositionsRepository.removeItemFromCartByItemId(itemId);

        assertEquals(1, removedCount);
        Optional<CartPosition> deletedCartPosition = cartPositionsRepository.findByItemId(itemId);
        assertTrue(deletedCartPosition.isEmpty());
    }

    @Test
    void removeItemFromCartByItemId_WhenItemNotInCart_ShouldReturnZero() {
        Long nonExistentItemId = 999L;

        int removedCount = cartPositionsRepository.removeItemFromCartByItemId(nonExistentItemId);

        assertEquals(0, removedCount);
    }

    @Test
    void clearCart_ShouldRemoveAllCartPositions() {
        List<CartPosition> initialCartPositions = cartPositionsRepository.findAll();
        assertFalse(initialCartPositions.isEmpty());

        cartPositionsRepository.clearCart();

        List<CartPosition> clearedCartPositions = cartPositionsRepository.findAll();
        assertTrue(clearedCartPositions.isEmpty());
    }

    @Test
    void save_ShouldPersistNewCartPosition() {
        Item newItem = itemRepository.findById(3L).orElseThrow();
        CartPosition newCartPosition = new CartPosition();
        newCartPosition.setItem(newItem);
        newCartPosition.setCount(1);

        CartPosition savedCartPosition = cartPositionsRepository.save(newCartPosition);

        assertNotNull(savedCartPosition.getId());
        Optional<CartPosition> retrievedCartPosition = cartPositionsRepository.findByItemId(newItem.getId());
        assertTrue(retrievedCartPosition.isPresent());
        assertEquals(newItem.getId(), retrievedCartPosition.get().getItem().getId());
        assertEquals(1, retrievedCartPosition.get().getCount());
    }

    @Test
    void delete_ShouldRemoveCartPosition() {
        Long cartPositionId = 1L;
        CartPosition cartPosition = cartPositionsRepository.findById(cartPositionId).orElseThrow();
        Long itemId = cartPosition.getItem().getId();

        cartPositionsRepository.deleteById(cartPositionId);

        Optional<CartPosition> deletedCartPosition = cartPositionsRepository.findById(cartPositionId);
        assertTrue(deletedCartPosition.isEmpty());

        Optional<CartPosition> byItemId = cartPositionsRepository.findByItemId(itemId);
        assertTrue(byItemId.isEmpty());
    }

    @Test
    void findAll_ShouldReturnAllCartPositions() {
        List<CartPosition> cartPositions = cartPositionsRepository.findAll();

        assertNotNull(cartPositions);
        assertFalse(cartPositions.isEmpty());

        for (CartPosition cartPosition : cartPositions) {
            assertNotNull(cartPosition.getId());
            assertNotNull(cartPosition.getItem());
            assertTrue(cartPosition.getCount() > 0);
        }
    }

    @Test
    void updateCountDirectly_ShouldModifyCartPosition() {
        Long cartPositionId = 1L;
        CartPosition cartPosition = cartPositionsRepository.findById(cartPositionId).orElseThrow();
        int newCount = 5;
        cartPosition.setCount(newCount);

        CartPosition updatedCartPosition = cartPositionsRepository.save(cartPosition);

        assertEquals(newCount, updatedCartPosition.getCount());

        CartPosition retrievedCartPosition = cartPositionsRepository.findById(cartPositionId).orElseThrow();
        assertEquals(newCount, retrievedCartPosition.getCount());
    }
}