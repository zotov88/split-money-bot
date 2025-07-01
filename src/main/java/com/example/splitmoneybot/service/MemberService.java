package com.example.splitmoneybot.service;

import com.example.splitmoneybot.dto.MemberDto;
import com.example.splitmoneybot.entity.Member;
import com.example.splitmoneybot.mapper.MemberMapper;
import com.example.splitmoneybot.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.splitmoneybot.constant.UserState.WAITING_FOR_GROUP_MEMBER;


@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final UserService userService;

    public List<Member> saveItems(List<MemberDto> members) {
        return memberRepository.saveAll(members.stream().map(memberMapper::toEntity).toList());
    }

    public List<Member> getItems(UUID id) {
        return memberRepository.findAllById(List.of(id));
    }

    public List<Member> getMembersByNames(List<String> names) {
        return memberRepository.findAllByNameIn(names);
    }

    public SendMessage startAddMember(String callbackData, Long chatId) {
        log.debug("Start add member for user {}", chatId);

        UUID groupId = UUID.fromString(callbackData.split("_")[2]);
        userService.updateCurrentGroupId(chatId, groupId);
        userService.setState(chatId, WAITING_FOR_GROUP_MEMBER);

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Введите участников и суммы в формате\nимя1 - сумма\nимя2 - сумма")
                .build();
    }

    public String prepareGroupMembers(List<Member> members) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            sb.append(i + 1).append(". ");
            sb.append(members.get(i).getName()).append(": ");
            sb.append(members.get(i).getMoney()).append("\n");
        }
        return sb.toString();
    }

    public void addMoneyToExistsMembers(List<Member> foundMembers, List<MemberDto> requestMembers) {
        Map<String, Integer> requestMembersMap = requestMembers.stream()
                .collect(Collectors.toMap(MemberDto::getName, MemberDto::getMoney, (a, b) -> b));
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
}
