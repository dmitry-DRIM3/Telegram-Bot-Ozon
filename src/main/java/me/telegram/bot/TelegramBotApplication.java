package me.telegram.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PreCheckoutQuery;
import com.pengrad.telegrambot.model.SuccessfulPayment;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.AnswerPreCheckoutQuery;
import com.pengrad.telegrambot.request.CreateInvoiceLink;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.StringResponse;
import lombok.SneakyThrows;
import me.telegram.bot.service.OrderService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramBotApplication extends TelegramBot {

    private final ExecutorService executorService;

    public final OrderService orderService;

    private final String providerToken;
    private BigDecimal price = BigDecimal.valueOf(250.00);

    @lombok.Builder
    public TelegramBotApplication(String botToken, String providerToken) throws IOException {
        super(botToken);
        this.providerToken = Optional.ofNullable(providerToken).orElse("");
        this.executorService = Executors.newFixedThreadPool(8);
        this.orderService = OrderService.getInstance();


    }

    public void run(){

        this.setUpdatesListener(updates -> {
            updates.stream()
                    .<Runnable>map(update -> () -> process(update))
                    .forEach(executorService::submit);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, exception -> System.out.println(exception.response().description()));


    }

    private void process(Update update) {
        Message message = update.message();
        if (message != null) {
            Optional.ofNullable(message.text())
                    .ifPresent(commandName -> this.serveCommand(commandName, message.chat().id()));
            Optional.ofNullable(message.successfulPayment())
                    .ifPresent(payment -> {
                        try {
                            servePayment(payment, message.chat().id());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            Optional.ofNullable(message.successfulPayment())
                    .ifPresent(payment -> Log(message.chat().id(),message));

        } else if (update.preCheckoutQuery() != null) {
            PreCheckoutQuery preCheckoutQuery = update.preCheckoutQuery();
            execute(new AnswerPreCheckoutQuery(preCheckoutQuery.id()));
        }
    }

    private void servePayment(SuccessfulPayment payment, Long id) throws IOException {
        orderService.CreatePurchase(id.toString());
    }

    public void Log(Long id, Message text){
        System.out.print("Buy "+id+text.text());
    }

    @SneakyThrows
    private void  serveCommand(String commandName, Long chatId) {
        switch (commandName) {
            case "/start": {
                SendMessage response = new SendMessage(chatId,
                        "Список команд:\n/menu - Главное меню\nНапишите артикул товара ozon");
                this.execute(response);
                break;
            }
            case "/menu": {
                SendMessage response = new SendMessage(chatId, "Меню")
                        .replyMarkup(new ReplyKeyboardMarkup(new String[][] {
                                {"Подписка"},
                                {"Поддержка"}
                        }).resizeKeyboard(true));
                this.execute(response);
                break;
            }
            case "Поддержка":{
             SendMessage response = new SendMessage(chatId, "По всем вопросам:")
                     .replyMarkup(new InlineKeyboardMarkup(
                             new InlineKeyboardButton("Разработчик").url("https://t.me/Drmi3s")
                     ));
                this.execute(response);
                break;
            }
            case "Подписка": {
                CreateInvoiceLink link = new CreateInvoiceLink("Подписка", "подписка на 7 дней", "0",
                        providerToken, "RUB",
                        new LabeledPrice("Цена", price.multiply(BigDecimal.valueOf(100L)).intValue()))
                        .needShippingAddress(false)
                        .needName(false)
                        .needPhoneNumber(false);

                StringResponse response = execute(link);
                SendMessage sendPhoto = new SendMessage(chatId,"Подписка на 7 дней:")
                        .replyMarkup(new InlineKeyboardMarkup(
                                new InlineKeyboardButton("Оплатить").url(response.result())
                        ));
                this.execute(sendPhoto);
                break;
            }
            default: {
                    var information = Parse(commandName);

                    if(information[0].length() > 1 && orderService.GetSubscription(chatId.toString()))
                    {



                        String urlPhoto = extractImageLink(information[1]);
                        orderService.Request(chatId.toString());
                        String subCount = orderService.GetUser(chatId.toString()).getCount().toString();
                        SendPhoto photo = new SendPhoto(chatId, urlPhoto)
                                .caption(information[0] + " Запросов:"+subCount);

                        this.execute(photo);
                    }
                    else if(!orderService.GetSubscription(chatId.toString())){
                        SendMessage response = new SendMessage(chatId, "Пробные запросы закончились. Оформите подписку на 7 дней");
                        this.execute(response);
                    }
                    else{
                        SendMessage response = new SendMessage(chatId, "Товар не найден.");
                        this.execute(response);
                    }
                break;
            }
        }


    }



    private String[] Parse(String id) throws IOException {
        String name = "dimas.efremov.2016@mail.ru";
        String password = "lbvflbvf1";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 YaBrowser/24.10.0.0 Safari/537.36";
        Connection.Response loginForm = Jsoup.connect("https://app.mayak.bz/users/sign_in")
                .method(Connection.Method.GET)
                .execute();
        String token = "";
        Document doc = loginForm.parse();
        for (Element meta : doc.select("meta")) {
            if (meta.attr("name").equals("csrf-token")) {
                token = meta.attr("content");
            }
        }
        HashMap<String, String> cookies = new HashMap<>(loginForm.cookies());
        System.out.println(token);
        loginForm = Jsoup.connect("https://app.mayak.bz/users/sign_in").userAgent(userAgent)
                .data("authenticity_token", token)
                .data("user[login]", name)
                .data("user[password]", password)
                .data("commit", "Войти")
                .cookies(cookies)
                .method(Connection.Method.POST)

                .execute();

        cookies = new HashMap<>(loginForm.cookies());

        Document dashboardPage = Jsoup.connect("https://app.mayak.bz/ozon/products/"+id)
                .cookies(cookies)
                .userAgent(userAgent)
                .get();


        var elementsWithClass = dashboardPage.select("#summary-tab > div > div.card-body > div.d-lg-flex.justify-content-between");
        String information = elementsWithClass.text().substring(0,elementsWithClass.text().indexOf('.')+1);
        String photo = String.valueOf(dashboardPage.select("#body > div.wrap > main > div.container-fluid > div.ozon-product-view.flex-column > div.flex-grow-1.mr-2 > div > img"));
        return new String[]{information,photo};
    }

    public static String extractImageLink(String input) {

        String regex = "<img[^>]+src\s*=\s*['\"]([^'\"]+)['\"][^>]*>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);


        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }
}
