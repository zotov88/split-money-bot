package com.example.splitmoneybot.repository;

import com.example.splitmoneybot.entity.GroupItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CroupItemRepository extends JpaRepository<GroupItem, UUID> {
}
