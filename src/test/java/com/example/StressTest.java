package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StressTest {
    private Exchange exchange;
    private Random random;
    private Client[] clients;

    // Коллекция для отслеживания времени и цены сделок USD/EUR (покупка EUR за USD)
    private List<Double> usdEurBuyPrices = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    public void setUp() {
        exchange = new Exchange();
        random = new Random();
        int numClients = 1000;
        clients = new Client[numClients];

        // Создаем клиентов и пополняем их балансы для всех валют
        for (int i = 0; i < numClients; i++) {
            clients[i] = exchange.createClient("Client" + (i + 1));
            for (Currency currency : Currency.values()) {
                exchange.deposit(clients[i], currency, 5000 + random.nextInt(5000)); // Баланс от 5000 до 10000 для каждой валюты
            }
        }
    }

    @Test
    public void testTotalMoneyConservationWithTradeGraph() throws InterruptedException {
        // Вычисляем общее количество денег до сделок для всех валют
        Map<Currency, Double> totalBefore = new HashMap<>();
        for (Currency currency : Currency.values()) {
            totalBefore.put(currency, 0.0);
        }

        for (Client client : clients) {
            for (Currency currency : Currency.values()) {
                totalBefore.put(currency, totalBefore.get(currency) + client.getBalance(currency));
            }
        }

        // Создаем пул потоков
        ExecutorService executorService = Executors.newFixedThreadPool(clients.length);

        // Создаем CountDownLatch для ожидания готовности всех потоков
        CountDownLatch readyLatch = new CountDownLatch(clients.length);

        // Создаем CountDownLatch для одновременного запуска всех сделок
        CountDownLatch startLatch = new CountDownLatch(1);

        // Каждый клиент создает случайные заявки на покупку и продажу для случайных пар валют
        for (Client client : clients) {
            executorService.submit(() -> {
                try {
                    // Генерация случайных заявок
                    List<Runnable> tasks = new ArrayList<>();
                    for (int i = 0; i < 100; i++) {
                        // Генерация случайной валютной пары
                        Currency baseCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                        Currency quoteCurrency;
                        do {
                            quoteCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                        } while (quoteCurrency == baseCurrency);

                        CurrencyPair pair = new CurrencyPair(baseCurrency, quoteCurrency);
                        double price = 0.5 + random.nextDouble() * 1.5; // Случайная цена от 0.5 до 2.0
                        double amount = 50 + random.nextInt(100); // Случайное количество от 50 до 150

                        // Логирование цены сделок USD/EUR
                        if (pair.equals(new CurrencyPair(Currency.USD, Currency.EUR)) && random.nextBoolean()) {
                            tasks.add(() -> {
                                exchange.createBuyOrder(client, pair, price, amount);
                                usdEurBuyPrices.add(price); // Логируем цену сделки для пары USD/EUR (покупка EUR за USD)
                            });
                        } else {
                            // Добавляем заявку на покупку или продажу
                            if (random.nextBoolean()) {
                                tasks.add(() -> exchange.createBuyOrder(client, pair, price, amount));
                            } else {
                                tasks.add(() -> exchange.createSellOrder(client, pair, price, amount));
                            }
                        }
                    }

                    // Поток готов к выполнению сделок
                    readyLatch.countDown();

                    // Ожидаем, пока все потоки будут готовы
                    readyLatch.await();

                    // Ожидаем сигнала на запуск сделок
                    startLatch.await();

                    // Выполняем все сделки
                    for (Runnable task : tasks) {
                        task.run();
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

        // Вычисляем общее количество денег после сделок для всех валют
        Map<Currency, Double> totalAfter = new HashMap<>();
        for (Currency currency : Currency.values()) {
            totalAfter.put(currency, 0.0);
        }

        for (Client client : clients) {
            for (Currency currency : Currency.values()) {
                totalAfter.put(currency, totalAfter.get(currency) + client.getBalance(currency));
            }
        }

        // Проверяем, что общее количество денег сошлось для каждой валюты
        for (Currency currency : Currency.values()) {
            assertEquals(totalBefore.get(currency), totalAfter.get(currency), 0.001,
                    "Total " + currency + " is not conserved");
        }

        // Выводим график средней цены сделок USD/EUR (покупка EUR за USD) в консоли
        printUsdEurTradeGraph();
    }

    private void printUsdEurTradeGraph() {
        System.out.println("\nUSD/EUR Trade Price (Buying EUR with USD) Graph:");
        DecimalFormat df = new DecimalFormat("#.##");

        // Определяем диапазон значений для шкалы графика
        double minPrice = Collections.min(usdEurBuyPrices);
        double maxPrice = Collections.max(usdEurBuyPrices);
        int scaleHeight = 10; // Количество уровней для вертикальной шкалы

        for (int level = scaleHeight; level >= 0; level--) {
            double priceLevel = minPrice + (maxPrice - minPrice) * level / scaleHeight;
            System.out.printf("%7s |", df.format(priceLevel));

            // Рисуем график по каждому значению
            for (double price : usdEurBuyPrices) {
                if (price >= priceLevel) {
                    System.out.print(" * ");
                } else {
                    System.out.print("   ");
                }
            }
            System.out.println();
        }

        // Нижняя ось времени
        System.out.print("        ");
        for (int i = 0; i < usdEurBuyPrices.size(); i++) {
            if (i % 5 == 0) System.out.print(String.format("%-3d", i)); // Метки по времени с шагом
            else System.out.print("   ");
        }
        System.out.println("\n");
    }
}
