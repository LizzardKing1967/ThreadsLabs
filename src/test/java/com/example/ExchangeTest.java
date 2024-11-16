package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ExchangeTest {
    private Exchange exchange;
    private Client client1;
    private Client client2;

    private Client client3;

    @BeforeEach
    public void setUp() {
        exchange = new Exchange();
        client1 = exchange.createClient("Client1");
        client2 = exchange.createClient("Client2");
        client3 = exchange.createClient("Client3");
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
        exchange.deposit(client1, Currency.USD, 120);
        exchange.deposit(client2, Currency.EUR, 100);

        Order buyOrder = exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);
        Order sellOrder = exchange.createSellOrder(client2, new CurrencyPair(Currency.EUR, Currency.USD), 1, 100);

        assertEquals(0, client1.getBalance(Currency.USD));
        assertEquals(100, client1.getBalance(Currency.EUR));

        assertEquals(0, client2.getBalance(Currency.EUR));
        assertEquals(120, client2.getBalance(Currency.USD));
    }

    @Test
    public void testPartialOrderExecution() {
        // Пополняем балансы клиентов
        exchange.deposit(client1, Currency.USD, 120);
        exchange.deposit(client2, Currency.EUR, 120);


        // Создаем заявку на покупку 100 EUR за USD
        Order buyOrder = exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);
        // Создаем заявку на продажу 50 EUR за USD (пара валют исправлена на EUR/USD)
        Order sellOrder = exchange.createSellOrder(client2, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 50);

        // Выполняем сделки

        // Проверяем балансы клиентов после выполнения частичных сделок
        assertEquals(60, client1.getBalance(Currency.USD)); // 120 - 50 * 1.2
        assertEquals(50, client1.getBalance(Currency.EUR)); // 0 + 50
    }

    @Test
    public void testInsufficientFunds() {
        exchange.deposit(client1, Currency.USD, 1000);

        assertThrows(IllegalArgumentException.class, () -> {
            exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 1000);
        });
    }

    @Test
    public void testPartialDoubleSell() {
        exchange.deposit(client1, Currency.USD, 1000);
        exchange.deposit(client2, Currency.EUR, 1000);
        exchange.deposit(client3, Currency.USD, 1000);

        Order buyOrder = exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 30);
        Order sellOrder = exchange.createSellOrder(client2, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);
        Order buyOrder1 = exchange.createBuyOrder(client3, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 70);
        assertEquals(964, client1.getBalance(Currency.USD));
        assertEquals(900, client2.getBalance(Currency.EUR));
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
    public void testPartialBuyAndListOpenOrders() {
        Client buyer = exchange.createClient("Buyer");
        Client seller1 = exchange.createClient("Seller1");
        Client seller2 = exchange.createClient("Seller2");

        // Пополняем их балансы
        exchange.deposit(buyer, Currency.USD, 200);  // Покупатель может купить до 200 USD
        exchange.deposit(seller1, Currency.EUR, 60); // Продавец 1 имеет 60 EUR
        exchange.deposit(seller2, Currency.EUR, 100); // Продавец 2 имеет 100 EUR

        CurrencyPair pair = new CurrencyPair(Currency.EUR, Currency.USD);

        // Продавец 1 создает ордер на продажу 60 EUR по цене 1.2 USD за EUR
        exchange.createSellOrder(seller1, pair, 1.2, 60);

        // Продавец 2 создает ордер на продажу 100 EUR по цене 1.2 USD за EUR
        exchange.createSellOrder(seller2, pair, 1.2, 100);

        // Покупатель создает ордер на покупку 100 EUR по цене 1.2 USD за EUR
        exchange.createBuyOrder(buyer, pair, 1.2, 100);

        // Проверяем результат частичного выполнения:
        // - Покупатель купит 60 EUR у продавца 1 (ордер выполнен полностью)
        // - Покупатель купит 40 EUR у продавца 2 (ордер частично выполнен, остается 60 EUR)

        // Проверка балансов
        assertEquals(100, buyer.getBalance(Currency.EUR), 0.01);   // Покупатель получил 100 EUR
        assertEquals(200 - (100 * 1.2), buyer.getBalance(Currency.USD), 0.01); // У покупателя осталось 100 USD
        assertEquals(0, seller1.getBalance(Currency.EUR), 0.01); // Продавец 1 продал все 60 EUR
        assertEquals(60, seller2.getBalance(Currency.EUR), 0.01); // У продавца 2 осталось 60 EUR

        // Вывод открытых заявок
        List<Order> openOrders = exchange.getOpenOrders();

        // Проверка открытых заявок
        assertEquals(1, openOrders.size(), "Expected one open order after partial execution.");
        Order remainingOrder = openOrders.get(0);
        assertEquals(seller2, remainingOrder.getClient(), "Remaining order should belong to Seller2.");
        assertEquals(60, remainingOrder.getAmount(), 0.01, "Remaining order should have 60 EUR.");
        assertEquals(1.2, remainingOrder.getPrice(), 0.01, "Remaining order price should be 1.2 USD/EUR.");
    }
}