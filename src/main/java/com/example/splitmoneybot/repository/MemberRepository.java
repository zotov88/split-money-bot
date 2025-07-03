package com.example.splitmoneybot.repository;

import com.example.splitmoneybot.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    List<Member> findAllByNameIn(List<String> names);
}
