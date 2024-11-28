package com.example.observers;

import com.example.entyties.Client;
import com.example.entyties.Currency;

public interface BalanceObserver {
    void update(Client client, Currency currency, long newBalance, long changeAmount);
}
