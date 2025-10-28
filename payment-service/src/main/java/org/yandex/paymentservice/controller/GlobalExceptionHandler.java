package org.yandex.paymentservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.yandex.paymentservice.model.ErrorResponse;
import org.yandex.paymentservice.model.exception.NotEnoughMoneyException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotEnoughMoneyException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleApiException(NotEnoughMoneyException ex) {
        log.error("Payment API exception: {}", ex.getMessage());

        HttpStatus status = switch (ex.extCode) {
            case 404 -> HttpStatus.NOT_FOUND;
            case 400 -> HttpStatus.BAD_REQUEST;
            case 500 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return Mono.just(ResponseEntity.status(status)
                .body(new ErrorResponse("Error during payment processing", ex.extCode+"",ex.getMessage())));
    }

}
