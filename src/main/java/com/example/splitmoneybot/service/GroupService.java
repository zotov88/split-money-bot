package com.example.splitmoneybot.service;

import com.example.splitmoneybot.dto.GroupDto;
import com.example.splitmoneybot.dto.UserDto;
import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.User;
import com.example.splitmoneybot.mapper.Mapper;
import com.example.splitmoneybot.repository.GroupRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupService {

    private final UserService userService;
    private final GroupRepository groupRepository;
    private final Mapper<User, UserDto> userMapper;
    private final Mapper<Group, GroupDto> groupMapper;

    public GroupDto createGroup(Update update) {
        String text = update.getMessage().getText();
        String[] createCollectCommand = text.split(" - ");
        if (createCollectCommand.length < 2) {
            log.debug("Not found name for collect");
            throw new RuntimeException("Not found name for collect");
        }
        String groupName = createCollectCommand[1];
        Long chatId = update.getMessage().getChatId();

        return groupMapper.toDto(create(chatId, groupName));
    }

    private Group create(Long chatId, String groupName) {
        log.debug("Create group with name {}", groupName);

        User user = userMapper.toEntity(userService.saveOrGet(chatId));
        Group newGroup = Group.builder()
                .name(groupName)
                .user(user)
                .build();
        Group foundGroup = groupRepository.findGroupByNameAndUser(groupName, user);
        if (foundGroup != null) {
            log.debug("Group is already exists");
            return null;
        }
        user.getGroups().add(newGroup);

        return groupRepository.save(newGroup);
    }

//    public List<Group> getAllGroups(Long chatId) {
//        return groupRepository.findAllByChatId(chatId);
//    }
}
