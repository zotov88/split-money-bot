package com.example.splitmoneybot.repository;

import com.example.splitmoneybot.entity.Group;
import com.example.splitmoneybot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    Group findGroupByNameAndUser(String groupName, User user);

    @Query("SELECT g FROM Group g JOIN g.user u WHERE u.chatId = :chatId")
    List<Group> findAllByChatId(Long chatId);
}
