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


    Mono<CartPosition> findByItemId(Long itemId);

    @Modifying
    @Query("UPDATE cart_positions SET count = count + 1 WHERE item_id = :itemId")
    Mono<Integer> increaseItemCount(@Param("id") Long itemId);

    @Modifying
    @Query("UPDATE cart_positions SET count = count - 1 WHERE item_id = :id")
    Mono<Integer> decreaseItemCount(@Param("id") Long itemId);

    @Modifying
    @Query("DELETE FROM cart_positions WHERE item_id = :itemId")
    Mono<Integer> removeItemFromCartByItemId(@Param("itemId") Long itemId);


    @Modifying
    @Query("DELETE FROM cart_positions")
    Mono<Integer>  clearCart();

    @Query("""
        SELECT i.*, cp.count FROM cart_positions cp
        JOIN items i on cp.item_id = i.id
        """)
    Flux<ItemDto> getAllCartPositions();
}
