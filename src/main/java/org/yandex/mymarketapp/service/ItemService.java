package org.yandex.mymarketapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.yandex.mymarketapp.model.domain.CartPosition;
import org.yandex.mymarketapp.model.domain.Item;
import org.yandex.mymarketapp.model.dto.ItemDto;
import org.yandex.mymarketapp.model.dto.Paging;
import org.yandex.mymarketapp.model.exception.ItemNotFoundException;
import org.yandex.mymarketapp.model.mapper.ItemMapper;
import org.yandex.mymarketapp.repo.CartPositionsRepository;
import org.yandex.mymarketapp.repo.ItemRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepo;
    private final CartPositionsRepository cartRepo;
    private final ItemMapper mapper;

    public ItemDto getItemById(Long id) {
       Item i =  itemRepo.getItemById(id).orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
       long count = cartRepo.findByItemId(id).orElse(new CartPosition()).getCount();
       return mapper.toDto(i, count);
    }

    public List<List<ItemDto>> searchItems(String searchTerm, String sortBy, int pageNumber, int pageSize) {
        var items = itemRepo.findItems(searchTerm, sortBy, PageRequest.of(pageNumber-1, pageSize)).stream().toList();
        if (items.isEmpty()) {
            return new ArrayList<>();
        }
        List<List<ItemDto>> itemsRows = new ArrayList<>();
        for (int row = 0; row < items.size()/3+1; row++) {
            List<ItemDto> itemRow = new ArrayList<>();
            int max = row*3 + 3 < items.size() ? row*3 + 3 : items.size();
            for (int i = row*3; i < max; i++) {
                long count = cartRepo.findByItemId(items.get(i).getId()).orElse(new CartPosition()).getCount();
                ItemDto dto = mapper.toDto(items.get(i), count);
                itemRow.add(dto);
            }
            if (!itemRow.isEmpty()){
                itemsRows.add(itemRow);
            }
        }
        return itemsRows;
    }


    public Paging getPageInfo(int pageSize, int pageNumber){
        int count = itemRepo.getTotalItemsCount();
        int pageCount = count / pageSize + (count % pageSize == 0 ? 0 : 1);
        boolean isLastPage = pageNumber == pageCount;
        boolean isFirstPage = (pageNumber == 1);
        return new Paging(pageNumber, pageSize,  !isLastPage && count != 0, !isFirstPage && count != 0);
    }


}

