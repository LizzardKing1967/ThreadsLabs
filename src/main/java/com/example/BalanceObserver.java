package com.example;

public interface BalanceObserver {
    void update(Client client, Currency currency, long newBalance, long changeAmount);
}
