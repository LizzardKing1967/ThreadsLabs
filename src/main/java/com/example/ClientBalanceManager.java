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
    public void withdraw(Client client, Currency currency, long amount) {
        Lock lock = clientLocks.computeIfAbsent(client, k -> new ReentrantLock());

        lock.lock(); // Блокируем клиента перед операцией
        try {
            clientBalances.computeIfAbsent(client, k -> new ConcurrentHashMap<>())
                    .compute(currency, (key, current) -> {
                        if (current == null || current < amount) {
                            throw new IllegalArgumentException("Insufficient funds");
                        }
                        return current - amount;
                    });
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage() + " for client " + client.getName() + " and currency " + currency);
        } finally {
            lock.unlock(); // Обязательно освобождаем блокировку
        }
    }

    // Получение всех балансов клиента
    public Map<Currency, Long> getBalances(Client client) {
        Map<Currency, Long> balances = clientBalances.get(client);
        return balances != null ? Map.copyOf(balances) : Map.of();
    }
}