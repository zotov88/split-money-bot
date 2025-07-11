package com.example.splitmoneybot.service;

import com.example.splitmoneybot.dto.MemberDto;
import com.example.splitmoneybot.mapper.MemberMapper;
import com.example.splitmoneybot.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberServiceTest {

    private final static long DEFAULT_CHAT_ID = 1234567L;
    private final static String DEFAULT_TEXT = "";
    @Mock
    MemberRepository memberRepository;
    MemberMapper memberMapper = new MemberMapper();
    @Mock
    UserService userService;
    private final MemberService memberService = new MemberService(memberRepository, memberMapper, userService);

    @Test
    void simple() {
        // Arrange
        Update update = getUpdate();
        update.getMessage().setText("Иван - 100");
        List<MemberDto> expected = List.of(MemberDto.builder().name("Иван").money(100).build());

        // Act
        List<MemberDto> actual = memberService.getMemberDtos(update);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void expectedMerge() {
        // Arrange
        Update update = getUpdate();
        update.getMessage().setText("Иван - 100\nИван - 99");
        List<MemberDto> expected = List.of(MemberDto.builder().name("Иван").money(199).build());

        // Act
        List<MemberDto> actual = memberService.getMemberDtos(update);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void expectedTwoDto() {
        // Arrange
        Update update = getUpdate();
        update.getMessage().setText("Иван - 100\nАлексей - 1234\nИван - 99");
        List<MemberDto> expected = List.of(
                MemberDto.builder().name("Алексей").money(1234).build(),
                MemberDto.builder().name("Иван").money(199).build()
        );

        // Act
        List<MemberDto> actual = memberService.getMemberDtos(update);

        // Assert
        assertEquals(expected, actual);
    }

    private Update getUpdate() {
        Chat chat = new Chat();
        chat.setId(DEFAULT_CHAT_ID);
        Message message = new Message();
        message.setText(DEFAULT_TEXT);
        message.setChat(chat);
        Update update = new Update();
        update.setMessage(message);

        return update;
    }
}