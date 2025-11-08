package org.yandex.mymarketapp.model.domain;

import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("cart_positions")
public class CartPosition {

    @Id
    private Long id;

    @Column("item_id")
    private Long itemId;

    @Column("count")
    @Min(value = 0, message = "Count position cannot be 0 or less")
    private int count;

    @Column("user_id")
    private Long userId;

    public CartPosition(Long itemId, Long userId) {
        this.itemId = itemId;
        this.userId = userId;
        this.count = 1;
    }
}
