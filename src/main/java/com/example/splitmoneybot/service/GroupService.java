package com.example.splitmoneybot.service;

import com.example.splitmoneybot.dto.GroupDto;
import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.Item;
import com.example.splitmoneybot.entity.User;
import com.example.splitmoneybot.mapper.Mapper;
import com.example.splitmoneybot.repository.GroupRepository;
import com.example.splitmoneybot.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupService {

    private final UserService userService;
    private final ItemService itemService;
    private final GroupRepository groupRepository;
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
        log.debug("Create group {}", groupName);

        User user = userService.saveOrGet(chatId);
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

    public List<Group> getAllGroupByChatId(Long chatId) {
        return groupRepository.findAllByChatId(chatId);
    }

    public List<GroupDto> getAllGroupDtoByChatId(Long chatId) {
        return getAllGroupByChatId(chatId).stream().map(groupMapper::toDto).toList();
    }

    public GroupDto getGroupDtoById(UUID id) {
        return groupMapper.toDto(groupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Group not found: " + id)));
    }

    @Transactional
    public void addMemberToGroup(Update update) {
        String[] data = update.getMessage().getText().split(" - ");
        String memberName = data[0];
        Integer memberMoney = Integer.parseInt(data[1]);

        Long chatId = update.getMessage().getChatId();
        UUID groupId = userService.getCurrentGroupId(chatId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Not found group " + groupId));
        Item member = Item.builder().name(memberName).money(memberMoney).build();
        Item savedMember = itemService.saveItem(member);
        group.getItems().add(savedMember);
//        groupRepository.save(group);
    }
}
