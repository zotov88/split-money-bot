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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.splitmoneybot.constant.BotConstant.GROUP_ALREADY_EXISTS;
import static com.example.splitmoneybot.constant.BotConstant.GROUP_CREATED;

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

    @Transactional
    public Group getGroupById(UUID id) {
        return groupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Group not found: " + id));
    }

    @Transactional
    public void addMember(Update update) {
        List<MemberDto> requestMembers = getMemberDtos(update);
        List<Member> foundMembers = memberService.getMembersByNames(requestMembers.stream().map(MemberDto::getName).toList());
        if (!foundMembers.isEmpty()) {
            memberService.addMoneyToExistsMembers(foundMembers, requestMembers);
            memberService.removeRepeatMembersFromRequest(foundMembers, requestMembers);
        }
        List<Member> savedMembers = memberService.saveItems(requestMembers);
        UUID groupId = userService.getCurrentGroupId(update.getMessage().getChatId());
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Not found group " + groupId));
        group.getMembers().addAll(savedMembers);
    }

    public void deleteMember(Update update) {
        String requestName = update.getMessage().getText();
        UUID groupId = userService.getCurrentGroupId(update.getMessage().getChatId());
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

    private List<MemberDto> getMemberDtos(Update update) {
        String[] data = update.getMessage().getText().split("\n");
        return Arrays.stream(data).map(this::mapMemberDto).collect(Collectors.toList());
    }

    public SendMessage createGroup(Update update, Long chatId) {
        GroupDto group = createGroup(update);
        if (group == null) {
            return SendMessage.builder().chatId(chatId.toString()).text(GROUP_ALREADY_EXISTS).build();
        } else {
            return SendMessage.builder().chatId(chatId.toString()).text(GROUP_CREATED).build();
        }
    }

    public SendMessage showAllGroups(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите группу:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(prepareGroups(getAllGroupDtoByChatId(chatId))).build())
                .build();
    }

    private List<List<InlineKeyboardButton>> prepareGroups(List<GroupDto> groups) {
        return groups.stream()
                .map(group -> {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(group.getName());
                    button.setCallbackData("group_" + group.getId());
                    return List.of(button);
                })
                .toList();
    }

    private MemberDto mapMemberDto(String d) {
        String[] split = d.split(" - ");
        return MemberDto.builder()
                .name(split[0])
                .money(Integer.valueOf(split[1]))
                .build();
    }

    public SendMessage getGroup(String callbackData, Long chatId) {
        UUID groupId = UUID.fromString(callbackData.split("_")[1]);
        Group group = getGroupById(groupId);
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(InlineKeyboardButton.builder()
                                        .text("➕ Добавить")
                                        .callbackData("add_member_" + groupId)
                                        .build(),
                                InlineKeyboardButton.builder()
                                        .text("➖ Удалить")
                                        .callbackData("delete_member_" + groupId)
                                        .build())
                ))
                .build();
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(memberService.prepareGroupMembers(group.getMembers()))
                .replyMarkup(keyboard)
                .build();
    }
}
