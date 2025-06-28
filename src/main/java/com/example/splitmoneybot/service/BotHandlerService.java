package com.example.splitmoneybot.service;

import com.example.splitmoneybot.constant.UserState;
import com.example.splitmoneybot.dto.GroupDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.UUID;

import static com.example.splitmoneybot.constant.BotConstant.*;
import static com.example.splitmoneybot.constant.UserState.*;

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
            getGroup(callbackData, chatId);
        }
        if (callbackData.startsWith("add_member_")) {
            startAddMember(callbackData, chatId);
        }
    }

    private void getGroup(String callbackData, Long chatId) {
        UUID groupId = UUID.fromString(callbackData.split("_")[1]);
        GroupDto group = groupService.getGroupDtoById(groupId);

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(InlineKeyboardButton.builder()
                                .text("➕ Добавить участника")
                                .callbackData("add_member_" + groupId)
                                .build())
                )).build();

        String response = String.format(
                "Группа: %s\nID: %s\nУчастники: %d",
                group.getName(),
                group.getId(),
                group.getItemIds().size()
        );
        executeMessage(SendMessage.builder().chatId(chatId.toString()).text(response).replyMarkup(keyboard).build());
    }

    private void startAddMember(String callbackData, Long chatId) {
        log.debug("Start add member for user {}", chatId);

        UUID groupId = UUID.fromString(callbackData.split("_")[2]);
        userService.updateCurrentGroupId(chatId, groupId);
        userService.setState(chatId, WAITING_FOR_GROUP_MEMBER);

        executeMessage(SendMessage.builder()
                .chatId(chatId.toString())
                .text("Введите участников и суммы в формате\nимя1 - сумма\nимя2 - сумма")
                .build());
    }

    private void commandHandler(Update update) {
        String command = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if ("/start".equals(command)) {
            userService.setState(chatId, IDLE);
            executeMessage(SendMessage.builder().chatId(chatId.toString()).text(WELCOME_MESSAGE).build());
        }
        if ("/create_group".equals(command)) {
            userService.setState(chatId, WAITING_FOR_GROUP_NAME);
            executeMessage(SendMessage.builder().chatId(chatId.toString()).text(NEW_GROUP).build());
        }
        if ("/all_groups".equals(command)) {
            showAllGroups(chatId);
        }
    }

    private void showAllGroups(Long chatId) {
        List<GroupDto> groups = groupService.getAllGroupDtoByChatId(chatId);
        executeMessage(SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите группу:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(prepareGroups(groups)).build())
                .build());
    }

    private List<List<InlineKeyboardButton>> prepareGroups(List<GroupDto> groups) {
        return groups.stream()
                .map(group -> {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(group.getName());
                    button.setCallbackData("group_" + group.getId());
                    return List.of(button);
                })
                .toList();
    }

    private void textHandler(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        UserState state = userService.getState(chatId);

        if (WAITING_FOR_GROUP_NAME.equals(state) && text.startsWith("группа - ")) {
            createGroup(update, chatId);
            userService.setState(chatId, IDLE);
        }
        if (WAITING_FOR_GROUP_MEMBER.equals(state) && text.matches(regexAddMember)) {
            addMember(update);
            userService.setState(chatId, IDLE);
        }
    }

    private void createGroup(Update update, Long chatId) {
        GroupDto group = groupService.createGroup(update);
        if (group == null) {
            executeMessage(SendMessage.builder().chatId(chatId.toString()).text(GROUP_ALREADY_EXISTS).build());
        } else {
            executeMessage(SendMessage.builder().chatId(chatId.toString()).text(GROUP_CREATED).build());
        }
    }

    private void addMember(Update update) {
        groupService.addMemberToGroup(update);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending welcome message", e);
        }
    }
}
