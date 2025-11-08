package org.yandex.mymarketapp.model.exception;

public class OrderNotFoundException extends MarketException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}
