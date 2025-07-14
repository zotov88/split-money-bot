package com.example.splitmoneybot.service;

import com.example.splitmoneybot.entity.CurrentGroup;
import com.example.splitmoneybot.repository.CurrentGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrentGroupService {

    private final CurrentGroupRepository currentGroupRepository;

    public UUID get(Long chatId) {
        UUID groupId = currentGroupRepository.findById(chatId).map(CurrentGroup::getGroupId).orElse(null);
        log.debug("Get current group {} for {}", groupId, chatId);
        return groupId;
    }

    public void update(Long chatId, UUID groupId) {
        log.debug("Update current group {} for {}", groupId, chatId);
        currentGroupRepository.save(CurrentGroup.builder().chatId(chatId).groupId(groupId).build());
    }
}
