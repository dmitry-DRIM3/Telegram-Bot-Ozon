package me.telegram.bot.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Data
@Builder
public class Purchase {
    private String chatId;
    private Instant purchaseDate;
    private Integer count;

    // Установка значения count в методе @Builder
    @Builder
    public Purchase(String chatId, Instant purchaseDate, Integer count) {
        this.chatId = chatId;
        this.purchaseDate = purchaseDate;
        this.count = count;
    }

    public Boolean GetSubscription(){
        return count > 0 || purchaseDate.isAfter(Instant.now().minus(7, ChronoUnit.DAYS));
    }

    public void Request(){
        count--;

        count = Math.clamp(count,0,1000);
    }
}
