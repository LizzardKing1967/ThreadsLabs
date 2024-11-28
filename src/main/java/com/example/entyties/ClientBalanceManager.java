package com.example.entyties;

import com.example.observers.BalanceObservable;
import com.example.observers.BalanceObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientBalanceManager implements BalanceObservable {
    private final Map<Client, Map<Currency, Long>> clientBalances = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final List<BalanceObserver> observers = new ArrayList<>();

    @Override
    public void addObserver(BalanceObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(BalanceObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Client client, Currency currency, long newBalance, long changeAmount) {
        for (BalanceObserver observer : observers) {
            observer.update(client, currency, newBalance, changeAmount);
        }
    }

    public long getBalance(Client client, Currency currency) {
        rwLock.readLock().lock();
        try {
            Map<Currency, Long> clientBalance = clientBalances.get(client);
            if (clientBalance != null) {
                return clientBalance.getOrDefault(currency, 0L);
            } else {
                return 0L;
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void deposit(Client client, Currency currency, long amount) {
        rwLock.writeLock().lock();
        try {
            long newBalance = clientBalances.computeIfAbsent(client, k -> new ConcurrentHashMap<>())
                    .merge(currency, amount, Long::sum);
            notifyObservers(client, currency, newBalance, amount);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void withdraw(Client client, Currency currency, long amount) {
        rwLock.writeLock().lock();
        try {
            Map<Currency, Long> balances = clientBalances.computeIfAbsent(client, k -> new ConcurrentHashMap<>());
            long currentBalance = balances.getOrDefault(currency, 0L);
            if (currentBalance < amount) {
                return;
            }
            long newBalance = currentBalance - amount;
            balances.put(currency, newBalance);
            notifyObservers(client, currency, newBalance, -amount);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean hasSufficientFunds(Client client, Currency currency, long amount) {
        long balance = getBalance(client, currency);
        return amount <= balance;
    }
}