package org.yandex.mymarketapp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class CartPositionsRepositoryTest extends PostgresBaseIntegrationTest {

    @Autowired
    private CartPositionsRepository cartPositionsRepository;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void beforeEach(){
        this.executeSqlScript("sql/init-cartpositions.sql");
    }
    
    Long userId = 0L;

    @Test
    void findByItemId_WhenItemExistsInCart_ShouldReturnCartPosition() {
        Mono<CartPosition> foundCartPosition = cartPositionsRepository.findByItemIdAndUserId(1L, userId);

        StepVerifier.create(foundCartPosition)
                .assertNext(cartPosition -> {
                    assertEquals(1L, cartPosition.getItemId());
                    assertTrue(cartPosition.getCount() > 0);
                })
                .verifyComplete();
    }

    @Test
    void findByItemId_WhenItemNotInCart_ShouldReturnEmpty() {
        Mono<CartPosition> foundCartPosition = cartPositionsRepository.findByItemIdAndUserId(999L, userId);

        StepVerifier.create(foundCartPosition)
                .verifyComplete();
    }

    @Test
    void increaseItemCount_ShouldIncrementCountByOne() {
        Long itemId = 1L;

        Mono<Integer> initialCountMono = cartPositionsRepository.findByItemIdAndUserId(itemId, userId)
                .map(CartPosition::getCount);

        Mono<Integer> operation = initialCountMono
                .flatMap(initialCount ->
                        cartPositionsRepository.increaseItemCount(itemId, userId)
                                .then(cartPositionsRepository.findByItemIdAndUserId(itemId, userId))
                                .map(updated -> updated.getCount())
                                .doOnNext(updatedCount -> assertEquals(initialCount + 1, updatedCount))
                );

        StepVerifier.create(operation)
                .expectNextMatches(updatedCount -> updatedCount > 0)
                .verifyComplete();
    }

    @Test
    void decreaseItemCount_ShouldDecrementCountByOne() {
        Long itemId = 1L;

        // Get initial count and ensure it's > 1
        Mono<CartPosition> initialCartPosition = cartPositionsRepository.findByItemIdAndUserId(itemId, userId)
                .filter(cp -> cp.getCount() > 1);

        Mono<Integer> operation = initialCartPosition
                .flatMap(initial ->
                        cartPositionsRepository.decreaseItemCount(itemId, userId)
                                .then(cartPositionsRepository.findByItemIdAndUserId(itemId, userId))
                                .map(updated -> updated.getCount())
                                .doOnNext(updatedCount -> assertEquals(initial.getCount() - 1, updatedCount))
                );

        StepVerifier.create(operation)
                .expectNextMatches(updatedCount -> updatedCount >= 0)
                .verifyComplete();
    }

    @Test
    void removeItemFromCartByItemId_WhenItemExists_ShouldRemoveCartPosition() {
        Long itemId = 1L;

        // First verify the item exists in cart
        StepVerifier.create(cartPositionsRepository.findByItemIdAndUserId(itemId, userId))
                .expectNextCount(1)
                .verifyComplete();

        // Remove the item
        Mono<Integer> removeOperation = cartPositionsRepository.removeItemFromCartByItemId(itemId, userId);

        StepVerifier.create(removeOperation)
                .expectNext(1) // Should remove 1 row
                .verifyComplete();

        // Verify the item is no longer in cart
        StepVerifier.create(cartPositionsRepository.findByItemIdAndUserId(itemId, userId))
                .verifyComplete();
    }

    @Test
    void removeItemFromCartByItemId_WhenItemNotInCart_ShouldReturnZero() {
        Long nonExistentItemId = 999L;

        Mono<Integer> removeOperation = cartPositionsRepository.removeItemFromCartByItemId(nonExistentItemId, userId);

        StepVerifier.create(removeOperation)
                .expectNext(0) // Should remove 0 rows
                .verifyComplete();
    }

    @Test
    void clearCart_ShouldRemoveAllCartPositions() {
        // First verify there are items in cart
        StepVerifier.create(cartPositionsRepository.findAll().collectList())
                .assertNext(positions -> assertFalse(positions.isEmpty()))
                .verifyComplete();

        // Clear the cart
        Mono<Integer> clearOperation = cartPositionsRepository.clearCart(userId);

        StepVerifier.create(clearOperation)
                .expectNextMatches(rowsAffected -> rowsAffected > 0)
                .verifyComplete();

        // Verify cart is empty
        StepVerifier.create(cartPositionsRepository.findAll().collectList())
                .assertNext(positions -> assertTrue(positions.isEmpty()))
                .verifyComplete();
    }

    @Test
    void save_ShouldPersistNewCartPosition() {
        // Get an item to add to cart
        Mono<Item> itemMono = itemRepository.findById(3L);

        Mono<CartPosition> saveOperation = itemMono
                .flatMap(item -> {
                    CartPosition newCartPosition = new CartPosition();
                    newCartPosition.setItemId(item.getId());
                    newCartPosition.setCount(1);
                    return cartPositionsRepository.save(newCartPosition);
                })
                .flatMap(saved -> cartPositionsRepository.findByItemIdAndUserId(saved.getItemId(), userId));

        StepVerifier.create(saveOperation)
                .assertNext(cartPosition -> {
                    assertNotNull(cartPosition.getId());
                    assertEquals(3L, cartPosition.getItemId());
                    assertEquals(1, cartPosition.getCount());
                })
                .verifyComplete();
    }

    @Test
    void delete_ShouldRemoveCartPosition() {
        Long cartPositionId = 1L;

        // First get the cart position to verify it exists
        Mono<CartPosition> existingPosition = cartPositionsRepository.findById(cartPositionId);

        StepVerifier.create(existingPosition)
                .expectNextCount(1)
                .verifyComplete();

        // Delete the cart position
        Mono<Void> deleteOperation = cartPositionsRepository.deleteById(cartPositionId);

        StepVerifier.create(deleteOperation)
                .verifyComplete();

        // Verify it's deleted
        StepVerifier.create(cartPositionsRepository.findById(cartPositionId))
                .verifyComplete();
    }

    @Test
    void findAll_ShouldReturnAllCartPositions() {
        Flux<CartPosition> cartPositions = cartPositionsRepository.findAll();

        StepVerifier.create(cartPositions.collectList())
                .assertNext(positions -> {
                    assertNotNull(positions);
                    assertFalse(positions.isEmpty());

                    for (CartPosition cartPosition : positions) {
                        assertNotNull(cartPosition.getId());
                        assertNotNull(cartPosition.getItemId());
                        assertTrue(cartPosition.getCount() > 0);
                    }
                })
                .verifyComplete();
    }

    @Test
    void updateCountDirectly_ShouldModifyCartPosition() {
        Long cartPositionId = 1L;
        int newCount = 5;

        Mono<CartPosition> updateOperation = cartPositionsRepository.findById(cartPositionId)
                .flatMap(cartPosition -> {
                    cartPosition.setCount(newCount);
                    return cartPositionsRepository.save(cartPosition);
                })
                .flatMap(updated -> cartPositionsRepository.findById(cartPositionId));

        StepVerifier.create(updateOperation)
                .assertNext(updatedCartPosition -> {
                    assertEquals(newCount, updatedCartPosition.getCount());
                    assertEquals(cartPositionId, updatedCartPosition.getId());
                })
                .verifyComplete();
    }

    @Test
    void getAllCartPositions_ShouldReturnItemDtos() {
        Flux<ItemDto> cartItems = cartPositionsRepository.getAllCartPositions(userId);

        StepVerifier.create(cartItems.collectList())
                .assertNext(items -> {
                    assertNotNull(items);
                    assertFalse(items.isEmpty());
                    // Add more specific assertions based on your ItemDto structure
                })
                .verifyComplete();
    }

    @Test
    void save_ShouldCreateCartPositionWithCountOne() {
        Long itemId = 3L;
        CartPosition newCartPosition = new CartPosition(itemId, userId);

        Mono<CartPosition> saveOperation = cartPositionsRepository.save(newCartPosition)
                .flatMap(saved -> cartPositionsRepository.findByItemIdAndUserId(itemId, userId));

        StepVerifier.create(saveOperation)
                .assertNext(cartPosition -> {
                    assertEquals(itemId, cartPosition.getItemId());
                    assertEquals(1, cartPosition.getCount());
                })
                .verifyComplete();
    }

    @Test
    void decreaseItemCount_WhenCountIsOne_ShouldRemoveItem() {
        Long itemId = 3L;
        CartPosition cartPosition = new CartPosition(itemId, userId);
        cartPosition.setCount(1);

        Mono<Integer> operation = cartPositionsRepository.save(cartPosition)
                .then(cartPositionsRepository.decreaseItemCount(itemId, userId));

        StepVerifier.create(operation)
                .verifyError(DataIntegrityViolationException.class);
    }
}