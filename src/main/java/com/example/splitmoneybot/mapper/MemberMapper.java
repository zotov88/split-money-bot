package com.example.splitmoneybot.mapper;

import com.example.splitmoneybot.dto.MemberDto;
import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberMapper {

    public MemberDto toDto(Member entity) {
        return MemberDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .money(entity.getMoney())
                .groupId(entity.getGroup().getId())
                .build();
    }

    public List<MemberDto> toDto(List<Member> entities) {
        return entities.stream().map(this::toDto).toList();
    }

    public Member toEntity(MemberDto dto, Group group) {
        return Member.builder()
                .id(dto.getId())
                .name(dto.getName())
                .money(dto.getMoney())
                .group(group)
                .build();
    }
}
