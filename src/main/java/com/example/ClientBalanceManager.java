package com.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientBalanceManager {

    // Храним блокировки для каждого клиента
    private final Map<Client, Lock> clientLocks = new ConcurrentHashMap<>();

    // Храним балансы клиентов
    private final Map<Client, Map<Currency, Long>> clientBalances = new ConcurrentHashMap<>();

    // Создаём состояние клиента
    public void createClientState(Client client) {
        clientBalances.putIfAbsent(client, new ConcurrentHashMap<>());
    }

    // Получаем баланс клиента
    public long getBalance(Client client, Currency currency) {
        return clientBalances
                .getOrDefault(client, Map.of())
                .getOrDefault(currency, 0L);
    }

    // Операция депозита
    public void deposit(Client client, Currency currency, long amount) {
        Lock lock = clientLocks.computeIfAbsent(client, k -> new ReentrantLock());

        lock.lock(); // Блокируем клиента перед операцией
        try {
            clientBalances.computeIfAbsent(client, k -> new ConcurrentHashMap<>())
                    .merge(currency, amount, Long::sum);
        } finally {
            lock.unlock(); // Обязательно освобождаем блокировку
        }
    }

    // Операция снятия средств
    public boolean withdraw(Client client, Currency currency, long amount) {
        Lock lock = clientLocks.computeIfAbsent(client, k -> new ReentrantLock());
        lock.lock(); // Блокируем клиента перед операцией
        try {
            Map<Currency, Long> balances = clientBalances.computeIfAbsent(client, k -> new ConcurrentHashMap<>());
            if (balances.getOrDefault(currency, 0L) < amount) {
                return false; // Недостаточно средств
            }
            balances.merge(currency, -amount, Long::sum);
            return true; // Операция выполнена успешно
        } finally {
            lock.unlock(); // Освобождаем блокировку
        }
    }

    public boolean tryReserveFunds(Client client, Currency currency, long amount) {
        Lock lock = clientLocks.computeIfAbsent(client, k -> new ReentrantLock());
        lock.lock();
        try {
            long currentBalance = clientBalances
                    .computeIfAbsent(client, k -> new ConcurrentHashMap<>())
                    .getOrDefault(currency, 0L);
            if (currentBalance >= amount) {
                clientBalances.get(client).merge(currency, -amount, Long::sum); // Резервируем средства
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    // Метод для отката резервирования
    public boolean hasSufficientFunds(Client client, Currency currency, long amount) {
        return getBalance(client, currency) >= amount;
    }

    public synchronized void rollbackReserve(Client client, Currency currency, long amount) {
        deposit(client, currency, amount); // Возвращаем средства в случае отката
    }

    // Получение всех балансов клиента
    public Map<Currency, Long> getBalances(Client client) {
        Map<Currency, Long> balances = clientBalances.get(client);
        return balances != null ? Map.copyOf(balances) : Map.of();
    }


}