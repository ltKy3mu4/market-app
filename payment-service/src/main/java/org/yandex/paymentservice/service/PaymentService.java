package org.yandex.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yandex.paymentservice.model.PaymentRequest;
import org.yandex.paymentservice.model.UserBalance;
import org.yandex.paymentservice.model.exception.NotEnoughMoneyException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PaymentService {

    private final Map<Long, UserBalance> userBalances = new ConcurrentHashMap<>();

    public UserBalance getBalance(Long userId) {
        userBalances.computeIfAbsent(userId, k -> new UserBalance(userId, 500.0f));
        return userBalances.get(userId);
    }

    public UserBalance tryProcessPayment(Long userId, PaymentRequest req) {
        UserBalance balance = getBalance(userId);
        if (balance.getBalance() < req.getAmount()) {
            throw new NotEnoughMoneyException("Not enough money for user " + userId, 400);
        }
        balance.setBalance(balance.getBalance() - req.getAmount());
        log.info("Processed payment of {} for user {}. New balance: {}", req.getAmount(), userId, balance.getBalance());
        return balance;
    }

}
