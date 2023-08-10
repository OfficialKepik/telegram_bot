package org.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final BotConfig botConfig;

    @Autowired
    public TelegramBot(BotConfig botConfig, UserRepository userRepository) {
        this.botConfig = botConfig;
        List<BotCommand> botCommands = new ArrayList<>();
        botCommands.add(new BotCommand("/hello", "get a welcome message"));
        botCommands.add(new BotCommand("/help", "get an info"));
        try {
            execute(new SetMyCommands(botCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error occurred " + e.getMessage());
        }
        this.userRepository = userRepository;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        String text = "";
        if (update.hasMessage() && update.getMessage().hasText()) {
            //System.out.println("ok");
            text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getChat().getFirstName();

            switch (text) {
                case "/hello":
                    hello(chatId, firstName);
                    saveUser(update.getMessage());
                    break;
                case "/help":
                    sendMessage(chatId, "This bot is created to demonstrate");
                    break;
                default:
                    sendMessage(chatId, "Sorry, the command was not recognized");
            }
        }
    }

    private void saveUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            User user = new User();
            user.setChatId(message.getChat().getId());
            user.setFirstName(message.getChat().getFirstName());
            user.setLastName(message.getChat().getLastName());
            user.setUserName(message.getChat().getUserName());
            // phone number :)
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved : {}", user);
        }
    }

    private void hello(long chatId, String firstName) {
        String answer = "Hello, " + firstName + "!";
        sendMessage(chatId, answer);
        log.info("Replied to user: " + firstName);
    }

    private void sendMessage(long chatId, String answer) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(answer);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error occurred " + e.getMessage());
        }
    }
}
