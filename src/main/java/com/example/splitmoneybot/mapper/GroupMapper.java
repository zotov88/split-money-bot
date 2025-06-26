package com.example.splitmoneybot.mapper;

import com.example.splitmoneybot.dto.GroupDto;
import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.Item;
import com.example.splitmoneybot.service.ItemService;
import com.example.splitmoneybot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GroupMapper implements Mapper<Group, GroupDto> {

    private final UserService userService;
    private final ItemService itemService;

    @Override
    public GroupDto toDto(Group entity) {
        return GroupDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .chatId(getChatId(entity))
                .itemIds(geItemIds(entity.getItems()))
                .build();
    }

    private static Long getChatId(Group entity) {
        return entity == null ? null : entity.getUser().getChatId();
    }

    private List<UUID> geItemIds(List<Item> items) {
        return items == null ? null : items.stream().map(Item::getId).toList();
    }

    @Override
    public Group toEntity(GroupDto dto) {
        return Group.builder()
                .id(dto.getId())
                .name(dto.getName())
                .user(userService.getById(dto.getChatId()))
                .items(itemService.getItems(dto.getId()))
                .build();
    }
}
