package org.yandex.mymarketapp.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yandex.mymarketapp.model.domain.Item;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> getItemById(Long id);

    @Query("""
        SELECT i FROM Item i
        WHERE (:searchTerm IS NULL OR
               LOWER(i.title) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR
               LOWER(i.description) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')))
        ORDER BY
            CASE WHEN :sortBy = 'NO' THEN i.id END ASC,
            CASE WHEN :sortBy = 'ALPHA' THEN i.title END ASC,
            CASE WHEN :sortBy = 'PRICE' THEN i.price END ASC
        """)
    Page<Item> findItems(@Param("searchTerm") String searchTerm, @Param("sortBy") String sortBy, Pageable pageable);

    @Query("select count(o) from Item o")
    int getTotalItemsCount();
}
