package org.yandex.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.yandex.paymentservice.model.PaymentRequest;
import org.yandex.paymentservice.model.UserBalance;
import org.yandex.paymentservice.service.PaymentService;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentsApi, BalanceApi  {

    private final PaymentService paymentService;

    @Override
    @PreAuthorize("hasAuthority('SCOPE_PAYMENT')")
    public Mono<ResponseEntity<UserBalance>> getUserBalance(Long userId, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(paymentService.getBalance(userId)));
    }

    @Override
    @PreAuthorize("hasAuthority('SCOPE_PAYMENT')")

    public Mono<ResponseEntity<UserBalance>> processPayment(Long userId, Mono<PaymentRequest> paymentRequest, ServerWebExchange exchange) {
        return paymentRequest
                .map(req -> paymentService.tryProcessPayment(userId, req))
                .map(ResponseEntity::ok);
    }
}
