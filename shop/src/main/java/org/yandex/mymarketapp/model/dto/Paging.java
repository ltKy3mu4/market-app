package org.yandex.mymarketapp.model.dto;

public record Paging(
    int pageNumber,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious){};
