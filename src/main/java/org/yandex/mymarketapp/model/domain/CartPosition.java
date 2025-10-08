package org.yandex.mymarketapp.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cart_positions")
public class CartPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;
    @Column(nullable = false)
    @Min(value = 0, message = "Count position cannot be 0 or less")
    private int count;

    public CartPosition(Item item) {
        this.item = item;
        this.count = 1;
    }
}
