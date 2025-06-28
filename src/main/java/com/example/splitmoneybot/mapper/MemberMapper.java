package com.example.splitmoneybot.mapper;

import com.example.splitmoneybot.dto.MemberDto;
import com.example.splitmoneybot.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper implements Mapper<Member, MemberDto> {

    @Override
    public MemberDto toDto(Member entity) {
        return MemberDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .money(entity.getMoney())
                .build();
    }

    @Override
    public Member toEntity(MemberDto dto) {
        return Member.builder()
                .id(dto.getId())
                .name(dto.getName())
                .money(dto.getMoney())
                .build();
    }
}
