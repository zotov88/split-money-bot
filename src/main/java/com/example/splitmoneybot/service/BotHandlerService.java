package com.example.splitmoneybot.service;

import com.example.splitmoneybot.constant.UserState;
import com.example.splitmoneybot.entity.Group;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static com.example.splitmoneybot.constant.BotConstant.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotHandlerService extends TelegramLongPollingBot {

    private final UserService userService;
    private final GroupService groupService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUserName;

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            log.debug("Received message is empty");
            return;
        }
        if (!update.getMessage().hasText()) {
            log.debug("There is not text in message");
        }

        String text = update.getMessage().getText();
        UserState state = userService.getState(update.getMessage().getChatId());
        if (text.startsWith("/")) {
            commandHandler(update);
        }
        if (state.equals(UserState.WAITING_FOR_GROUP_NAME) && text.startsWith("группа - ")) {
            createGroup(update);
        }
    }

    private void commandHandler(Update update) {
        String command = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if ("/start".equals(command)) {
            userService.setState(chatId, UserState.IDLE);
            sendSimpleText(chatId, WELCOME_MESSAGE);
        }
        if ("/new_collect".equals(command)) {
            userService.setState(chatId, UserState.WAITING_FOR_GROUP_NAME);
            sendSimpleText(chatId, NEW_GROUP);
        }
    }

    private void createGroup(Update update) {
        String text = update.getMessage().getText();
        String[] createCollectCommand = text.split(" - ");
        if (createCollectCommand.length < 2) {
            log.debug("Not found name for collect");
            throw new RuntimeException("Not found name for collect");
        }
        String collectName = createCollectCommand[1];
        Long chatId = update.getMessage().getChatId();
        Group group = groupService.createGroup(chatId, collectName);
        if (group == null) {
            sendSimpleText(chatId, GROUP_ALREADY_EXISTS);
        }
        sendSimpleText(chatId, GROUP_CREATED);
    }

    private void sendSimpleText(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
        extractedMessage(message);
    }

    private void extractedMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending welcome message", e);
        }
    }
}
