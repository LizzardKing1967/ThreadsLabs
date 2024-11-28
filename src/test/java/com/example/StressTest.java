package com.example;

import com.example.api.Exchange;
import com.example.disruprorEventUtils.OrderEvent;
import com.example.disruprorEventUtils.OrderStatusEvent;
import com.example.disruprorEventUtils.WriteToProcessHandler;
import com.example.entyties.*;
import com.example.entyties.Currency;
import com.example.observers.ClientBalanceObserver;
import com.example.orderProcessors.OrderBufferProcessor;
import com.example.orderProcessors.OrderStatusProcessor;
import com.example.statusNotifiers.EmailOrderStatusNotifier;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
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

    Disruptor<OrderEvent> orderDisruptor;  // Первый Disruptor для записи ордеров
    Disruptor<OrderEvent> processingDisruptor; // Второй Disruptor для обработки ордеров

    Disruptor<OrderStatusEvent> statusDisruptor;
    int numClients;

    @BeforeEach
    public void setUp() {
        int bufferSize = 1024; // Размер кольцевого буфера
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setPriority(Thread.MAX_PRIORITY); // Можно использовать MIN_PRIORITY или MAX_PRIORITY
                return thread;
            }
        };
        // Создаем два Disruptor'а для записи и обработки ордеров
        random = new Random();
        numClients = 1000;
        clients = new Client[numClients];

        // Менеджер балансов и наблюдатель
        balanceManager = new ClientBalanceManager();
        ClientBalanceObserver observer = new ClientBalanceObserver();

        // Добавляем наблюдателя
        balanceManager.addObserver(observer);

        // Инициализация клиентов и их депозитов
        for (int i = 0; i < numClients; i++) {
            clients[i] = new Client("Client" + (i + 1));  // Создаем нового клиента
            for (com.example.entyties.Currency currency : com.example.entyties.Currency.values()) {  // Для каждой валюты
                long depositAmount = 500 + random.nextLong(50000);  // Случайная сумма от 50000 до 100000
                balanceManager.deposit(clients[i], currency, depositAmount);  // Депозит
            }
        }

        orderDisruptor = new Disruptor<>(OrderEvent.EVENT_FACTORY, bufferSize, threadFactory, ProducerType.MULTI, new BusySpinWaitStrategy());
        processingDisruptor = new Disruptor<>(OrderEvent.EVENT_FACTORY, bufferSize, threadFactory, ProducerType.SINGLE, new BlockingWaitStrategy());
        statusDisruptor = new Disruptor<>(OrderStatusEvent::new, bufferSize, threadFactory, ProducerType.SINGLE, new BlockingWaitStrategy());


        // Создаем обработчик передачи данных
        WriteToProcessHandler writeHandler = new WriteToProcessHandler(processingDisruptor.getRingBuffer());

        // Привязываем обработчик записи к orderDisruptor
        orderDisruptor.handleEventsWith(writeHandler);

        // Привязываем обработчик обработки к processingDisruptor
        OrderBufferProcessor processor = new OrderBufferProcessor(
                processingDisruptor.getRingBuffer(),statusDisruptor.getRingBuffer(), balanceManager);
        processingDisruptor.handleEventsWith(processor);

        OrderStatusProcessor statusProcessor = new OrderStatusProcessor(new EmailOrderStatusNotifier());
        statusDisruptor.handleEventsWith(statusProcessor);


        // Запускаем оба Disruptor
        orderDisruptor.start();
        processingDisruptor.start();
        statusDisruptor.start();
        exchange = new Exchange(orderDisruptor.getRingBuffer());
    }

    @Test
    public void testTotalMoneyConservationWithTradeGraph() throws InterruptedException, ExecutionException {
        Map<com.example.entyties.Currency, Long> totalBefore = new HashMap<>();
        for (com.example.entyties.Currency currency : com.example.entyties.Currency.values()) {
            totalBefore.put(currency, 0L);
        }

        for (Client client : clients) {
            for (com.example.entyties.Currency currency : com.example.entyties.Currency.values()) {
                totalBefore.put(currency, totalBefore.get(currency) +
                        balanceManager.getBalance(client, currency));
            }
        }

        List<CompletableFuture<Order>> futures = new ArrayList<>();

        for (int i = 0; i < numClients; i++) {
            Client client = clients[i];

            // Для каждого клиента создаём 500 асинхронных ордеров
            for (int j = 0; j < 1000; j++) {
                CompletableFuture<Order> future = CompletableFuture.supplyAsync(() -> {
                    com.example.entyties.Currency baseCurrency = com.example.entyties.Currency.values()[random.nextInt(com.example.entyties.Currency.values().length)];
                    com.example.entyties.Currency quoteCurrency;
                    do {
                        quoteCurrency = com.example.entyties.Currency.values()[random.nextInt(com.example.entyties.Currency.values().length)];
                    } while (quoteCurrency == baseCurrency);

                    CurrencyPair pair = new CurrencyPair(baseCurrency, quoteCurrency);
                    long price = 50 + random.nextInt(500);
                    long amount = random.nextInt(1000);
                    OrderType type = random.nextBoolean() ? OrderType.BUY : OrderType.SELL;
                    Order order = new Order(client, type, pair, price, amount, OrderStatus.PROCESSING);

                    exchange.createOrder(order);  // Отправляем ордер в биржу
                    return order;
                }, executor);

                // Добавляем CompletableFuture в список
                futures.add(future);
            }
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();  // Это блокирует выполнение до завершения всех задач

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            orderDisruptor.shutdown();
            processingDisruptor.shutdown();
            executor.shutdown();
        }));

        Map<com.example.entyties.Currency, Long> totalAfter = new HashMap<>();
        for (com.example.entyties.Currency currency : com.example.entyties.Currency.values()) {
            totalAfter.put(currency, 0L);
        }

        for (Client client : clients) {
            for (com.example.entyties.Currency currency : com.example.entyties.Currency.values()) {
                totalAfter.put(currency, totalAfter.get(currency) +
                        balanceManager.getBalance(client, currency));
            }
        }

        // Проверяем сохранение баланса для каждой валюты
        for (com.example.entyties.Currency currency : Currency.values()) {
            assertEquals(totalBefore.get(currency), totalAfter.get(currency),
                    "Total " + currency + " is not conserved");
        }
    }

}
