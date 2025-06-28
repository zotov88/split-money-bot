package com.example.splitmoneybot.service;

import com.example.splitmoneybot.constant.UserState;
import com.example.splitmoneybot.entity.User;
import com.example.splitmoneybot.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    private final Map<Long, UUID> currentGroupIdMap = new HashMap<>();

    @Transactional
    public User saveOrGet(Long chatId) {
        return userRepository.findById(chatId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .chatId(chatId)
                                .state(UserState.IDLE)
                                .build()
                ));
    }

    @Transactional
    public void setState(Long chatId, UserState state) {
        log.debug("Set state {} for {}", state, chatId);
        User user = userRepository.findById(chatId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .chatId(chatId)
                                .state(state)
                                .build()
                ));
        user.setState(state);
    }

    @Transactional()
    public UserState getState(Long chatId) {
        log.debug("Get state for {}", chatId);
        return userRepository.findById(chatId)
                .map(User::getState)
                .orElse(UserState.IDLE);
    }

    public User getById(Long chatId) {
        return userRepository.getReferenceById(chatId);
    }

    public void updateCurrentGroupId(Long chatId, UUID groupId) {
        currentGroupIdMap.put(chatId, groupId);
        log.debug("Current group {}", currentGroupIdMap);
    }

    public UUID getCurrentGroupId(Long chatId) {
        return currentGroupIdMap.get(chatId);
    }
}
