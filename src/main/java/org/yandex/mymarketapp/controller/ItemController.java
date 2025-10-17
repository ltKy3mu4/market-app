package org.yandex.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.ItemService;
import reactor.core.publisher.Mono;


@Slf4j
@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;


    @GetMapping("/{id}")
    public Mono<String> showItem(@PathVariable Long id, Model model) {
        return itemService.getItemById(id)
                .doOnNext(item -> model.addAttribute("item", item))
                .thenReturn("item");
    }


    @PostMapping("/{id}")
    public Mono<String> updateItemQuantity(@PathVariable Long id, @ModelAttribute ActionForm form) {
        if (form == null || form.action() == null) {
            return Mono.just("redirect:/items/" + id);
        }

        Mono<Void> operation = switch (form.action()) {
            case "PLUS" -> cartService.increaseQuantityInCart(id);
            case "MINUS" -> cartService.decreaseQuantityInCart(id);
            default -> Mono.empty();
        };

        return operation.thenReturn("redirect:/items/" + id);
    }

    public record ActionForm (String action) {
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public Mono<String> handleItemNotFound(ItemNotFoundException ex) {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex));
    }

}