package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

//public class ExchangeTest {
//    private Exchange exchange;
//    private ClientBalanceManager balanceManager;
//    private Client client1;
//    private Client client2;
//    private Client client3;
//
//    @BeforeEach
//    public void setUp() {
//        balanceManager = new ClientBalanceManager();
//        exchange = new Exchange(balanceManager);
//
//        client1 = exchange.createClient("Client1");
//        client2 = exchange.createClient("Client2");
//        client3 = exchange.createClient("Client3");
//    }
//
//    @Test
//    public void testCreateClientAndDeposit() {
//        assertEquals("Client1", client1.getName());
//        assertEquals(0, balanceManager.getBalance(client1, Currency.USD));
//        assertEquals(0, balanceManager.getBalance(client1, Currency.EUR));
//
//        balanceManager.deposit(client1, Currency.USD, 1000);
//        assertEquals(1000, balanceManager.getBalance(client1, Currency.USD));
//
//        balanceManager.deposit(client1, Currency.EUR, 500);
//        assertEquals(500, balanceManager.getBalance(client1, Currency.EUR));
//    }
//
//    @Test
//    public void testCreateBuyAndSellOrder() {
//        balanceManager.deposit(client1, Currency.USD, 120);
//        balanceManager.deposit(client2, Currency.EUR, 100);
//
//        Order buyOrder = exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);
//        Order sellOrder = exchange.createSellOrder(client2, new CurrencyPair(Currency.EUR, Currency.USD), 1, 100);
//
//        assertEquals(0, balanceManager.getBalance(client1, Currency.USD));
//        assertEquals(100, balanceManager.getBalance(client1, Currency.EUR));
//
//        assertEquals(0, balanceManager.getBalance(client2, Currency.EUR));
//        assertEquals(120, balanceManager.getBalance(client2, Currency.USD));
//    }
//
//    @Test
//    public void testPartialOrderExecution() {
//        balanceManager.deposit(client1, Currency.USD, 120);
//        balanceManager.deposit(client2, Currency.EUR, 120);
//
//        Order buyOrder = exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);
//        Order sellOrder = exchange.createSellOrder(client2, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 50);
//
//        assertEquals(60, balanceManager.getBalance(client1, Currency.USD));
//        assertEquals(50, balanceManager.getBalance(client1, Currency.EUR));
//    }
//
//    @Test
//    public void testInsufficientFunds() {
//        balanceManager.deposit(client1, Currency.USD, 1000);
//
//        assertThrows(IllegalArgumentException.class, () -> {
//            exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 1000);
//        });
//    }
//
//    @Test
//    public void testPartialDoubleSell() {
//        balanceManager.deposit(client1, Currency.USD, 1000);
//        balanceManager.deposit(client2, Currency.EUR, 1000);
//        balanceManager.deposit(client3, Currency.USD, 1000);
//
//        Order buyOrder = exchange.createBuyOrder(client1, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 30);
//        Order sellOrder = exchange.createSellOrder(client2, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 100);
//        Order buyOrder1 = exchange.createBuyOrder(client3, new CurrencyPair(Currency.EUR, Currency.USD), 1.2, 70);
//
//        assertEquals(964, balanceManager.getBalance(client1, Currency.USD));
//        assertEquals(900, balanceManager.getBalance(client2, Currency.EUR));
//    }
//
//    @Test
//    public void testConcurrentDepositAndWithdraw() throws InterruptedException {
//        balanceManager.deposit(client1, Currency.USD, 1000);
//
//        ExecutorService executorService = Executors.newFixedThreadPool(2);
//
//        executorService.submit(() -> {
//            for (int i = 0; i < 1000; i++) {
//                balanceManager.deposit(client1, Currency.USD, 1);
//            }
//        });
//
//        executorService.submit(() -> {
//            for (int i = 0; i < 1000; i++) {
//                balanceManager.withdraw(client1, Currency.USD, 1);
//            }
//        });
//
//        executorService.shutdown();
//        executorService.awaitTermination(1, TimeUnit.MINUTES);
//
//        assertEquals(1000, balanceManager.getBalance(client1, Currency.USD));
//    }
//
//    @Test
//    public void testPartialBuyAndListOpenOrders() {
//        Client buyer = exchange.createClient("Buyer");
//        Client seller1 = exchange.createClient("Seller1");
//        Client seller2 = exchange.createClient("Seller2");
//
//        balanceManager.deposit(buyer, Currency.USD, 200);
//        balanceManager.deposit(seller1, Currency.EUR, 60);
//        balanceManager.deposit(seller2, Currency.EUR, 100);
//
//        CurrencyPair pair = new CurrencyPair(Currency.EUR, Currency.USD);
//
//        exchange.createSellOrder(seller1, pair, 1.2, 60);
//        exchange.createSellOrder(seller2, pair, 1.2, 100);
//        exchange.createBuyOrder(buyer, pair, 1.2, 100);
//
//        assertEquals(100, balanceManager.getBalance(buyer, Currency.EUR), 0.01);
//        assertEquals(200 - (100 * 1.2), balanceManager.getBalance(buyer, Currency.USD), 0.01);
//        assertEquals(0, balanceManager.getBalance(seller1, Currency.EUR), 0.01);
//        assertEquals(60, balanceManager.getBalance(seller2, Currency.EUR), 0.01);
//
//        List<Order> openOrders = exchange.getOpenOrders();
//
//        assertEquals(1, openOrders.size());
//        Order remainingOrder = openOrders.get(0);
//        assertEquals(seller2, remainingOrder.getClient());
//        assertEquals(60, remainingOrder.getAmount(), 0.01);
//        assertEquals(1.2, remainingOrder.getPrice(), 0.01);
//    }
//}
