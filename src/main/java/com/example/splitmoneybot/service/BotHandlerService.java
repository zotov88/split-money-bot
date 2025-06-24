package com.example.splitmoneybot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotHandlerService extends TelegramLongPollingBot {

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

        if (text.startsWith("/")) {
            commandHandler(update);
        }
    }

    private void commandHandler(Update update) {
        String command = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if ("/start".equals(command)) {
            sendWelcomeMessage(chatId);
        }
        if ("/new_collect".equals(command)) {
            log.debug("new_collect");
        }
    }

    private void sendWelcomeMessage(Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("Я помогу определить кто кому сколько должен денег, когда вклад в общаг не равный." +
                        "\nЧтобы начать, создайте новую складчину с помощью команды /new_collect")
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
