package org.yandex.mymarketapp.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@Table(name = "order_positions")
@Entity
public class OrderPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private String title;
    private String description;
    @Column(nullable = false, name = "img_path")
    private String imgPath;
    @Positive
    private double price;
    @Positive
    private int count;
}
