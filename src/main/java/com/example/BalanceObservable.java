package com.example;

public interface BalanceObservable {
    void addObserver(BalanceObserver observer);
    void removeObserver(BalanceObserver observer);
    void notifyObservers(Client client, Currency currency, long newBalance, long change);
}
