package org.yandex.mymarketapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.model.dto.ViewPage;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.repo.ItemRepository;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepo;

    @Cacheable(value = "item", key = "#id")
    public Mono<Item> getItemById(Long id) {
        return itemRepo.findById(id)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Item not found with id: " + id)))
                .doOnNext(item -> log.info("Item #{} was download from db", id));
    }

    @Cacheable(value = "item_page", key = "#searchTerm+'_'+#sortBy+'_'+#pageNumber+'_'+#pageSize")
    public Mono<ViewPage> searchItems(String searchTerm, String sortBy, int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;

        return itemRepo.findItems(searchTerm, sortBy, pageSize, offset)
                .map(i -> new ItemDto(i.getId(), i.getTitle(),i.getDescription(), i.getImgPath(), i.getPrice(), 0))
                .buffer(3)
                .filter(row -> !row.isEmpty())
                .collectList()
                .doOnNext(p -> log.info("Called Db for items page to get items"))
                .map(ViewPage::new);

    }

    @Cacheable(value = "page_info", key = "#pageNumber+'_'+#pageSize")
    public Mono<Paging> getPageInfo(int pageSize, int pageNumber) {
        return itemRepo.getTotalItemsCount()
                .defaultIfEmpty(0)
                .doOnNext(items -> log.info("Called DB for item page to get page info"))
                .map(count -> {
                    int pageCount = count / pageSize + (count % pageSize == 0 ? 0 : 1);
                    boolean isLastPage = pageNumber == pageCount;
                    boolean isFirstPage = (pageNumber == 1);
                    return new Paging(pageNumber, pageSize, !isLastPage && count != 0, !isFirstPage && count != 0);
                });
    }
}

