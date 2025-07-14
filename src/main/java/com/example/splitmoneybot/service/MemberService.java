package com.example.splitmoneybot.service;

import com.example.splitmoneybot.dto.MemberDto;
import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.Member;
import com.example.splitmoneybot.mapper.MemberMapper;
import com.example.splitmoneybot.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.splitmoneybot.constant.BotConstant.*;
import static com.example.splitmoneybot.constant.UserState.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final CurrentGroupService currentGroupService;
    private final UserService userService;

    public List<Member> save(List<MemberDto> members, Group group) {
        return memberRepository.saveAll(members.stream().map(m -> memberMapper.toEntity(m, group)).toList());
    }

    public List<Member> getAll(UUID id) {
        return memberRepository.findAllById(List.of(id));
    }

    public List<Member> getMembersByNamesAndGroupId(List<String> names, UUID groupId) {
        return memberRepository.findAllByNameInAndGroupId(names, groupId);
    }

    public SendMessage startAddMember(String callbackData, Long chatId) {
        log.debug("Start add member for user {}", chatId);
        currentGroupService.update(chatId, getGroupId(callbackData));
        userService.setState(chatId, WAITING_FOR_ADD_MEMBER);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Введите участников и суммы в формате\nимя1 - сумма\nимя2 - сумма")
                .build();
    }

    public SendMessage startDeleteMember(String callbackData, Long chatId) {
        log.debug("Start delete member for user {}", chatId);
        currentGroupService.update(chatId, getGroupId(callbackData));
        userService.setState(chatId, WAITING_FOR_DELETE_MEMBER);
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Введите имя участника для удаления.")
                .build();
    }

    public String prepareGroupMembers(TreeSet<Member> members) {
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (Member member : members) {
            sb.append(count++).append(". ").append(member.getName()).append(": ");
            sb.append(member.getMoney()).append("\n");
        }
        return sb.isEmpty() ? ADD_MEMBERS : sb.toString();
    }

    public void addMoneyToExistsMembers(List<Member> foundMembers, List<MemberDto> requestMembers) {
        Map<String, Integer> requestMembersMap = requestMembers.stream()
                .collect(Collectors.toMap(MemberDto::getName, MemberDto::getMoney, (prev, curr) -> prev));
        for (Member foundMember : foundMembers) {
            if (requestMembersMap.containsKey(foundMember.getName())) {
                foundMember.setMoney(foundMember.getMoney() + requestMembersMap.get(foundMember.getName()));
            }
        }
    }

    public void removeRepeatMembersFromRequest(List<Member> foundMembers, List<MemberDto> requestMembers) {
        List<String> foundMemberNames = foundMembers.stream().map(Member::getName).toList();
        requestMembers.removeIf(requestMember -> foundMemberNames.contains(requestMember.getName()));
    }

    public List<MemberDto> getMemberDtos(Update update) {
        String[] data = update.getMessage().getText().split("\n");
        Map<String, MemberDto> memberDtoMap = Arrays.stream(data)
                .map(this::mapMemberDto)
                .collect(Collectors.toMap(MemberDto::getName, dto -> dto, this::sumMoney));

        return new ArrayList<>(memberDtoMap.values());
    }

    private MemberDto mapMemberDto(String line) {
        String[] split = line.split(" - ");
        return MemberDto.builder()
                .name(split[0])
                .money(Integer.valueOf(split[1]))
                .build();
    }

    private MemberDto sumMoney(MemberDto curr, MemberDto prev) {
        curr.setMoney(curr.getMoney() + prev.getMoney());
        return curr;
    }

    public void fillMemberDtoGroupId(Update update, List<MemberDto> requestMembers) {
        UUID groupId = currentGroupService.get(update.getMessage().getChatId());
        requestMembers.forEach(memberDto -> memberDto.setGroupId(groupId));
    }

    public Map<MemberDto, Integer> getMemberMoneyMap(List<Member> members) {
        return members.stream()
                .map(memberMapper::toDto)
                .collect(Collectors.toMap(m -> m, MemberDto::getMoney));
    }

    private UUID getGroupId(String callbackData) {
        return UUID.fromString(callbackData.split(SPLITTER)[2]);
    }
}
