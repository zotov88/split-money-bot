package com.example.splitmoneybot.mapper;

import com.example.splitmoneybot.dto.UserDto;
import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.User;
import com.example.splitmoneybot.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper implements Mapper<User, UserDto> {

    private final GroupService groupService;

    @Override
    public UserDto toDto(User entity) {
        return UserDto.builder()
                .chatId(entity.getChatId())
                .state(entity.getState())
                .groupIds(entity.getGroups().stream().map(Group::getId).toList())
                .build();
    }

    @Override
    public User toEntity(UserDto dto) {
        return User.builder()
                .chatId(dto.getChatId())
                .state(dto.getState())
                .groups(groupService.getAllGroupByChatId(dto.getChatId()))
                .build();
    }
}
