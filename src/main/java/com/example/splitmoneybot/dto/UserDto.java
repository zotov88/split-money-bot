package com.example.splitmoneybot.dto;

import com.example.splitmoneybot.constant.UserState;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserDto {

    private Long chatId;
    private UserState state;
    private List<UUID> groupIds;
}
