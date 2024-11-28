package com.example.statusNotifiers;

import com.example.entyties.Order;

public interface OrderStatusNotifier {
    void notifyStatusChange(Order order); // Уведомление о изменении статуса
}
