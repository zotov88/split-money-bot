package com.example.splitmoneybot.service;

import com.example.splitmoneybot.constant.UserState;
import com.example.splitmoneybot.entity.User;
import com.example.splitmoneybot.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

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
        return userRepository.findById(chatId)
                .map(User::getState)
                .orElse(UserState.IDLE);
    }
}
