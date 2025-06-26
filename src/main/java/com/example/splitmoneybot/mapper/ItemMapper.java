package com.example.splitmoneybot.mapper;

import com.example.splitmoneybot.dto.ItemDto;
import com.example.splitmoneybot.entity.Item;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper implements Mapper<Item, ItemDto> {

    @Override
    public ItemDto toDto(Item entity) {
        return ItemDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .money(entity.getMoney())
                .build();
    }

    @Override
    public Item toEntity(ItemDto dto) {
        return Item.builder()
                .id(dto.getId())
                .name(dto.getName())
                .money(dto.getMoney())
                .build();
    }
}
