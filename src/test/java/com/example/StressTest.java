package com.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StressTest {
    private Exchange exchange;
    private Random random;
    private Client[] clients;
    private Semaphore semaphore;

    @BeforeEach
    public void setUp() {
        exchange = new Exchange(); // Инициализируем Exchange
        random = new Random();
        int numClients = 1000;
        clients = new Client[numClients];

        // Создаем клиентов и пополняем их балансы для всех валют
        for (int i = 0; i < numClients; i++) {
            clients[i] = exchange.createClient("Client" + (i + 1));
            for (Currency currency : Currency.values()) {
                // Баланс от 5000 до 10000 для каждой валюты
                exchange.deposit(clients[i], currency,  (5000 + random.nextLong(5000)));
            }
        }

        // Устанавливаем семафор на 10 одновременных задач (можно настроить по необходимости)
        semaphore = new Semaphore(10);
    }

    @Test
    public void testTotalMoneyConservationWithTradeGraph() throws InterruptedException {
        // Вычисляем общее количество денег до сделок для всех валют
        Map<Currency, Long> totalBefore = new HashMap<>();
        for (Currency currency : Currency.values()) {
            totalBefore.put(currency, 0L);
        }

        for (Client client : clients) {
            for (Currency currency : Currency.values()) {
                totalBefore.put(currency, totalBefore.get(currency) +
                        exchange.getClientBalanceManager().getBalance(client, currency));
            }
        }

        // Создаем пул потоков
        ExecutorService executorService = Executors.newFixedThreadPool(clients.length);

        // Создаем CountDownLatch для ожидания готовности всех потоков
        CountDownLatch readyLatch = new CountDownLatch(clients.length);

        // Создаем CountDownLatch для одновременного запуска всех сделок
        CountDownLatch startLatch = new CountDownLatch(1);

        // Каждый клиент создает случайные заявки на покупку и продажу
        for (Client client : clients) {
            executorService.submit(() -> {
                try {
                    // Генерация случайных заявок
                    List<Runnable> tasks = new ArrayList<>();
                    for (int i = 0; i < 1000; i++) {
                        // Генерация случайной валютной пары
                        Currency baseCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                        Currency quoteCurrency;
                        do {
                            quoteCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                        } while (quoteCurrency == baseCurrency);

                        CurrencyPair pair = new CurrencyPair(baseCurrency, quoteCurrency);
                        long price = 10 + random.nextInt(15); // Случайная цена от 50 до 200
                        long amount = 50 + random.nextInt(100); // Случайное количество от 50 до 150

                        // Добавляем заявку на покупку или продажу
                        if (random.nextBoolean()) {
                            tasks.add(() -> exchange.createBuyOrder(client, pair, price, amount));
                        } else {
                            tasks.add(() -> exchange.createSellOrder(client, pair, price, amount));
                        }
                    }

                    // Поток готов к выполнению сделок
                    readyLatch.countDown();

                    // Ожидаем, пока все потоки будут готовы
                    readyLatch.await();

                    // Ожидаем сигнала на запуск сделок
                    startLatch.await();

                    // Выполняем все сделки с использованием семафора
                    for (Runnable task : tasks) {
                        try {
                            semaphore.acquire(); // Захватываем разрешение на выполнение задачи
                            task.run(); // Выполняем сделку
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            semaphore.release(); // Освобождаем разрешение после выполнения задачи
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        // Ожидаем, пока все потоки будут готовы
        readyLatch.await();

        // Запускаем все сделки одновременно
        startLatch.countDown();

        // Завершаем выполнение пула потоков
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        exchange.shutdown();

        // Вычисляем общее количество денег после сделок для всех валют
        Map<Currency, Long> totalAfter = new HashMap<>();
        for (Currency currency : Currency.values()) {
            totalAfter.put(currency, 0L);
        }

        for (Client client : clients) {
            for (Currency currency : Currency.values()) {
                totalAfter.put(currency, totalAfter.get(currency) +
                        exchange.getClientBalanceManager().getBalance(client, currency));
            }
        }
        long tolerance = 10; // Допустимая погрешность
        // Проверяем, что общее количество денег сохраняется для каждой валюты
        for (Currency currency : Currency.values()) {
            assertEquals(totalBefore.get(currency), totalAfter.get(currency), tolerance,
                    "Total " + currency + " is not conserved");
        }
    }

}
