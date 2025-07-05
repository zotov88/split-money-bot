package com.example.splitmoneybot.service;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@UtilityClass
@Slf4j
public class CurrentGroup {

    private final Map<Long, UUID> currentGroupIdMap = new HashMap<>();

    public void update(Long chatId, UUID groupId) {
        currentGroupIdMap.put(chatId, groupId);
        log.debug("Current group {}", currentGroupIdMap);
    }

    public UUID get(Long chatId) {
        return currentGroupIdMap.get(chatId);
    }

}
