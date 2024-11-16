package com.example;

import java.util.List;
import java.util.concurrent.*;

public class Exchange implements ExchangeInterface {
    private final BlockingQueue<Runnable> orderQueue;
    private final OrderProcessor orderProcessor;
    private final ClientBalanceManager balanceManager = new ClientBalanceManager();
    private final ExecutorService executorService;  // Используем пул потоков

    public Exchange() {
        this.orderQueue = new LinkedBlockingQueue<>();
        this.orderProcessor = new OrderProcessor(orderQueue, balanceManager);
        this.executorService = Executors.newFixedThreadPool(10);  // Пул потоков для обработки
        this.orderProcessor.startProcessing();
    }

    @Override
    public Client createClient(String name) {
        Client client = new Client(name);
        // Убедимся, что все задачи будут выполняться в рамках общего пула потоков
        CompletableFuture.runAsync(() -> orderQueue.add(() -> balanceManager.createClientState(client)), executorService);
        return client;
    }

    @Override
    public CompletableFuture<Void> deposit(Client client, Currency currency, Long amount) {
        return CompletableFuture.runAsync(() -> {
            orderQueue.add(() -> balanceManager.deposit(client, currency, amount));
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> withdraw(Client client, Currency currency, Long amount) {
        return CompletableFuture.runAsync(() -> {
            orderQueue.add(() -> balanceManager.withdraw(client, currency, amount));
        }, executorService);
    }

    @Override
    public CompletableFuture<Order> createBuyOrder(Client client, CurrencyPair pair, Long price, Long amount) {
        CompletableFuture<Order> futureOrder = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            long requiredAmount = price * amount; // Сумма, необходимая для покупки

            boolean isFundsReserved = false;
            try {
                isFundsReserved = balanceManager.tryReserveFunds(client, pair.getQuote(), requiredAmount); // Попытка зарезервировать средства

                if (isFundsReserved) {
                    // Если средства зарезервированы, добавляем заявку в очередь
                    orderQueue.add(() -> {
                        try {
                            Order order = new Order(client, pair, price, amount, OrderType.BUY);
                            orderProcessor.processOrder(order);
                            futureOrder.complete(order);
                        } catch (Exception e) {
                            futureOrder.completeExceptionally(e);
                        }
                    });
                } else {
                    System.out.println("Insufficient funds for client " + client.getName() + " to buy " + amount + " of " + pair.getBase());
                    futureOrder.completeExceptionally(new InsufficientFundsException("Insufficient funds for the purchase"));
                }
            } catch (Exception e) {
                System.err.println("Error reserving funds: " + e.getMessage());
                futureOrder.completeExceptionally(e);
            } finally {
                if (!isFundsReserved) {
                    balanceManager.rollbackReserve(client, pair.getQuote(), requiredAmount); // Откат резервирования, если не получилось
                }
            }
        }, executorService);

        return futureOrder;
    }
    @Override
    public CompletableFuture<Order> createSellOrder(Client client, CurrencyPair pair, Long price, Long amount) {
        CompletableFuture<Order> futureOrder = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            boolean isGoodsReserved = false;
            try {
                isGoodsReserved = balanceManager.tryReserveFunds(client, pair.getBase(), amount); // Попытка зарезервировать товар

                if (isGoodsReserved) {
                    orderQueue.add(() -> {
                        try {
                            Order order = new Order(client, pair, price, amount, OrderType.SELL);
                            orderProcessor.processOrder(order);
                            futureOrder.complete(order);
                        } catch (Exception e) {
                            futureOrder.completeExceptionally(e);
                        }
                    });
                } else {
                    System.out.println("Insufficient goods for client " + client.getName() + " to sell " + amount + " of " + pair.getBase());
                    futureOrder.completeExceptionally(new InsufficientFundsException("Insufficient goods for the sale"));
                }
            } catch (Exception e) {
                System.err.println("Error reserving goods: " + e.getMessage());
                futureOrder.completeExceptionally(e);
            } finally {
                if (!isGoodsReserved) {
                    balanceManager.rollbackReserve(client, pair.getBase(), amount); // Откат резервирования товара
                }
            }
        }, executorService);

        return futureOrder;
    }

    @Override
    public CompletableFuture<List<Order>> getOpenOrders() {
        return CompletableFuture.supplyAsync(() -> orderProcessor.getOpenOrders(), executorService);
    }

    @Override
    public CompletableFuture<ClientState> getClientState(Client client) {
        return CompletableFuture.supplyAsync(() -> orderProcessor.getClientState(client), executorService);
    }

    @Override
    public ClientBalanceManager getClientBalanceManager() {
        return balanceManager;
    }

    public void shutdown() {
        try {
            // Завершаем все задания в очереди
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
