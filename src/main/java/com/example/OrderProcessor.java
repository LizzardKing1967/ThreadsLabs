package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OrderProcessor {
    private final BlockingQueue<Runnable> orderQueue;
    private final Thread processingThread;
    private final CopyOnWriteArrayList<Order> openOrders = new CopyOnWriteArrayList<>();  // Используем CopyOnWriteArrayList
    private final Map<Client, ClientState> clientStates = new ConcurrentHashMap<>();
    private final ClientBalanceManager balanceManager;
    private final Lock orderLock = new ReentrantLock();

    public OrderProcessor(BlockingQueue<Runnable> orderQueue, ClientBalanceManager balanceManager) {
        this.orderQueue = orderQueue;
        this.balanceManager = balanceManager;
        this.processingThread = new Thread(this::processOrders);
    }

    public void startProcessing() {
        processingThread.start();
    }

    private void processOrders() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Runnable task = orderQueue.take();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void processOrder(Order order) {
        orderLock.lock();
        try {
            openOrders.add(order);
            matchOrders(order);
        } finally {
            orderLock.unlock();
        }
    }

    private void matchOrders(Order newOrder) {
        for (Order existingOrder : openOrders) {
            if (canMatch(newOrder, existingOrder) && !newOrder.getClient().equals(existingOrder.getClient())) {
                executeTrade(newOrder, existingOrder);
                if (newOrder.isFulfilled()) {
                    openOrders.remove(newOrder); // Работа с CopyOnWriteArrayList безопасна
                    break;
                }
                if (existingOrder.isFulfilled()) {
                    openOrders.remove(existingOrder);
                }
            }
        }
    }

    private boolean canMatch(Order order1, Order order2) {
        return order1.getPair().equals(order2.getPair()) &&
                ((order1.getType() == OrderType.BUY && order1.getPrice() >= order2.getPrice()) ||
                        (order1.getType() == OrderType.SELL && order1.getPrice() <= order2.getPrice()));
    }

    private void executeTrade(Order buyOrder, Order sellOrder) {
        long tradeAmount = Math.min(buyOrder.getAmount(), sellOrder.getAmount());
        long tradePrice = buyOrder.getPrice();

        if (buyOrder.isFulfilled() || sellOrder.isFulfilled()) {
            return;
        }

        orderLock.lock();
        try {
            boolean tradeSuccess = updateBalance(buyOrder.getClient(), sellOrder.getClient(), buyOrder.getPair(), tradeAmount, tradePrice);
            if (tradeSuccess) {
                logTrade(buyOrder, sellOrder, tradeAmount, tradePrice);
                buyOrder.decreaseAmount(tradeAmount);
                sellOrder.decreaseAmount(tradeAmount);
            }
        } finally {
            orderLock.unlock();
        }
    }
    private final Lock balanceLock = new ReentrantLock();

    private boolean updateBalance(Client buyer, Client seller, CurrencyPair pair, long tradeAmount, long tradePrice) {
        Currency baseCurrency = pair.getBase();
        Currency quoteCurrency = pair.getQuote();

        balanceLock.lock();
        try {
            // Проверяем доступные средства у сторон
            long buyerRequiredFunds = tradeAmount * tradePrice;
            if (!balanceManager.hasSufficientFunds(buyer, quoteCurrency, buyerRequiredFunds)) {
                System.err.println("Buyer has insufficient funds.");
                return false;
            }
            if (!balanceManager.hasSufficientFunds(seller, baseCurrency, tradeAmount)) {
                System.err.println("Seller has insufficient goods.");
                return false;
            }

            // Выполняем транзакцию
            balanceManager.withdraw(buyer, quoteCurrency, buyerRequiredFunds);
            balanceManager.deposit(buyer, baseCurrency, tradeAmount);

            balanceManager.withdraw(seller, baseCurrency, tradeAmount);
            balanceManager.deposit(seller, quoteCurrency, buyerRequiredFunds);

            return true;
        } catch (Exception e) {
            System.err.println("Failed to execute trade: " + e.getMessage());
            return false;
        } finally {
            balanceLock.unlock();
        }
    }

    private void logTrade(Order buyOrder, Order sellOrder, long tradeAmount, long tradePrice) {
        String message = String.format(
                "Trade executed: Buyer=%s, Seller=%s, Pair=%s, Amount=%d, Price=%d",
                buyOrder.getClient().getName(),
                sellOrder.getClient().getName(),
                buyOrder.getPair(),
                tradeAmount,
                tradePrice
        );
        System.out.println(message);
    }



    public List<Order> getOpenOrders() {
        return new ArrayList<>(openOrders);
    }

    public ClientState getClientState(Client client) {
        return clientStates.getOrDefault(client, new ClientState(client.getName(), Map.of()));
    }
}