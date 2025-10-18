package org.yandex.mymarketapp.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.util.UriComponentsBuilder;
import org.yandex.mymarketapp.service.CartService;
import org.yandex.mymarketapp.service.ItemService;
import reactor.core.publisher.Mono;


@Controller
@RequiredArgsConstructor
public class ItemsController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping(value = {"/", "/items"})
    public Mono<String> getItemsPage(
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "sort", required = false, defaultValue = "NO") String sort,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") int pageNumber,
            Model model) {

        model.addAttribute("search", search);
        model.addAttribute("sort", sort);

        return itemService.getPageInfo(pageSize, pageNumber)
                .doOnNext( p -> model.addAttribute("paging", p))
                .flatMap(e -> itemService.searchItems(search, sort, pageNumber, pageSize))
                .doOnNext(items -> model.addAttribute("items", items))
                .thenReturn("items");
    }

    @PostMapping("/items")
    public Mono<String> handleItemAction(@ModelAttribute MainFormData data, @RequestParam(defaultValue = "0") Long userId) {
        if (data == null || data.action() == null || data.id() == null) {
            return Mono.just(UriComponentsBuilder.fromPath("redirect:/")
                    .queryParam("search", data== null || data.search == null ? "" : data.search)
                    .queryParam("sort", data== null || data.sort == null ? "NO" : data.sort)
                    .queryParam("pageSize", data== null || data.pageSize == null ? 10 : data.pageSize)
                    .queryParam("pageNumber", data == null || data.pageNumber == null? 1 : data.pageNumber)
                    .build()
                    .encode()
                    .toUriString());
        }


        Mono<Void> operation = switch (data.action) {
            case "PLUS" -> cartService.increaseQuantityInCart(data.id, userId);
            case "MINUS" -> cartService.decreaseQuantityInCart(data.id, userId);
            default -> Mono.empty();
        };
        return operation.then(Mono.fromCallable(() ->
                UriComponentsBuilder.fromPath("redirect:/")
                        .queryParam("search", data.search == null ? "" : data.search)
                        .queryParam("sort", data.sort == null ? "NO" : data.sort)
                        .queryParam("pageSize", data.pageSize == null ? 10 : data.pageSize)
                        .queryParam("pageNumber", data.pageNumber == null ? 1 : data.pageNumber)
                        .build()
                        .toUriString()
        ));
    }

    public record MainFormData(Long id, String action, String search,  String sort, Integer pageSize, Integer pageNumber) {}
}
