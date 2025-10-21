package org.yandex.mymarketapp.repo;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.dto.ItemDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CartPositionsRepository extends R2dbcRepository<CartPosition, Long> {

    Flux<CartPosition> findByUserId(@Param("userId") Long userId);

    Mono<CartPosition> findByItemIdAndUserId(Long itemId, Long userId);

    @Modifying
    @Query("UPDATE cart_positions SET count = count + 1 WHERE item_id = :itemId and user_id = :userId")
    Mono<Integer> increaseItemCount(@Param("id") Long itemId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE cart_positions SET count = count - 1 WHERE item_id = :id and user_id = :userId")
    Mono<Integer> decreaseItemCount(@Param("id") Long itemId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM cart_positions WHERE item_id = :itemId and user_id = :userId")
    Mono<Integer> removeItemFromCartByItemId(@Param("itemId") Long itemId, @Param("userId") Long userId);


    @Modifying
    @Query("DELETE FROM cart_positions where user_id = :userId")
    Mono<Integer>  clearCart(@Param("userId") Long userId);

    @Query("""
        SELECT i.*, cp.count FROM cart_positions cp
        JOIN items i on cp.item_id = i.id
        WHERE cp.user_id = :userId
        """)
    Flux<ItemDto> getAllCartPositions(@Param("userId") Long userId);
}
