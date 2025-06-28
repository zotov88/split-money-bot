package com.example.splitmoneybot.service;

import com.example.splitmoneybot.dto.MemberDto;
import com.example.splitmoneybot.entity.Member;
import com.example.splitmoneybot.mapper.MemberMapper;
import com.example.splitmoneybot.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    public List<Member> saveItems(List<MemberDto> members) {
        return memberRepository.saveAll(members.stream().map(memberMapper::toEntity).toList());
    }

    public List<Member> getItems(UUID id) {
        return memberRepository.findAllById(List.of(id));
    }
}
