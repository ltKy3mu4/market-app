package org.yandex.mymarketapp.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ItemMapper {

    ItemDto toDto(Item item, long count);

//    List<ItemDto> toDtos(List<Item> items);
}
