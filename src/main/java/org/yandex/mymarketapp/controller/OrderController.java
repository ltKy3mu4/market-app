package org.yandex.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.exception.OrderNotFoundException;
import org.yandex.mymarketapp.service.OrderService;
import reactor.core.publisher.Mono;


@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public Mono<String> showOrders(Model model) {
        return orderService.getAllOrders()
                .collectList()
                .doOnNext(orders -> model.addAttribute("orders", orders))
                .thenReturn("orders");
    }

    @GetMapping("/orders/{id}")
    public Mono<String> showOrderDetails(@PathVariable Long id, Model model) {
        return orderService.getOrderById(id)
                .doOnNext(order -> model.addAttribute("order", order))
                .thenReturn("order");
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public Mono<String> handleItemNotFound(OrderNotFoundException ex) {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex));
    }

}