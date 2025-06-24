package com.example.splitmoneybot.service;

import com.example.splitmoneybot.constant.UserState;
import com.example.splitmoneybot.entity.User;
import com.example.splitmoneybot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public User save(Long chatId) {
        User newUser = User.builder()
                .chatId(chatId)
                .state(UserState.IDLE)
                .build();
        return userRepository.findById(chatId).orElse(userRepository.save(newUser));
    }

    public void setState(Long chatId, UserState state) {
        User user = save(chatId);
        user.setState(state);
        userRepository.save(user);
    }

    public UserState getState(Long chatId) {
        return userRepository.findById(chatId)
                .map(User::getState)
                .orElse(UserState.IDLE);
    }
}
