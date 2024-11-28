package com.example;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.locks.ReentrantLock;

public class OrderBufferProcessor implements EventHandler<OrderEvent> {
    private final RingBuffer<OrderEvent> orderRingBuffer;
    private final ClientBalanceManager balanceManager;
    private final ReentrantLock lock = new ReentrantLock();

    private final RingBuffer<OrderStatusEvent> statusRingBuffer;

    public OrderBufferProcessor(RingBuffer<OrderEvent> orderRingBuffer, RingBuffer<OrderStatusEvent> statusRingBuffer, ClientBalanceManager balanceManager) {
        this.orderRingBuffer = orderRingBuffer;
        this.balanceManager = balanceManager;
        this.statusRingBuffer = statusRingBuffer;

    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        Order order = event.getOrder();
        if (order == null || order.getStatus() == OrderStatus.COMPLETED) {
            return; // Skip completed orders
        }

        boolean orderProcessed = false;

        // Проверка баланса клиента перед обработкой ордера
        if (!hasSufficientFunds(order)) {
            order.setOrderStatus(OrderStatus.CANCELLED); // Устанавливаем статус CANCELLED
            publishOrderStatus(order);
            return; // Ордер не обрабатываем, так как средств недостаточно
        }

        if (order.getType() == OrderType.SELL) {
            orderProcessed = processSellOrder(order);
        } else if (order.getType() == OrderType.BUY) {
            orderProcessed = processBuyOrder(order);
        }

        // Если заявка не была обработана, она становится CANCELLED
        if (!orderProcessed) {
            order.setOrderStatus(OrderStatus.CANCELLED);
        }
    }

    private void publishOrderStatus(Order order) {
        // Публикуем статус в статусный буфер
        long sequence = statusRingBuffer.next();
        try {
            OrderStatusEvent statusEvent = statusRingBuffer.get(sequence);
            statusEvent.setOrder(order);
        } finally {
            statusRingBuffer.publish(sequence);
        }
    }
    private boolean hasSufficientFunds(Order order) {
        long tradeAmount = order.getAmount();
        long tradePrice = order.getPrice();
        long tradeValue = tradeAmount * tradePrice;

        // Для покупки проверяем наличие средств на счете клиента
        if (order.getType() == OrderType.BUY) {
            return balanceManager.hasSufficientFunds(order.getClient(), order.getCurrencyPair().getQuote(), tradeValue);
        }
        // Для продажи проверяем наличие достаточного количества базовой валюты на счете
        else if (order.getType() == OrderType.SELL) {
            return balanceManager.hasSufficientFunds(order.getClient(), order.getCurrencyPair().getBase(), tradeAmount);
        }
        return false;
    }

    private boolean processSellOrder(Order sellOrder) {
        for (long i = orderRingBuffer.getCursor(); i >= orderRingBuffer.getMinimumGatingSequence(); i--) {
            OrderEvent buyEvent = orderRingBuffer.get(i);
            if (buyEvent != null && isMatchingOrder(buyEvent.getOrder(), sellOrder)) {
                handleTransaction(buyEvent.getOrder(), sellOrder);
                return true; // Заявка была обработана
            }
        }
        return false; // Нет подходящего контрагента
    }

    private boolean processBuyOrder(Order buyOrder) {
        for (long i = orderRingBuffer.getCursor(); i >= orderRingBuffer.getMinimumGatingSequence(); i--) {
            OrderEvent sellEvent = orderRingBuffer.get(i);
            if (sellEvent != null && isMatchingOrder(buyOrder, sellEvent.getOrder())) {
                handleTransaction(buyOrder, sellEvent.getOrder());
                return true; // Заявка была обработана
            }
        }
        return false; // Нет подходящего контрагента
    }

    private boolean isMatchingOrder(Order buyOrder, Order sellOrder) {
        return buyOrder != null && sellOrder != null &&
                buyOrder.getCurrencyPair().equals(sellOrder.getCurrencyPair()) &&
                buyOrder.getPrice() >= sellOrder.getPrice() &&
                !buyOrder.getClient().equals(sellOrder.getClient()) &&
                buyOrder.getAmount() > 0 && sellOrder.getAmount() > 0;
    }

    private void handleTransaction(Order buyOrder, Order sellOrder) {
        if (buyOrder.getAmount() == 0 || sellOrder.getAmount() == 0) {
            return;
        }

        long tradeAmount = Math.min(buyOrder.getAmount(), sellOrder.getAmount());
        long tradePrice = sellOrder.getPrice();
        long tradeValue = tradeAmount * tradePrice;

        lock.lock();
        try {
            if (balanceManager.hasSufficientFunds(buyOrder.getClient(), buyOrder.getCurrencyPair().getQuote(), tradeValue) &&
                    balanceManager.hasSufficientFunds(sellOrder.getClient(), sellOrder.getCurrencyPair().getBase(), tradeAmount)) {
                Transaction transaction = new Transaction(balanceManager);
                transaction.execute(buyOrder, sellOrder, tradeAmount, tradeValue);

                buyOrder.setAmount(buyOrder.getAmount() - tradeAmount);
                sellOrder.setAmount(sellOrder.getAmount() - tradeAmount);

                updateOrderStatus(buyOrder);
                updateOrderStatus(sellOrder);
            }
        } finally {
            lock.unlock();
        }
    }

    private void updateOrderStatus(Order order) {
        if (order.getAmount() > 0) {
            order.setOrderStatus(OrderStatus.PARTIALCOMPLETED);
        } else {
            order.setOrderStatus(OrderStatus.COMPLETED);
        }
        publishOrderStatus(order);
    }
}
