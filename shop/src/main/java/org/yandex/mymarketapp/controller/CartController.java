package org.yandex.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.yandex.mymarketapp.model.domain.User;
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

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/items")
    public Mono<String> showCart(Model model, @AuthenticationPrincipal User user) {
        return cartService.getCartItems(user.getId())
                .flatMap( cart -> {
                    double totalPrice = cart.items().stream().mapToDouble(e->e.getPrice()*e.getCount()).sum();
                    return Mono.zip(Mono.just(cart), cartService.isMoneyEnoughToBuy(totalPrice, user.getId()));
                })
                .doOnNext(tuple -> {
                    model.addAttribute("items", tuple.getT1().items());
                    model.addAttribute("moneyEnough", tuple.getT2());
                    double totalPrice =  tuple.getT1().items().stream().mapToDouble(e-> e.getPrice()*e.getCount()).sum();
                    model.addAttribute("total", totalPrice);
                }).thenReturn("cart");
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/items")
    public Mono<String> updateCartItem(@ModelAttribute CartBuyForm form, @AuthenticationPrincipal User user) {

        if (form == null || form.action() == null || form.id() == null) {
            return Mono.just(UriComponentsBuilder.fromPath("redirect:/cart/items")
                    .build()
                    .toUriString());
        }


        Mono<Void> operation = switch (form.action()) {
            case "PLUS" -> cartService.increaseQuantityInCart(form.id, user.getId());
            case "MINUS" -> cartService.decreaseQuantityInCart(form.id, user.getId());
            case "DELETE" -> cartService.removeFromCart(form.id, user.getId());
            default -> Mono.empty();
        };

        return operation.thenReturn("redirect:/cart/items");
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/buy")
    public Mono<String> buyItems(@AuthenticationPrincipal User user) {
        return orderService.makeOrder(user.getId()).thenReturn("redirect:/orders");
    }


    public record CartBuyForm(Long id, String action){};
}