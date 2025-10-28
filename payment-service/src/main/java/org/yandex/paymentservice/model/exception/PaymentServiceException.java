package org.yandex.paymentservice.model.exception;

public class PaymentServiceException extends RuntimeException {
    public int extCode;
    public PaymentServiceException(int extCode, String message) {
        super(message);
        this.extCode = extCode;
    }
}
