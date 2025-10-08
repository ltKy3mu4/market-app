package org.yandex.mymarketapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.yandex.mymarketapp.model.domain.CartPosition;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartPositionsRepository extends JpaRepository<CartPosition, Long> {


    Optional<CartPosition> findByItemId(Long itemId);

    @Transactional
    @Modifying
    @Query("update CartPosition cp set cp.count = cp.count+1 where cp.item.id = :id")
    void increaseItemCount(@Param("id") Long itemId);

    @Transactional
    @Modifying
    @Query("update CartPosition cp set cp.count = cp.count-1 where cp.item.id = :id")
    void decreaseItemCount(@Param("id") Long itemId);

    @Transactional
    @Modifying
    @Query("delete CartPosition cp where cp.item.id = :itemId")
    int removeItemFromCartByItemId(@Param("itemId") Long itemId);


    @Transactional
    @Modifying
    @Query("delete CartPosition cp")
    void clearCart();
}
