package com.example;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Exchange implements ExchangeInterface {
    private final RingBuffer<OrderEvent> buyRingBuffer;
    private final RingBuffer<OrderEvent> sellRingBuffer;
    private final ClientBalanceManager balanceManager;

    public Exchange(ClientBalanceManager balanceManager, RingBuffer<OrderEvent> buyRingBuffer, RingBuffer<OrderEvent> sellRingBuffer) {
        this.balanceManager = balanceManager;
        this.buyRingBuffer = buyRingBuffer;
        this.sellRingBuffer = sellRingBuffer;
    }

    @Override
    public void createOrder(Order order) {
        RingBuffer<OrderEvent> targetBuffer = order.getType() == OrderType.BUY ? buyRingBuffer : sellRingBuffer;

        try {
            if (order.getType() == OrderType.BUY &&
                    !balanceManager.hasSufficientFunds(order.getClient(), order.getCurrencyPair().getQuote(), order.getTotalValue())) {
                throw new IllegalArgumentException("Insufficient funds for buy order.");
            } else if (order.getType() == OrderType.SELL &&
                    !balanceManager.hasSufficientFunds(order.getClient(), order.getCurrencyPair().getBase(), order.getAmount())) {
                throw new IllegalArgumentException("Insufficient balance for sell order.");
            }

            long sequence = targetBuffer.next(); // Получаем свободную ячейку
            try {
                OrderEvent event = targetBuffer.get(sequence);
                event.setOrder(order); // Записываем данные
            } finally {
                targetBuffer.publish(sequence); // Публикуем событие
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating order: " + e.getMessage());
        }
    }
}
