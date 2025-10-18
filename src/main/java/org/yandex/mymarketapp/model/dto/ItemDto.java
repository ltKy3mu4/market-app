package org.yandex.mymarketapp.model.dto;


public record ItemDto(long id, String title, String description, String imgPath, double price, Integer count) {
}
