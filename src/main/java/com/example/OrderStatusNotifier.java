package com.example;

public interface OrderStatusNotifier {
    void notifyStatusChange(Order order); // Уведомление о изменении статуса
}
