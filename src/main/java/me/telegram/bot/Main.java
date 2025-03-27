package me.telegram.bot;
import java.io.IOException;

public class Main {

    private static final String BOT_TOKEN = "7866348477:AAGupn9DoQjFhjJIfQrD4EkBviGUb2tAW1k";

    private static final String PROVIDER_TOKEN = "390540012:LIVE:60335";

    public static void main(String[] args) throws IOException {
        TelegramBotApplication application = TelegramBotApplication.builder()
                .botToken(BOT_TOKEN)
                .providerToken(PROVIDER_TOKEN)
                .build();
        application.run();
        application.orderService. Load();


    }
}