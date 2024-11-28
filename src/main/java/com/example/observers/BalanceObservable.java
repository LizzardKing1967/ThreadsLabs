package com.example.observers;

import com.example.entyties.Client;
import com.example.entyties.Currency;

public interface BalanceObservable {
    void addObserver(BalanceObserver observer);
    void removeObserver(BalanceObserver observer);
    void notifyObservers(Client client, Currency currency, long newBalance, long change);
}
