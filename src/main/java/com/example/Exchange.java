package com.example;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Exchange implements ExchangeInterface {
    private final ClientBalanceManager balanceManager;
    private final BlockingQueue<Order> buyQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Order> sellQueue = new LinkedBlockingQueue<>();
    private final OrderConsumer orderConsumer;

    public Exchange(ClientBalanceManager balanceManager) {
        this.balanceManager = balanceManager;
        this.orderConsumer = new OrderConsumer(buyQueue, sellQueue, balanceManager);
        this.orderConsumer.start();
    }
    @Override
    public void createOrder(Order order) {
        try {
            // Валидация средств через ClientBalanceManager
            if (order.getType() == OrderType.BUY) {
                if (!balanceManager.hasSufficientFunds(order.getClient(), order.getCurrencyPair().getQuote(), order.getTotalValue())) {
                    throw new IllegalArgumentException("Insufficient funds for buy order.");
                }
                buyQueue.add(order);
            } else if (order.getType() == OrderType.SELL) {
                if (!balanceManager.hasSufficientFunds(order.getClient(), order.getCurrencyPair().getBase(), order.getAmount())) {
                    throw new IllegalArgumentException("Insufficient balance for sell order.");
                }
                sellQueue.add(order);
            }
        } catch (IllegalArgumentException e) {
            // Выводим сообщение об ошибке в консоль
            System.err.println("Error creating order: " + e.getMessage());
        }
    }

    public void stopExchange() {
        orderConsumer.stopProcessing();
    }

    public void waitForCompletion() throws InterruptedException {
        orderConsumer.join();  // Ожидаем завершения потока
    }

    public BlockingQueue<Order> getBuyQueue() {
        return this.buyQueue; // Поле должно возвращаться напрямую
    }

    public BlockingQueue<Order> getSellQueue() {
        return this.buyQueue;
    }
}
