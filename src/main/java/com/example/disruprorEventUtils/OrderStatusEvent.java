package com.example.disruprorEventUtils;

import com.example.entyties.Order;

public class OrderStatusEvent {
    private Order order; // Ордер, для которого обновляется статус

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
