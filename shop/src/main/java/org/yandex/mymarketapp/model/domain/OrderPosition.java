package org.yandex.mymarketapp.model.domain;

import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name = "order_positions")
public class OrderPosition {
    @Id
    private long id;
    @Column("order_id")
    private Long orderId;
    @Column
    private String title;
    @Column
    private String description;
    @Column
    private String imgPath;
    @Positive
    private double price;
    @Positive
    private int count;
}
