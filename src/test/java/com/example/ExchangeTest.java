package com.example;

import com.example.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ExchangeTest {
    private Exchange exchange;
    private Client client1;
    private Client client2;

    @BeforeEach
    public void setUp() {
        exchange = new Exchange();
        client1 = exchange.createClient("Client1");
        client2 = exchange.createClient("Client2");
    }

    @Test
    public void testCreateClientAndDeposit() {
        assertEquals("Client1", client1.getName());
        assertEquals(0, client1.getBalance(Currency.USD));
        assertEquals(0, client1.getBalance(Currency.EUR));

        exchange.deposit(client1, Currency.USD, 1000);
        assertEquals(1000, client1.getBalance(Currency.USD));

        exchange.deposit(client1, Currency.EUR, 500);
        assertEquals(500, client1.getBalance(Currency.EUR));
    }

    @Test
    public void testCreateBuyAndSellOrder() {
        exchange.deposit(client1, Currency.USD, 1000);
        exchange.deposit(client2, Currency.EUR, 1000);

        Order buyOrder = exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);
        Order sellOrder = exchange.createSellOrder(client2, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);

        assertEquals(0, client1.getBalance(Currency.USD));
        assertEquals(100, client1.getBalance(Currency.EUR));

        assertEquals(0, client2.getBalance(Currency.EUR));
        assertEquals(120, client2.getBalance(Currency.USD));
    }

    @Test
    public void testPartialOrderExecution() {
        exchange.deposit(client1, Currency.USD, 1000);
        exchange.deposit(client2, Currency.EUR, 1000);

        Order buyOrder = exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);
        Order sellOrder = exchange.createSellOrder(client2, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 50);

        assertEquals(400, client1.getBalance(Currency.USD));
        assertEquals(50, client1.getBalance(Currency.EUR));

        assertEquals(950, client2.getBalance(Currency.EUR));
        assertEquals(60, client2.getBalance(Currency.USD));
    }

    @Test
    public void testInsufficientFunds() {
        exchange.deposit(client1, Currency.USD, 1000);

        assertThrows(IllegalArgumentException.class, () -> {
            exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 1000);
        });
    }

    @Test
    public void testMismatchCurrencyPairs() {
        exchange.deposit(client1, Currency.USD, 1000);
        exchange.deposit(client2, Currency.EUR, 1000);

        Order buyOrder = exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);
        Order sellOrder = exchange.createSellOrder(client2, new CurrencyPair(Currency.GBP, Currency.USD), 1.2, 100);

        assertEquals(1000, client1.getBalance(Currency.USD));
        assertEquals(1000, client2.getBalance(Currency.EUR));
    }

    @Test
    public void testConcurrentDepositAndWithdraw() throws InterruptedException {
        exchange.deposit(client1, Currency.USD, 1000);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.submit(() -> {
            for (int i = 0; i < 1000; i++) {
                exchange.deposit(client1, Currency.USD, 1);
            }
        });

        executorService.submit(() -> {
            for (int i = 0; i < 1000; i++) {
                exchange.withdraw(client1, Currency.USD, 1);
            }
        });

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        assertEquals(1000, client1.getBalance(Currency.USD));
    }

    @Test
    public void testStressTotalMoneyConservation() throws InterruptedException {
        Random random = new Random();
        int numClients = 1000;
        Client[] clients = new Client[numClients];

        // Создаем клиентов и пополняем их балансы
        for (int i = 0; i < numClients; i++) {
            clients[i] = exchange.createClient("Client" + (i + 1));
            exchange.deposit(clients[i], Currency.USD, 10000 + random.nextInt(5000));
            exchange.deposit(clients[i], Currency.EUR, 5000 + random.nextInt(5000));
        }

        // Вычисляем общее количество денег до сделок
        double totalUSDBefore = 0;
        double totalEURBefore = 0;
        for (Client client : clients) {
            totalUSDBefore += client.getBalance(Currency.USD);
            totalEURBefore += client.getBalance(Currency.EUR);
        }

        // Создаем пул потоков
        ExecutorService executorService = Executors.newFixedThreadPool(numClients);

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