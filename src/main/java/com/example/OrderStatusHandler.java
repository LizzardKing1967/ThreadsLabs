package com.example;

import com.lmax.disruptor.RingBuffer;

public class OrderStatusHandler {

    private final RingBuffer<OrderStatusEvent> statusRingBuffer;

    public OrderStatusHandler(RingBuffer<OrderStatusEvent> statusRingBuffer) {
        this.statusRingBuffer = statusRingBuffer;
    }

    public void processOrderStatus(Order order) {
        // Публикуем событие в буфер статусов
        long sequence = statusRingBuffer.next();
        try {
            OrderStatusEvent statusEvent = statusRingBuffer.get(sequence);
            statusEvent.setOrder(order);
        } finally {
            statusRingBuffer.publish(sequence); // Публикуем статус
        }
    }
}
