package com.example;

import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.BlockingQueue;

public class OrderConsumer {
    private final RingBuffer<OrderEvent> ringBuffer;

    public OrderConsumer(RingBuffer<OrderEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void publishOrder(Order order) {
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setOrder(order);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}

