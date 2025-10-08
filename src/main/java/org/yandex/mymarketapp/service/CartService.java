package org.yandex.mymarketapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.mapper.ItemMapper;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final ItemRepository itemsRepo;
    private final CartPositionsRepository cartRepo;
    private final ItemMapper itemMapper;

    @Transactional
    public void increaseQuantityInCart(Long itemId){
        Optional<CartPosition> cp = cartRepo.findByItemId(itemId);
        if (cp.isEmpty()) {
            log.info("adding new cart position for id {}", itemId);
            Item item = itemsRepo.getItemById(itemId).orElseThrow(() -> new ItemNotFoundException("Item "+itemId+" cannot be added to cart cause such id is not present"));
            CartPosition c = new CartPosition(item);
            cartRepo.save(c);
        } else {
            log.info("Count of items with id {} increased", itemId);
            cartRepo.increaseItemCount(itemId);
        }
    }

    @Transactional
    public void decreaseQuantityInCart(Long itemId){
        Optional<CartPosition> cp = cartRepo.findByItemId(itemId);
        if (cp.isEmpty()) {
            log.info("there is nothing to remove from cart for id {}", itemId);
            return;
        }
        if (cp.get().getCount() <= 1) {
            this.removeFromCart(itemId);
        } else {
            log.info("Count of items with id {} decreased", itemId);
            cartRepo.decreaseItemCount(itemId);
        }
    }

    @Transactional
    public void removeFromCart(Long itemId){
        log.info("removing position for id {}", itemId);
        int res = cartRepo.removeItemFromCartByItemId(itemId);
        if (res > 0) {
            log.info("successfully removed position from cart for id {}", itemId);
        } else {
            log.warn("Positions for item id {} was not found for cart", itemId);
        }
    }

    public List<ItemDto>  getCartItems(){
        List<ItemDto> dtos = new ArrayList<>();
        for (CartPosition cp : cartRepo.findAll()){
            var item = itemsRepo.getItemById(cp.getItem().getId()).orElseThrow( () -> new ItemNotFoundException("Item not found with id: " + cp.getItem().getId()));
            dtos.add(itemMapper.toDto(item, cp.getCount()));
        }
        return dtos;
    }

    @Transactional
    public void clearCart(){
        cartRepo.clearCart();
    }
}
