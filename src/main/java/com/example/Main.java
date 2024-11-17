package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Random random = new Random();
    private static final int NUM_CLIENTS = 100; // Количество клиентов
    private static final int ORDERS_PER_CLIENT = 100; // Количество ордеров на одного клиента

    public static void main(String[] args) throws InterruptedException {
        // Инициализация Exchange и BalanceManager
        ClientBalanceManager balanceManager = new ClientBalanceManager();


        // Список клиентов
        List<Client> clients = new ArrayList<>();
        for (int i = 0; i < NUM_CLIENTS; i++) {
            clients.add(new Client("Client" + (i + 1)));
            for (Currency currency : Currency.values()) {
                balanceManager.deposit(clients.get(i), currency,  (5000 + random.nextLong(5000)));
            }
        }
        Exchange exchange = new Exchange(balanceManager);
        // Создаем пул потоков с ограничением на 10 потоков одновременно
        ExecutorService executorService = Executors.newFixedThreadPool(10); // 10 потоков

        // Создаем CountDownLatch для ожидания готовности всех потоков
        CountDownLatch readyLatch = new CountDownLatch(clients.size());

        // Каждый клиент создает случайные заявки на покупку и продажу
        for (Client client : clients) {
            executorService.submit(() -> {
                try {
                    // Генерация случайных заявок
                    for (int i = 0; i < ORDERS_PER_CLIENT; i++) {
                        // Генерация случайной валютной пары
                        Currency baseCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                        Currency quoteCurrency;
                        do {
                            quoteCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                        } while (quoteCurrency == baseCurrency);

                        CurrencyPair pair = new CurrencyPair(baseCurrency, quoteCurrency);
                        long price = 10 + random.nextInt(15); // Случайная цена от 50 до 200
                        long amount = 50 + random.nextInt(10); // Случайное количество от 50 до 150

                        // Добавляем заявку на покупку или продажу
                        if (random.nextBoolean()) {
                            exchange.createOrder(new Order(client, OrderType.BUY, pair, price, amount));
                        } else {
                            exchange.createOrder(new Order(client, OrderType.SELL, pair, price, amount));
                        }
                    }

                    // Поток готов к выполнению сделок
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Ожидаем, пока все потоки будут готовы

        // Начинаем стресс-тест, после того как все заявки созданы
        System.out.println("Stress test started...");

        // Ожидаем, пока все сделки будут обработаны (например, с паузой 5 секунд)
        // Завершаем выполнение пула потоков
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        System.out.println("Stress test completed.");
    }
}
