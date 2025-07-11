package com.example.splitmoneybot.service;

import com.example.splitmoneybot.constant.UserState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static com.example.splitmoneybot.constant.BotConstant.WELCOME_MESSAGE;
import static com.example.splitmoneybot.constant.UserState.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotHandlerService extends TelegramLongPollingBot {

    private final UserService userService;
    private final GroupService groupService;
    private final MemberService memberService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUserName;

    @Value("${regex.add-member}")
    private String regexAddMember;

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
        if (update.hasCallbackQuery()) {
            callbackHandler(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            if (update.getMessage().getText().startsWith("/")) {
                commandHandler(update);
            } else {
                textHandler(update);
            }
        }
    }

    private void callbackHandler(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        if (callbackData.startsWith("group_")) {
            executeMessage(groupService.showGroupByCallback(callbackData, chatId));
        }
        if (callbackData.startsWith("add_group")) {
            executeMessage(groupService.startAddGroup(chatId));
        }
        if (callbackData.startsWith("delete_group")) {
            executeMessage(groupService.startDeleteGroup(chatId));
        }
        if (callbackData.startsWith("add_member_")) {
            executeMessage(memberService.startAddMember(callbackData, chatId));
        }
        if (callbackData.startsWith("delete_member_")) {
            executeMessage(memberService.startDeleteMember(callbackData, chatId));
        }
        if (callbackData.startsWith("average_")) {
            executeMessage(groupService.showAverageSum(callbackData, chatId));
        }
        if (callbackData.startsWith("split_")) {
            executeMessage(groupService.showSplittedMoney(callbackData, chatId));
        }
    }

    private void commandHandler(Update update) {
        String command = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if ("/start".equals(command)) {
            executeMessage(SendMessage.builder().chatId(chatId.toString()).text(WELCOME_MESSAGE).build());
        }
        if ("/groups".equals(command)) {
            executeMessage(groupService.showAllGroups(chatId));
        }
        userService.setState(chatId, IDLE);
    }

    private void textHandler(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        UserState state = userService.getState(chatId);

        if (WAITING_FOR_ADD_GROUP.equals(state)) {
            executeMessage(groupService.createGroup(update, chatId));
            executeMessage(groupService.showAllGroups(chatId));
        }
        if (WAITING_FOR_DELETE_GROUP.equals(state)) {
            executeMessage(groupService.delete(update, chatId));
            executeMessage(groupService.showAllGroups(chatId));
        }
        if (WAITING_FOR_ADD_MEMBER.equals(state) && text.matches(regexAddMember)) {
            groupService.addMember(update);
            executeMessage(groupService.showGroup(chatId));
        }
        if (WAITING_FOR_DELETE_MEMBER.equals(state)) {
            groupService.deleteMember(update);
            executeMessage(groupService.showGroup(chatId));
        }
        userService.setState(chatId, IDLE);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message", e);
        }
    }
}
