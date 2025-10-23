package org.yandex.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.yandex.mymarketapp.model.dto.BalanceDto;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.OrderService;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping("/items")
    public Mono<String> showCart(Model model, @RequestParam(defaultValue = "0") Long userId) {
        return cartService.getCartItems(userId)
                .collectList()
                .flatMap( items -> Mono.zip(Mono.just(items), cartService.isMoneyEnoughToBuy(userId)))
                .doOnNext(tuple -> {
                    model.addAttribute("items", tuple.getT1());
                    model.addAttribute("moneyEnough", tuple.getT2());
                    double totalPrice =  tuple.getT1().stream().mapToDouble(e-> e.price()*e.count()).sum();
                    model.addAttribute("total", totalPrice);
                }).thenReturn("cart");
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(@ModelAttribute CartBuyForm form, @RequestParam(defaultValue = "0") Long userId) {

        if (form == null || form.action() == null || form.id() == null) {
            return Mono.just(UriComponentsBuilder.fromPath("redirect:/cart/items")
                    .build()
                    .toUriString());
        }


        Mono<Void> operation = switch (form.action()) {
            case "PLUS" -> cartService.increaseQuantityInCart(form.id, userId);
            case "MINUS" -> cartService.decreaseQuantityInCart(form.id, userId);
            case "DELETE" -> cartService.removeFromCart(form.id, userId);
            default -> Mono.empty();
        };

        return operation.thenReturn("redirect:/cart/items");
    }

    @PostMapping("/buy")
    public Mono<String> buyItems(@RequestParam(defaultValue = "0") Long userId) {
        return orderService.makeOrder(userId).thenReturn("redirect:/orders");
    }

//    @GetMapping("/money-is-enough")
//    public Mono<ResponseEntity<Boolean>> isMoneyEnoughToBuy(@RequestParam(defaultValue = "0") Long userId) {
//        return cartService.isMoneyEnoughToBuy(userId).map(ResponseEntity::ok);
//    }

    public record CartBuyForm(Long id, String action){};
}