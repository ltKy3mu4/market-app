package org.yandex.mymarketapp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.yandex.mymarketapp.model.domain.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select o from Order o left join fetch o.items")
    List<Order> getAllWithPositions();

    @Query("select o from Order o left join fetch o.items where o.id = :id")
    Optional<Order> getByIdWithPositions(@Param("id") long id);


}
