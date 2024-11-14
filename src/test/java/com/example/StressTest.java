package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StressTest {
    private Exchange exchange;
    private Random random;
    private Client[] clients;

    @BeforeEach
    public void setUp() {
        exchange = new Exchange();
        random = new Random();
        int numClients = 1000;
        clients = new Client[numClients];

        // Создаем клиентов и пополняем их балансы
        for (int i = 0; i < numClients; i++) {
            clients[i] = exchange.createClient("Client" + (i + 1));
            exchange.deposit(clients[i], Currency.USD, 10000 + random.nextInt(5000));
            exchange.deposit(clients[i], Currency.EUR, 5000 + random.nextInt(5000));
        }
    }

    @Test
    public void testTotalMoneyConservation() throws InterruptedException {
        // Вычисляем общее количество денег до сделок
        double totalUSDBefore = 0;
        double totalEURBefore = 0;
        for (Client client : clients) {
            totalUSDBefore += client.getBalance(Currency.USD);
            totalEURBefore += client.getBalance(Currency.EUR);
        }

        // Создаем пул потоков
        ExecutorService executorService = Executors.newFixedThreadPool(clients.length);

        // Каждый клиент создает случайные заявки на покупку и продажу
        for (Client client : clients) {
            executorService.submit(() -> {
                for (int i = 0; i < 100; i++) {
                    try {
                        CurrencyPair pair = new CurrencyPair(Currency.EUR, Currency.USD);
                        double price = 1.2 + random.nextDouble() * 0.1; // Случайная цена от 1.2 до 1.3
                        double amount = 50 + random.nextInt(100); // Случайное количество от 50 до 150

                        if (random.nextBoolean()) {
                            exchange.createBuyOrder(client, pair, price, amount);
                        } else {
                            exchange.createSellOrder(client, pair, price, amount);
                        }

                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // Завершаем выполнение потоков
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        // Вычисляем общее количество денег после сделок
        double totalUSDAfter = 0;
        double totalEURAfter = 0;
        for (Client client : clients) {
            totalUSDAfter += client.getBalance(Currency.USD);
            totalEURAfter += client.getBalance(Currency.EUR);
        }

        // Проверяем, что общее количество денег сошлось
        assertEquals(totalUSDBefore, totalUSDAfter, 0.001, "Total USD is not conserved");
        assertEquals(totalEURBefore, totalEURAfter, 0.001, "Total EUR is not conserved");
    }
}
