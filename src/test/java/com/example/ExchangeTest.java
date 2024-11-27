package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ExchangeTest {
    private Exchange exchange;
    private ClientBalanceManager balanceManager;

    private final BlockingQueue<Order> buyQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Order> sellQueue = new LinkedBlockingQueue<>();

    private OrderConsumer orderConsumer;
    private Client client1;
    private Client client2;

    @BeforeEach
    public void setUp() {
        balanceManager = new ClientBalanceManager();


        client1 = new Client("Client1");
        client2 = new Client("Client2");

        balanceManager.deposit(client1, Currency.USD, 10000);
        balanceManager.deposit(client2, Currency.EUR, 5000);
        exchange = new Exchange(balanceManager, buyQueue, sellQueue);
        orderConsumer = new OrderConsumer(buyQueue, sellQueue, balanceManager);
        orderConsumer.start();
    }

    @Test
    public void testCreateBuyOrderWithSufficientFunds() throws InterruptedException {
        Order buyOrder = new Order(client1, OrderType.BUY, new CurrencyPair(Currency.EUR, Currency.USD), 2, 100, OrderStatus.PROCESSING);

        exchange.createOrder(buyOrder);

        // Проверяем, что ордер добавлен в очередь
        BlockingQueue<Order> buyQueue = exchange.getBuyQueue();
        assertFalse(buyQueue.isEmpty());
        assertEquals(buyOrder, exchange.getBuyQueue().peek());
    }

    @Test
    public void testCreateSellOrderWithInsufficientFunds() {
        Order sellOrder = new Order(client1, OrderType.SELL, new CurrencyPair(Currency.EUR, Currency.USD), 1, 600, OrderStatus.PROCESSING);

        exchange.createOrder(sellOrder);

        // Ордер не должен быть добавлен в очередь из-за недостатка средств
        assertTrue(exchange.getSellQueue().isEmpty());
    }

    @Test
    public void testConcurrentOrderProcessing() throws InterruptedException {
        Order buyOrder = new Order(client1, OrderType.BUY, new CurrencyPair(Currency.EUR, Currency.USD), 2, 100 , OrderStatus.PROCESSING);
        Order sellOrder = new Order(client2, OrderType.SELL, new CurrencyPair(Currency.EUR, Currency.USD), 2, 100, OrderStatus.PROCESSING);

        exchange.createOrder(buyOrder);
        exchange.createOrder(sellOrder);

        // Ждем завершения обработки

        orderConsumer.waitForCompletion();

        // Проверяем, что ордера были обработаны
        assertEquals(0, exchange.getBuyQueue().size());
        assertEquals(0, exchange.getSellQueue().size());

        // Проверяем корректность балансов
        assertEquals(9800, balanceManager.getBalance(client1, Currency.USD)); // 1000 - 100 * 2
        assertEquals(100, balanceManager.getBalance(client1, Currency.EUR)); // Куплено 100 EUR
        assertEquals(4900, balanceManager.getBalance(client2, Currency.EUR)); // 500 + 100
        assertEquals(200, balanceManager.getBalance(client2, Currency.USD)); // Продано за 120 USD
    }
}
