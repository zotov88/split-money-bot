package com.example.splitmoneybot.service;

import com.example.splitmoneybot.dto.GroupDto;
import com.example.splitmoneybot.dto.MemberDto;
import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.Member;
import com.example.splitmoneybot.entity.User;
import com.example.splitmoneybot.mapper.Mapper;
import com.example.splitmoneybot.repository.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupService {

    private final UserService userService;
    private final MemberService memberService;
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
        String[] data = update.getMessage().getText().split("\n");
        List<MemberDto> memberDtos = Arrays.stream(data).map(this::mapMemberDto).toList();
        List<Member> savedMembers = memberService.saveItems(memberDtos);
        UUID groupId = userService.getCurrentGroupId(update.getMessage().getChatId());
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Not found group " + groupId));
        group.getMembers().addAll(savedMembers);
    }

    private MemberDto mapMemberDto(String d) {
        String[] split = d.split(" - ");
        return MemberDto.builder().name(split[0]).money(Integer.valueOf(split[1])).build();
    }
}
