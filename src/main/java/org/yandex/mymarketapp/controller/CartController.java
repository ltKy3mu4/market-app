package org.yandex.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.OrderService;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping("/items")
    public String showCart(Model model) {
        List<ItemDto> cartItems = cartService.getCartItems();
        double totalPrice =  cartItems.stream().mapToDouble(e-> e.price()*e.count()).sum();
        model.addAttribute("items", cartItems);
        model.addAttribute("total", totalPrice);

        return "cart";
    }

    @PostMapping("/items")
    public String updateCartItem(@RequestParam Long id, @RequestParam String action) {

        switch (action) {
            case "PLUS":
                cartService.increaseQuantityInCart(id);
                break;
            case "MINUS":
                cartService.decreaseQuantityInCart(id);
                break;
            case "DELETE":
                cartService.removeFromCart(id);
                break;
        }

        return "redirect:/cart/items";
    }

    @PostMapping("/buy")
    public String buyItems() {
        List<ItemDto> items = cartService.getCartItems();
        cartService.clearCart();
        orderService.makeOrder(items);
        return "redirect:/orders"; // Redirect to orders page after purchase
    }
}