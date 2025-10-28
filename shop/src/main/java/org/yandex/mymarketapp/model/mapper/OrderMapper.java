package org.yandex.mymarketapp.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.yandex.mymarketapp.model.domain.Order;
import org.yandex.mymarketapp.model.domain.OrderPosition;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.OrderDto;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    OrderPosition toEntity(ItemDto dto);

    List<OrderPosition> toEntities(List<ItemDto> dtos);

    ItemDto toDto(OrderPosition op);

    OrderDto toDto(Order entity);

    List<OrderDto> toDtos(List<Order> orders);
}
