package org.yandex.mymarketapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.ItemService;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;


    @GetMapping("/{id}")
    public String showItem(@PathVariable Long id, Model model) {
        model.addAttribute("item", itemService.getItemById(id));
        return "item";
    }


    @PostMapping("/{id}")
    public String updateItemQuantity(@PathVariable Long id, @RequestParam String action) {
        switch (action) {
            case "PLUS":
                cartService.increaseQuantityInCart(id);
                break;
            case "MINUS":
                cartService.decreaseQuantityInCart(id);
                break;
        }
        return "redirect:/items/" + id;
    }
}