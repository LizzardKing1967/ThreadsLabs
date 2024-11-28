package com.example;

import com.lmax.disruptor.EventFactory;

public class OrderEvent {
    private Order order;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public void copyFrom(OrderEvent source) {
        this.order = source.order;
    }

    public static final EventFactory<OrderEvent> EVENT_FACTORY = OrderEvent::new;
}