package org.yandex.mymarketapp.model.domain;

import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "items")
public class Item {
    @Id
    private long id;
    @Column
    private String title;
    @Column
    private String description;
    @Column
    private String imgPath;
    @Column
    @Positive
    private double price;
}
