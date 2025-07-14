package com.example.splitmoneybot.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BotConstant {

    public String WELCOME_MESSAGE = "Я помогу определить кто кому сколько должен денег, когда вклад в общаг не равный." +
            "\nЧтобы начать, создайте новую группу с помощью команды /new_collect";

    public String CREATE_GROUP = "Создайте группу";

    public String SELECT_GROUP = "Выберете группу";

    public String GROUP_ALREADY_EXISTS = "Уже существует такая группа";

    public String GROUP_CREATED = "Создана группа: ";

    public String GROUP_DELETED = "Группа удалена: ";

    public String ADD_MEMBERS = "Добавьте участников";

    public String SPLITTER = "_";

    public String ADD_EMOJI = "\uD83D\uDE4B\u200D♂️";

    public String DELETE_EMOJI = "\uD83D\uDE45\u200D♂️";

    public String MONEY_EMOJI = "\uD83D\uDCB5";
}
