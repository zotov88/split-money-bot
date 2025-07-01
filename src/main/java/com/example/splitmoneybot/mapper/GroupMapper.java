package com.example.splitmoneybot.mapper;

import com.example.splitmoneybot.dto.GroupDto;
import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.Member;
import com.example.splitmoneybot.service.MemberService;
import com.example.splitmoneybot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GroupMapper implements Mapper<Group, GroupDto> {

    private final UserService userService;
    private final MemberService memberService;

    @Override
    public GroupDto toDto(Group entity) {
        return GroupDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .chatId(getChatId(entity))
                .memberIds(geItemIds(entity.getMembers()))
                .build();
    }

    @Override
    public Group toEntity(GroupDto dto) {
        return Group.builder()
                .id(dto.getId())
                .name(dto.getName())
                .user(userService.getById(dto.getChatId()))
                .members(memberService.getItems(dto.getId()))
                .build();
    }

    private Long getChatId(Group entity) {
        return entity == null ? null : entity.getUser().getChatId();
    }

    private List<UUID> geItemIds(List<Member> members) {
        return members == null ? null : members.stream().map(Member::getId).toList();
    }
}
