package com.example;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StressTest {
    private Exchange exchange;

    private Random random;
    private Client[] clients;
    private ClientBalanceManager balanceManager;

    ExecutorService executor = Executors.newCachedThreadPool();
    ThreadFactory threadFactory;

    Disruptor<OrderEvent> sellDisruptor;
    Disruptor<OrderEvent> buyDisruptor;
    int numClients;


    @BeforeEach
    public void setUp() {
        int bufferSize = 1024; // Размер кольцевого буфера
        threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        };

        // Создаём Disruptor для BUY ордеров
        buyDisruptor = new Disruptor<>(OrderEvent.EVENT_FACTORY, bufferSize, threadFactory, ProducerType.MULTI, new BlockingWaitStrategy());

        // Создаём Disruptor для SELL ордеров
        sellDisruptor = new Disruptor<>(OrderEvent.EVENT_FACTORY, bufferSize, threadFactory, ProducerType.MULTI, new BlockingWaitStrategy());

        random = new Random();
        numClients = 100;
        clients = new Client[numClients];

        // Менеджер балансов и наблюдатель
        balanceManager = new ClientBalanceManager();
        ClientBalanceObserver observer = new ClientBalanceObserver();

        // Добавляем наблюдателя
        balanceManager.addObserver(observer);

        // Инициализация клиентов и их депозитов
        for (int i = 0; i < numClients; i++) {
            clients[i] = new Client("Client" + (i + 1));  // Создаем нового клиента
            for (Currency currency : Currency.values()) {  // Для каждой валюты
                long depositAmount = 50000 + random.nextLong(50000);  // Случайная сумма от 50000 до 100000
                balanceManager.deposit(clients[i], currency, depositAmount);  // Депозит
            }
        }

        // Настроим обработчик для SELL ордеров с синхронизацией с BUY ордерами
        SynchronizedOrderProcessor processor = new SynchronizedOrderProcessor(
                buyDisruptor.getRingBuffer(), sellDisruptor.getRingBuffer(), balanceManager);

        buyDisruptor.handleEventsWith(processor);
        sellDisruptor.handleEventsWith(processor);

        // Запускаем Disruptor
        buyDisruptor.start();
        sellDisruptor.start();

        // Создаём биржу
        exchange = new Exchange(balanceManager, buyDisruptor.getRingBuffer(), sellDisruptor.getRingBuffer());
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

        List<CompletableFuture<Order>> futures = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            Client client = clients[i];

            // Для каждого клиента создаём 100 асинхронных ордеров
            for (int j = 0; j < 1000; j++) {
                CompletableFuture<Order> future = CompletableFuture.supplyAsync(() -> {
                    Currency baseCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                    Currency quoteCurrency;
                    do {
                        quoteCurrency = Currency.values()[random.nextInt(Currency.values().length)];
                    } while (quoteCurrency == baseCurrency);

                    CurrencyPair pair = new CurrencyPair(baseCurrency, quoteCurrency);
                    long price = 10 + random.nextInt(50);
                    long amount = 500 + random.nextInt(100);
                    OrderType type = random.nextBoolean() ? OrderType.BUY : OrderType.SELL;
                    Order order = new Order(client, type, pair, price, amount, OrderStatus.PROCESSING);

                    exchange.createOrder(order);  // Отправляем ордер в биржу
                    return order;
                }, executor);

                // Добавляем CompletableFuture в список
                futures.add(future);
            }
        }

        // Ожидаем завершения всех задач
        for (CompletableFuture<Order> future : futures) {
            try {
                Order order = future.join();  // Ожидаем завершения задачи
            } catch (Exception e) {
                // Обрабатываем ошибки
            }
        }

        // Останавливаем Disruptor при завершении
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            buyDisruptor.shutdown();
            sellDisruptor.shutdown();
            executor.shutdown();
        }));


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
