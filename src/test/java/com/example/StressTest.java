package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StressTest {
    private Exchange exchange;

    private OrderConsumer orderConsumer;
    private Random random;
    private Client[] clients;
    private ClientBalanceManager balanceManager;

    private final BlockingQueue<Order> buyQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Order> sellQueue = new LinkedBlockingQueue<>();

    @BeforeEach
    public void setUp() {
        balanceManager = new ClientBalanceManager();
        random = new Random();
        int numClients = 100;
        clients = new Client[numClients];

        ClientBalanceManager balanceManager = new ClientBalanceManager();
        ClientBalanceObserver observer = new ClientBalanceObserver();

        balanceManager.addObserver(observer);

        for (int i = 0; i < numClients; i++) {
            clients[i] = new Client("Client" + (i + 1));
            for (Currency currency : Currency.values()) {
                balanceManager.deposit(clients[i], currency,  (50000 + random.nextLong(50000)));
            }
        }

        exchange = new Exchange(balanceManager, buyQueue, sellQueue);
        orderConsumer = new OrderConsumer(buyQueue, sellQueue, balanceManager);
        orderConsumer.start();
    }

    @Test
    public void testTotalMoneyConservationWithTradeGraph() throws InterruptedException, ExecutionException {
        Map<Currency, Long> totalBefore = new HashMap<>();
        for (Currency currency : Currency.values()) {
            totalBefore.put(currency, 0L);
        }

        for (Client client : clients) {
            for (Currency currency : Currency.values()) {
                totalBefore.put(currency, totalBefore.get(currency) +
                        balanceManager.getBalance(client, currency));
            }
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Client client : clients) {

                    for (int i = 0; i < 100; i++) {
                        Currency baseCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                        Currency quoteCurrency;
                        do {
                            quoteCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                        } while (quoteCurrency == baseCurrency);

                        CurrencyPair pair = new CurrencyPair(baseCurrency, quoteCurrency);
                        long price = 10 + random.nextInt(50);
                        long amount = 500 + random.nextInt(1000);
                        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                            try {
                        if (random.nextBoolean()) {
                            exchange.createOrder(new Order(client, OrderType.BUY , pair, price, amount));
                        } else {
                            exchange.createOrder(new Order(client, OrderType.SELL , pair, price, amount));
                        }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        });
                        futures.add(future);
                    }

        }

        // Ожидаем завершения всех асинхронных заявок
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        if (orderConsumer != null){
            orderConsumer.waitForCompletion();
        }


        Map<Currency, Long> totalAfter = new HashMap<>();
        for (Currency currency : Currency.values()) {
            totalAfter.put(currency, 0L);
        }

        for (Client client : clients) {
            for (Currency currency : Currency.values()) {
                totalAfter.put(currency, totalAfter.get(currency) +
                        balanceManager.getBalance(client, currency));
            }
        }

        for (Currency currency : Currency.values()) {
            assertEquals(totalBefore.get(currency), totalAfter.get(currency),
                    "Total " + currency + " is not conserved");
        }
    }
}
