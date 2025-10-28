package org.yandex.mymarketapp.model.dto;

import java.util.List;

public record OrderDto (long id, List<ItemDto> items, double totalSum) {}
