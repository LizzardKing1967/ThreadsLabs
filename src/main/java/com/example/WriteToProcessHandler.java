package com.example;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

public class WriteToProcessHandler implements EventHandler<OrderEvent> {
    private final RingBuffer<OrderEvent> processingBuffer;

    public WriteToProcessHandler(RingBuffer<OrderEvent> processingBuffer) {
        this.processingBuffer = processingBuffer;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        long nextSequence = processingBuffer.next(); // Получаем следующую позицию в буфере обработки
        try {
            OrderEvent processingEvent = processingBuffer.get(nextSequence);
            processingEvent.copyFrom(event); // Копируем данные ордера в новый буфер
        } finally {
            processingBuffer.publish(nextSequence); // Публикуем событие в буфере обработки
        }
    }
}

