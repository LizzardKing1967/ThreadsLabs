package com.example.orderProcessors;


import com.example.disruprorEventUtils.OrderStatusEvent;
import com.example.entyties.Order;
import com.example.statusNotifiers.OrderStatusNotifier;
import com.lmax.disruptor.EventHandler;

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