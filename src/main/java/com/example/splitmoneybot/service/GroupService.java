package com.example.splitmoneybot.service;

import com.example.splitmoneybot.dto.GroupDto;
import com.example.splitmoneybot.dto.MemberDto;
import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.Member;
import com.example.splitmoneybot.entity.User;
import com.example.splitmoneybot.mapper.GroupMapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.splitmoneybot.constant.BotConstant.*;
import static com.example.splitmoneybot.constant.UserState.WAITING_FOR_ADD_GROUP;
import static com.example.splitmoneybot.constant.UserState.WAITING_FOR_DELETE_GROUP;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupService {

    private final UserService userService;
    private final MemberService memberService;
    private final CurrentGroupService currentGroupService;
    private final CalculateService calculateService;
    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;

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
        Group newGroup = Group.builder().name(groupName).user(user).build();
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

    @Transactional
    public void addMember(Update update) {
        List<MemberDto> requestMembers = memberService.getMemberDtos(update);
        List<String> memberNames = requestMembers.stream().map(MemberDto::getName).toList();
        UUID groupId = currentGroupService.get(update.getMessage().getChatId());
        List<Member> foundMembers = memberService.getMembersByNamesAndGroupId(memberNames, groupId);
        if (!foundMembers.isEmpty()) {
            memberService.addMoneyToExistsMembers(foundMembers, requestMembers);
            memberService.removeRepeatMembersFromRequest(foundMembers, requestMembers);
        }
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Not found group " + groupId));
        memberService.fillMemberDtoGroupId(update, requestMembers);
        List<Member> savedMembers = memberService.save(requestMembers, group);
        group.getMembers().addAll(savedMembers);
    }

    public SendMessage deleteMember(Update update) {
        String requestName = update.getMessage().getText();
        UUID groupId = currentGroupService.get(update.getMessage().getChatId());
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Not found group " + groupId));
        List<Member> members = group.getMembers();
        List<Member> foundMember = members.stream().filter(m -> m.getName().equals(requestName)).toList();
        String text;
        if (foundMember.isEmpty()) {
            log.debug("Member {} not found in group {}", requestName, group.getName());
            text = "Member " + requestName + " not found in group " + group.getName();
        } else {
            Member member = foundMember.get(0);
            members.remove(member);
            log.debug("{} remove from {}", requestName, group.getName());
            text = requestName + " remove from " + group.getName();
        }
        return SendMessage.builder().chatId(update.getMessage().getChatId()).text(text).build();
    }

    public SendMessage showAllGroups(Long chatId) {
        List<GroupDto> groups = getAllGroupDtoByChatId(chatId);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(groups.isEmpty() ? CREATE_GROUP : SELECT_GROUP)
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(prepareGroups(groups))
                        .build())
                .build();
    }

    private List<List<InlineKeyboardButton>> prepareGroups(List<GroupDto> groups) {
        List<List<InlineKeyboardButton>> buttons = groups.stream()
                .map(group -> {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(group.getName());
                    button.setCallbackData("group" + SPLITTER + group.getId());
                    return List.of(button);
                })
                .collect(Collectors.toList());
        buttons.add(createStartMenuGroup(groups));
        return buttons;
    }

    private List<InlineKeyboardButton> createStartMenuGroup(List<GroupDto> groups) {
        List<InlineKeyboardButton> menu = new ArrayList<>();
        menu.add(InlineKeyboardButton.builder()
                .text("➕")
                .callbackData("add_group")
                .build());
        if (!groups.isEmpty()) {
            menu.add(InlineKeyboardButton.builder()
                    .text("➖")
                    .callbackData("delete_group")
                    .build());
        }
        return menu;
    }

    private List<GroupDto> getAllGroupDtoByChatId(Long chatId) {
        return getAllGroupByChatId(chatId).stream().map(groupMapper::toDto).toList();
    }

    public SendMessage showGroupByCallback(String callbackData, Long chatId) {
        currentGroupService.update(chatId, getGroupId(callbackData));
        return showGroup(chatId);
    }

    private static UUID getGroupId(String callbackData) {
        return UUID.fromString(callbackData.split(SPLITTER)[1]);
    }

    public SendMessage showGroup(Long chatId) {
        UUID groupId = currentGroupService.get(chatId);
        log.debug("Show group {} for {}", groupId, chatId);
        Group group = getGroupById(groupId);
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(createKeyboardIntoGroup(group))
                .build();
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(memberService.prepareGroupMembers(new TreeSet<>(group.getMembers())))
                .replyMarkup(keyboard)
                .build();
    }

    private List<List<InlineKeyboardButton>> createKeyboardIntoGroup(Group group) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> memberAction = new ArrayList<>();
        memberAction.add(InlineKeyboardButton.builder()
                .text(ADD_EMOJI)
                .callbackData("add_member" + SPLITTER + group.getId())
                .build());
        if (!group.getMembers().isEmpty()) {
            memberAction.add(InlineKeyboardButton.builder()
                    .text(DELETE_EMOJI)
                    .callbackData("delete_member" + SPLITTER + group.getId())
                    .build());
        }

        List<InlineKeyboardButton> calculateAction = new ArrayList<>();
        if (group.getMembers().size() > 1) {
            calculateAction.add(InlineKeyboardButton.builder()
                    .text("Средняя")
                    .callbackData("average" + SPLITTER + group.getId())
                    .build());
            calculateAction.add(InlineKeyboardButton.builder()
                    .text("Раскидать")
                    .callbackData("split" + SPLITTER + group.getId())
                    .build());
        }

        keyboard.add(memberAction);
        keyboard.add(calculateAction);
        return keyboard;
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

    public SendMessage showAverageSum(String callbackData, Long chatId) {
        Group group = getGroupById(getGroupId(callbackData));
        int avg = calculateService.avg(group.getMembers().stream().map(Member::getMoney).toList());
        return SendMessage.builder()
                .chatId(chatId)
                .text("Средняя сумма в группе " + group.getName() + " = " + avg)
                .build();
    }

    public SendMessage showSplittedMoney(String callbackData, Long chatId) {
        Group group = getGroupById(getGroupId(callbackData));
        return SendMessage.builder()
                .chatId(chatId)
                .text(calculateService.splitMoney(memberService.getMemberMoneyMap(group.getMembers())))
                .build();
    }
}
