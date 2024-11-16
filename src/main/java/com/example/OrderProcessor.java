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
        for (Order existingOrder : openOrders) {  // Для CopyOnWriteArrayList можно работать без синхронизации
            if (canMatch(newOrder, existingOrder)) {
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
            // Логирование до обновления баланса
            System.out.println(String.format("Executing trade: Buyer=%s, Seller=%s, Amount=%d, Price=%d",
                    buyOrder.getClient().getName(),
                    sellOrder.getClient().getName(),
                    tradeAmount,
                    tradePrice));

            // Обновление баланса участников
            updateBalance(buyOrder.getClient(), sellOrder.getClient(), buyOrder.getPair(), tradeAmount, tradePrice);

            // Уменьшение объемов ордеров
            buyOrder.decreaseAmount(tradeAmount);
            sellOrder.decreaseAmount(tradeAmount);

            // Проверка выполнения ордеров
        } finally {
            orderLock.unlock();
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

    private final Lock balanceLock = new ReentrantLock();

    private void updateBalance(Client buyer, Client seller, CurrencyPair pair, long tradeAmount, long tradePrice) {
        Currency baseCurrency = pair.getBase();
        Currency quoteCurrency = pair.getQuote();

        // Синхронизируем операции снятия и внесения средств с помощью блокировки
        balanceLock.lock();
        try {
            // Сначала снимаем средства с покупателя
            long buyerWithdrawAmount = tradeAmount * tradePrice;
            balanceManager.withdraw(buyer, quoteCurrency, buyerWithdrawAmount); // Снимаем средства у покупателя
            balanceManager.deposit(buyer, baseCurrency, tradeAmount); // Вносим средства покупателю

            // Теперь снимаем средства с продавца
            balanceManager.withdraw(seller, baseCurrency, tradeAmount); // Снимаем средства у продавца
            balanceManager.deposit(seller, quoteCurrency, tradeAmount * tradePrice); // Вносим средства продавцу

            // Логирование после успешного выполнения транзакции
            System.out.println(String.format("Balance updated: Buyer=%s, Seller=%s, Base=%s, Quote=%s, Amount=%d, Price=%d",
                    buyer.getName(), seller.getName(), baseCurrency, quoteCurrency, tradeAmount, tradePrice));
        } catch (IllegalArgumentException e) {
            // Обработка ошибок снятия средств (например, недостаточно средств)
            System.err.println("Failed to update balances: " + e.getMessage());
        } finally {
            balanceLock.unlock(); // Всегда снимаем блокировку в блоке finally
        }
    }

    public List<Order> getOpenOrders() {
        return new ArrayList<>(openOrders);
    }

    public ClientState getClientState(Client client) {
        return clientStates.getOrDefault(client, new ClientState(client.getName(), Map.of()));
    }
}