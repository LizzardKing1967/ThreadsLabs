package com.example;


import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

public class OrderStatusProcessor implements EventHandler<OrderStatusEvent> {
    private final OrderStatusNotifier statusNotifier;

    public OrderStatusProcessor(OrderStatusNotifier statusNotifier) {
        this.statusNotifier = statusNotifier;
    }

    @Override
    public void onEvent(OrderStatusEvent event, long sequence, boolean endOfBatch) {
        Order order = event.getOrder();

        // Логика для обработки каждого статуса
        if (order != null) {
            // Уведомляем клиента о статусе ордера
            statusNotifier.notifyStatusChange(order);

        }
    }
}