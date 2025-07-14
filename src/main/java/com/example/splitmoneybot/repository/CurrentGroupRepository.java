package com.example.splitmoneybot.repository;

import com.example.splitmoneybot.entity.CurrentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentGroupRepository extends JpaRepository<CurrentGroup, Long> {
}
