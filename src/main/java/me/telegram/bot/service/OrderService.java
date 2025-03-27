package me.telegram.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pengrad.telegrambot.model.SuccessfulPayment;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.telegram.bot.config.ObjectMapperConfig;
import me.telegram.bot.model.Purchase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RequiredArgsConstructor
public class OrderService {

    private static final OrderService INSTANCE = new OrderService(
            ObjectMapperConfig.getInstance());

    private final ObjectMapper objectMapper;

    private HashMap<String, Purchase> purchaseMap = new HashMap<String, Purchase>();

    private String orderPath = "orders/orders.json";



    public void CreateUser(String chatId) throws IOException {
        Purchase purchase = Purchase.builder()
                .chatId(chatId)
                .purchaseDate(Instant.now().minus(10, ChronoUnit.DAYS))
                .count(5)
                .build();
        purchaseMap.put(chatId,purchase);
        Save();
    }

    public void Load() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(orderPath);

        // Проверяем, существует ли файл, и если нет - создаем его и все родительские папки
        if (!file.exists()) {
            file.getParentFile().mkdirs(); // Создаем все родительские папки
            file.createNewFile(); // Создаем новый файл
        }

        JsonNode jsonNode = objectMapper.readTree(file);

        purchaseMap.clear();
        objectMapper.registerModule(new JavaTimeModule());
        if (jsonNode.has("purchases")) {
            JsonNode purchasesNode = jsonNode.get("purchases");
            for (JsonNode node : purchasesNode) {
                Purchase purchase = objectMapper.readValue(node.toString(), Purchase.class);
                purchaseMap.put(purchase.getChatId(), purchase);
            }
        }
    }


    private void Save() throws IOException {
        if (!purchaseMap.isEmpty()) {
            File file = new File(orderPath);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootNode = objectMapper.createObjectNode();

            // Загрузка существующих данных из файла, если они есть
            if (file.exists()) {
                JsonNode jsonNode = objectMapper.readTree(file);

                // Обновление rootNode с узлами из jsonNode
                Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    rootNode.set(entry.getKey(), entry.getValue());
                }
            }

            ArrayNode purchasesArray = rootNode.withArray("purchases");

            // Добавление новых покупок в массив покупок
            for (Purchase purchase : purchaseMap.values()) {
                ObjectNode newPurchaseNode = objectMapper.createObjectNode();
                newPurchaseNode.put("chatId", purchase.getChatId());
                newPurchaseNode.put("purchaseDate", purchase.getPurchaseDate().toString());
                newPurchaseNode.put("count", purchase.getCount());
                purchasesArray.add(newPurchaseNode);
            }

            // Запись обновленных данных в файл
            try (FileWriter fileWriter = new FileWriter(file)) {
                ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
                writer.writeValue(fileWriter, rootNode);
                System.out.println("Новые покупки успешно добавлены в файл JSON.");
            } catch (IOException e) {
                System.out.println("Ошибка при записи в файл JSON: " + e.getMessage());
            }
        } else {
            System.out.println("Нет данных для сохранения. Файл JSON не создан.");
        }
    }

    public void CreatePurchase(String id) throws IOException {
        if (purchaseMap.containsKey(id)) {
            purchaseMap.get(id).setPurchaseDate(Instant.now());
            Save();
        }
        else {
            CreateUser(id);
            CreatePurchase(id);
        }
    }

    public static OrderService getInstance() {

        return INSTANCE;
    }

    public Boolean GetSubscription(String id) throws IOException {
        if (!purchaseMap.containsKey(id)) {
            CreateUser(id);
        }

        return purchaseMap.get(id).GetSubscription();
    }

    public Purchase GetUser(String id){
        return  purchaseMap.get(id);
    }

    public void Request(String id) throws IOException {
        if (purchaseMap.containsKey(id)) {
            purchaseMap.get(id).Request();
        }
        Save();
    }

}
