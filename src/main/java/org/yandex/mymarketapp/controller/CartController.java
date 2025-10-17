package org.yandex.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
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
    public Mono<String> showCart(Model model) {
        return cartService.getCartItems()
                .collectList()
                .doOnNext(items -> {
                    model.addAttribute("items", items);
                    double totalPrice =  items.stream().mapToDouble(e-> e.price()*e.count()).sum();
                    model.addAttribute("total", totalPrice);
                }).thenReturn("cart");
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(@ModelAttribute CartBuyForm form) {

        if (form == null || form.action() == null || form.id() == null) {
            return Mono.just(UriComponentsBuilder.fromPath("redirect:/cart/items")
                    .build()
                    .toUriString());
        }


        Mono<Void> operation = switch (form.action()) {
            case "PLUS" -> cartService.increaseQuantityInCart(form.id);
            case "MINUS" -> cartService.decreaseQuantityInCart(form.id);
            case "DELETE" -> cartService.removeFromCart(form.id);
            default -> Mono.empty();
        };

        return operation.thenReturn("redirect:/cart/items");
    }

    @PostMapping("/buy")
    public Mono<String> buyItems() {
        return orderService.makeOrder().thenReturn("redirect:/orders");
    }

    public record CartBuyForm(Long id, String action){};
}