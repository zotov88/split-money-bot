package com.example.splitmoneybot.service;

import com.example.splitmoneybot.entity.Item;
import com.example.splitmoneybot.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;

    public List<Item> getItems(UUID id) {
        return itemRepository.findAllById(List.of(id));
    }
}
