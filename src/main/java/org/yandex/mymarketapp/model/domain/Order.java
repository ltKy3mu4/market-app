package org.yandex.mymarketapp.model.domain;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Getter
@Setter
@Table(name = "orders")
public class Order {
    @Id
    private Long id;
    @Column
    @Min(value = 0)
    private double totalSum;

    @ReadOnlyProperty
    private List<OrderPosition> items;
}
