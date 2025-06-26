package com.example.splitmoneybot.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GroupDto {

    private UUID id;
    private String name;
    private Long chatId;
    private List<UUID> itemIds;
}
