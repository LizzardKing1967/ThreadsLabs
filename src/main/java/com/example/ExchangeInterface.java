package com.example;

import java.util.List;

public interface ExchangeInterface {
    Client createClient(String name);
    void deposit(Client client, Currency currency, double amount);
    void withdraw(Client client, Currency currency, double amount);

    Order createBuyOrder(Client client, CurrencyPair pair, double price, double amount);
    Order createSellOrder(Client client, CurrencyPair pair, double price, double amount);
    List<Order> getOpenOrders();
    ClientState getClientState(Client client);
}