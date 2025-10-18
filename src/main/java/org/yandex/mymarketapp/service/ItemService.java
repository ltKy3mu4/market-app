package org.yandex.mymarketapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.mapper.ItemMapper;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepo;

    public Mono<ItemDto> getItemById(Long id) {
        return itemRepo.findItemByIdWithCount(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found with id: " + id)));
    }

    public Mono<List<List<ItemDto>>> searchItems(String searchTerm, String sortBy, int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;

        return itemRepo.findItemsWithCount(searchTerm, sortBy, pageSize, offset)
                .buffer(3)
                .filter(row -> !row.isEmpty())
                .collectList();
    }


    public Mono<Paging> getPageInfo(int pageSize, int pageNumber) {
        return itemRepo.getTotalItemsCount()
                .defaultIfEmpty(0)
                .map(count -> {
                    int pageCount = count / pageSize + (count % pageSize == 0 ? 0 : 1);
                    boolean isLastPage = pageNumber == pageCount;
                    boolean isFirstPage = (pageNumber == 1);
                    return new Paging(pageNumber, pageSize, !isLastPage && count != 0, !isFirstPage && count != 0);
                });
    }
}

