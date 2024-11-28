package com.example.statusNotifiers;

import com.example.entyties.Order;

public class EmailOrderStatusNotifier implements OrderStatusNotifier {
    @Override
    public void notifyStatusChange(Order order) {
        // Реализация логики отправки email или уведомлений о статусе ордера
        System.out.println("Notifying client " + order.getClient() + " about status change: " + order.getStatus());
    }
}