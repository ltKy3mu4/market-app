package org.yandex.mymarketapp.model.dto;

import org.yandex.mymarketapp.model.domain.Item;

import java.util.List;

public record ViewPage(List<List<ItemDto>> items) {
}
