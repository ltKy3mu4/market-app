package org.yandex.mymarketapp.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yandex.mymarketapp.model.domain.Item;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDto{

    private long id;
    private String title;
    private String description;
    private String imgPath;
    private double price;
    private Integer count;

    public ItemDto(Item i, Integer count) {
        this(i.getId(), i.getTitle(), i.getDescription(), i.getImgPath(), i.getPrice(), count);
    }

    public long id(){
        return id;
    }

    public  String title(){
        return title;
    }
    public String description(){
        return description;
    }

    public String imgPath(){
        return imgPath;
    }

    public double price(){
        return price;
    }

    public Integer count(){
        return count;
    }


}
