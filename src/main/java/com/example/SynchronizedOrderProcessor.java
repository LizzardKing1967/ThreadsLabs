package com.example;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.locks.ReentrantLock;

public class SynchronizedOrderProcessor implements EventHandler<OrderEvent> {

    private final RingBuffer<OrderEvent> buyRingBuffer;
    private final RingBuffer<OrderEvent> sellRingBuffer;
    private final ClientBalanceManager balanceManager;

    private final ReentrantLock lock = new ReentrantLock();

    public SynchronizedOrderProcessor(RingBuffer<OrderEvent> buyRingBuffer, RingBuffer<OrderEvent> sellRingBuffer, ClientBalanceManager balanceManager) {
        this.buyRingBuffer = buyRingBuffer;
        this.sellRingBuffer = sellRingBuffer;
        this.balanceManager = balanceManager;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        Order order = event.getOrder();
        if (order == null) return;
        lock.lock();
        try {
            if (order.getType() == OrderType.SELL) {
                processSellOrder(order);
            } else if (order.getType() == OrderType.BUY) {
                processBuyOrder(order);
            }
        } finally {
            lock.unlock();  // Обязательно освобождаем блокировку
        }
    }

    private void processSellOrder(Order sellOrder) {
        // Ищем подходящие ордера на покупку в другом буфере
        for (long i = buyRingBuffer.getCursor(); i >= buyRingBuffer.getMinimumGatingSequence(); i--) {
            OrderEvent buyEvent = buyRingBuffer.get(i);
            if (buyEvent != null && isMatchingOrder(buyEvent.getOrder(), sellOrder)) {
                // Совпадение найдено, выполняем транзакцию
                handleTransaction(buyEvent.getOrder(), sellOrder);
                return;
            }
        }
    }

    private void processBuyOrder(Order buyOrder) {
        // Ищем подходящие ордера на продажу в другом буфере
        for (long i = sellRingBuffer.getCursor(); i >= sellRingBuffer.getMinimumGatingSequence(); i--) {
            OrderEvent sellEvent = sellRingBuffer.get(i);
            if (sellEvent != null && isMatchingOrder(buyOrder, sellEvent.getOrder())) {
                // Совпадение найдено, выполняем транзакцию
                handleTransaction(buyOrder, sellEvent.getOrder());
                return;
            }
        }
    }

    private boolean isMatchingOrder(Order buyOrder, Order sellOrder) {
        return buyOrder != null &&
                sellOrder != null &&
                buyOrder.getCurrencyPair().equals(sellOrder.getCurrencyPair()) &&
                buyOrder.getPrice() >= sellOrder.getPrice() &&  // Покупка по цене >= продажи
                !buyOrder.getClient().equals(sellOrder.getClient()); // Исключаем сделки с самим собой
    }

    private void handleTransaction(Order buyOrder, Order sellOrder) {
        if (buyOrder.getAmount() == 0 || sellOrder.getAmount() == 0) {
            return;
        }

        long tradeAmount = Math.min(buyOrder.getAmount(), sellOrder.getAmount());
        long tradePrice = sellOrder.getPrice();
        long tradeValue = tradeAmount * tradePrice;

        // Проверяем баланс и проводим транзакцию
        if (balanceManager.hasSufficientFunds(buyOrder.getClient(), buyOrder.getCurrencyPair().getQuote(), tradeValue) &&
                (balanceManager.hasSufficientFunds(sellOrder.getClient(), sellOrder.getCurrencyPair().getBase(), tradeAmount))) {
            Transaction transaction = new Transaction(balanceManager);
            transaction.execute(buyOrder, sellOrder, tradeAmount, tradeValue);

            // Уменьшаем объемы заявок
            buyOrder.setAmount(buyOrder.getAmount() - tradeAmount);
            sellOrder.setAmount(sellOrder.getAmount() - tradeAmount);

            updateOrderStatus(buyOrder);
            updateOrderStatus(sellOrder);
        }
    }

    private void updateOrderStatus(Order order) {
        if (order.getAmount() > 0) {
            order.setOrderStatus(OrderStatus.PARTIALCOMPLETED);
        } else {
            order.setOrderStatus(OrderStatus.COMPLETED);
        }
    }
}