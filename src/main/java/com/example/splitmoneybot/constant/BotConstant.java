package com.example.splitmoneybot.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BotConstant {

    public String WELCOME_MESSAGE = "Я помогу определить кто кому сколько должен денег, когда вклад в общаг не равный." +
            "\nЧтобы начать, создайте новую группу с помощью команды /new_collect";

    public String NEW_GROUP = "Введите название группы в формате" +
            "\n\"группа - название группы\".";

    public String GROUP_ALREADY_EXISTS = "Уже существует такая группа";

    public String GROUP_CREATED = "Создана группа: ";

    public String GROUP_DELETED = "Группа удалена: ";
}
