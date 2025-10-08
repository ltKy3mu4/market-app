package org.yandex.mymarketapp.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.ItemService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemsController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping(value = {"/","/items"})
    public String getItemsPage(
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "sort", required = false, defaultValue = "NO") String sort,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") int pageNumber,
            Model model) {

        List<List<ItemDto>> items = itemService.searchItems(search, sort, pageNumber, pageSize);
        Paging paging = itemService.getPageInfo(pageSize, pageNumber);

        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", paging);
        model.addAttribute("items", items);

        return "items";
    }

    @PostMapping("/items")
    public String handleItemAction(
            @RequestParam("id") Long itemId,
            @RequestParam("action") String action,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "sort", required = false, defaultValue = "NO") String sort,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") int pageNumber) {

        switch (action) {
            case "PLUS":
                cartService.increaseQuantityInCart(itemId);
                break;
            case "MINUS":
                cartService.decreaseQuantityInCart(itemId);
                break;
        }
        return "redirect:/?search=" + search + "&sort=" + sort + "&pageSize=" + pageSize + "&pageNumber=" + pageNumber;
    }
}
