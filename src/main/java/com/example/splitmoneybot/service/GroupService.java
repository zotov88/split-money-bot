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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.stream.Collectors;


import static com.example.splitmoneybot.constant.BotConstant.*;
import static com.example.splitmoneybot.constant.UserState.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupService {

    private final UserService userService;
    private final MemberService memberService;
    private final GroupRepository groupRepository;
    private final Mapper<Group, GroupDto> groupMapper;

    public SendMessage createGroup(Update update, Long chatId) {
        Group createdGroup = create(chatId, update.getMessage().getText());
        if (createdGroup == null) {
            return SendMessage.builder().chatId(chatId.toString()).text(GROUP_ALREADY_EXISTS).build();
        }
        return SendMessage.builder().chatId(chatId.toString()).text(GROUP_CREATED + createdGroup.getName()).build();
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

    public SendMessage delete(Update update, Long chatId) {
        log.debug("Start delete group for user {}", chatId);
        String groupName = update.getMessage().getText();
        User user = userService.saveOrGet(chatId);

        Group foundGroup = groupRepository.findGroupByNameAndUser(groupName, user);
        if (foundGroup == null) {
            log.debug("Group {} is not found", groupName);
            return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Не найдена группа " + groupName)
                    .build();
        }
        user.getGroups().remove(foundGroup);
        log.debug("Group {} is removed", groupName);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(GROUP_DELETED + groupName)
                .build();
    }


    public List<Group> getAllGroupByChatId(Long chatId) {
        return groupRepository.findAllByChatId(chatId);
    }

    @Transactional
    public Group getGroupById(UUID id) {
        return groupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Group not found: " + id));
    }

    // TODO Обрабатывать такие случаи корректно dd - 100, bb - 44, dd - 1
    // TODO Перенести актуальную группу в отдельный сервис
    @Transactional
    public void addMember(Update update) {
        List<MemberDto> requestMembers = memberService.getMemberDtos(update);
        List<Member> foundMembers = memberService.getMembersByNames(requestMembers.stream().map(MemberDto::getName).toList());
        if (!foundMembers.isEmpty()) {
            memberService.addMoneyToExistsMembers(foundMembers, requestMembers);
            memberService.removeRepeatMembersFromRequest(foundMembers, requestMembers);
        }
        List<Member> savedMembers = memberService.saveItems(requestMembers);
        UUID groupId = CurrentGroup.get(update.getMessage().getChatId());
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Not found group " + groupId));
        group.getMembers().addAll(savedMembers);
    }

    public void deleteMember(Update update) {
        String requestName = update.getMessage().getText();
        UUID groupId = CurrentGroup.get(update.getMessage().getChatId());
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Not found group " + groupId));
        List<Member> members = group.getMembers();
        List<Member> foundMember = members.stream().filter(m -> m.getName().equals(requestName)).toList();
        if (foundMember.isEmpty()) {
            log.debug("Member {} not found in group {}", requestName, group.getName());
        } else {
            Member member = foundMember.get(0);
            members.remove(member);
            log.debug("In group {} remove {}", group.getName(), member);
        }
    }

    public SendMessage showAllGroups(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите группу:")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(prepareGroups(getAllGroupDtoByChatId(chatId)))
                        .build())
                .build();
    }

    private List<List<InlineKeyboardButton>> prepareGroups(List<GroupDto> groups) {
        List<List<InlineKeyboardButton>> buttons = groups.stream()
                .map(group -> {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(group.getName());
                    button.setCallbackData("group_" + group.getId());
                    return List.of(button);
                })
                .collect(Collectors.toList());
        List<InlineKeyboardButton> menu = List.of(
                InlineKeyboardButton.builder()
                        .text("➕")
                        .callbackData("add_group")
                        .build(),
                InlineKeyboardButton.builder()
                        .text("➖")
                        .callbackData("delete_group")
                        .build()
        );
        buttons.add(menu);
        return buttons;
    }

    private List<GroupDto> getAllGroupDtoByChatId(Long chatId) {
        return getAllGroupByChatId(chatId).stream().map(groupMapper::toDto).toList();
    }

    public SendMessage getGroup(String callbackData, Long chatId) {
        UUID groupId = UUID.fromString(callbackData.split("_")[1]);
        Group group = getGroupById(groupId);
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(InlineKeyboardButton.builder()
                                        .text("\uD83D\uDE4B\u200D♂\uFE0F")
                                        .callbackData("add_member_" + groupId)
                                        .build(),
                                InlineKeyboardButton.builder()
                                        .text("\uD83D\uDE45\u200D♂\uFE0F")
                                        .callbackData("delete_member_" + groupId)
                                        .build()),
                        List.of(InlineKeyboardButton.builder()
                                .text("\uD83D\uDE45\u200D♂\uFE0F \uD83D\uDE45\u200D♀\uFE0F")
                                .callbackData("delete_group_" + groupId)
                                .build())))
                .build();
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(memberService.prepareGroupMembers(group.getMembers()))
                .replyMarkup(keyboard)
                .build();
    }

    public SendMessage startAddGroup(Long chatId) {
        log.debug("Prepare to add group for user {}", chatId);
        userService.setState(chatId, WAITING_FOR_ADD_GROUP);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Введите имя группы")
                .build();
    }

    public SendMessage startDeleteGroup(Long chatId) {
        log.debug("Prepare to delete group for user {}", chatId);
        userService.setState(chatId, WAITING_FOR_DELETE_GROUP);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Введите имя группы")
                .build();
    }

//    public SendMessage startAddMember(String callbackData, Long chatId) {
//        log.debug("Start add member for user {}", chatId);
//        UUID groupId = UUID.fromString(callbackData.split("_")[2]);
//        userService.updateCurrentGroupId(chatId, groupId);
//        userService.setState(chatId, WAITING_FOR_ADD_MEMBER);
//        return SendMessage.builder()
//                .chatId(chatId.toString())
//                .text("Введите участников и суммы в формате\nимя1 - сумма\nимя2 - сумма")
//                .build();
//    }
}
