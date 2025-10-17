package org.yandex.mymarketapp.repo;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ItemRepository extends R2dbcRepository<Item, Long> {

    Mono<Item> getItemById(Long id);

    @Query("""
        SELECT i.*, cp.count FROM items i
        LEFT JOIN cart_positions cp on i.id = cp.item_id
        WHERE (:searchTerm IS NULL OR
               LOWER(i.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(i.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY
            CASE WHEN :sortBy = 'NO' THEN i.id END ASC,
            CASE WHEN :sortBy = 'ALPHA' THEN i.title END ASC,
            CASE WHEN :sortBy = 'PRICE' THEN i.price END ASC
        LIMIT :limit OFFSET :offset
        """)
    Flux<ItemDto> findItemsWithCount(@Param("searchTerm") String searchTerm, @Param("sortBy") String sortBy, Integer limit, Integer offset);

    @Query("""
        SELECT i.*, cp.count FROM items i
        LEFT JOIN cart_positions cp on i.id = cp.item_id
        where i.id = :id
        """)
    Mono<ItemDto> findItemByIdWithCount(@Param("id") long id);

    @Query("SELECT COUNT(*) FROM items")
    Mono<Integer> getTotalItemsCount();
}
