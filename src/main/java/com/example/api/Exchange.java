package com.example.api;


import com.example.entyties.Order;
import com.example.disruprorEventUtils.OrderEvent;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.Semaphore;

public class Exchange implements ExchangeInterface {
    private final RingBuffer<OrderEvent> writeBuffer;

    private final Semaphore semaphore = new Semaphore(10);

    public Exchange(RingBuffer<OrderEvent> writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    @Override
    public void createOrder(Order order) {
        try {
            // Ожидаем, если семафор занят
            semaphore.acquire();
            try {
                // Публикуем ордер в буфер записи
                long sequence = writeBuffer.next();
                try {
                    OrderEvent event = writeBuffer.get(sequence);
                    event.setOrder(order);
                } finally {
                    writeBuffer.publish(sequence);
                }
            } finally {
                // Освобождаем разрешение для другого потока
                semaphore.release();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating order: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстановление прерванного состояния потока
            System.err.println("Thread was interrupted while waiting for semaphore");
        }
    }
}
