package com.example.splitmoneybot.service;

import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.User;
import com.example.splitmoneybot.repository.GroupRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupService {

    private final UserService userService;
    private final GroupRepository groupRepository;

    public Group createGroup(Long chatId, String groupName) {
        User user = userService.save(chatId);
        Group newGroup = Group.builder()
                .groupName(groupName)
                .user(user)
                .build();
        Group foundGroup = groupRepository.findGroupByGroupNameAndUser(groupName, user);
        if (foundGroup != null) {
            log.debug("Group is already exists");
            return null;
        }
        user.getGroups().add(newGroup);
        return groupRepository.save(newGroup);
    }
}
