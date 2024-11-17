package com.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientBalanceManager {
    private final Map<Client, Map<Currency, Long>> clientBalances = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void createClientState(Client client) {
        clientBalances.putIfAbsent(client, new ConcurrentHashMap<>());
    }

    public long getBalance(Client client, Currency currency) {
        rwLock.readLock().lock();
        try {
            Currency currency1 = Currency.valueOf(String.valueOf(currency));
            // Проверяем, есть ли клиент и есть ли баланс по конкретной валюте
            Map<Currency, Long> clientBalance = clientBalances.get(client);
            if (clientBalance != null) {
                long balance = clientBalance.getOrDefault(currency1, 0L);
                return balance;
            } else {
                // Если клиента нет, возвращаем 0 по умолчанию
                return 0L;
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void deposit(Client client, Currency currency, long amount) {
        rwLock.writeLock().lock();
        try {
            clientBalances.computeIfAbsent(client, k -> new ConcurrentHashMap<>())
                    .merge(currency, amount, Long::sum);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void withdraw(Client client, Currency currency, long amount) {
        rwLock.writeLock().lock();
        try {
            Map<Currency, Long> balances = clientBalances.computeIfAbsent(client, k -> new ConcurrentHashMap<>());
            if (balances.getOrDefault(currency, 0L) < amount) {
                return;
            }
            balances.merge(currency, -amount, Long::sum);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean hasSufficientFunds(Client client, Currency currency, long amount) {
        long balance = getBalance(client, currency);
        return amount < balance;
    }

}