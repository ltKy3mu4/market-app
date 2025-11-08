package org.yandex.paymentservice.model.exception;

public class NotEnoughMoneyException extends PaymentServiceException {
    public NotEnoughMoneyException(String message, int extCode) {
        super(extCode, message);
    }
}
