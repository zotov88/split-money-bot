package com.example.splitmoneybot.repository;

import com.example.splitmoneybot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
