package org.yandex.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.yandex.mymarketapp.model.domain.User;
import org.yandex.mymarketapp.model.dto.BalanceDto;
import reactor.core.publisher.Mono;

import org.yandex.payment.api.BalanceApi;
import org.yandex.payment.api.PaymentsApi;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BalanceController {

    private final BalanceApi balanceApi;

    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/balance")
    public Mono<ResponseEntity<BalanceDto>> getBalance(@AuthenticationPrincipal User user) {
        return balanceApi.getUserBalance(user.getId())
                .map(b -> new ResponseEntity<>(new BalanceDto(b.getBalance()), HttpStatus.OK))
                .doOnError(ex -> log.error("Failed to get balance for user {}", user.getId(), ex))
                .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());

    }

}
