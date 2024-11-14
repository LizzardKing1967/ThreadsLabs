package com.example;

import java.util.HashMap;
import java.util.Map;

public class Client {
    private String name;
    private Map<Currency, Double> balances;

    public Client(String name) {
        this.name = name;
        this.balances = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public Map<Currency, Double> getBalances() {
        return this.balances;
    }

    public double getBalance(Currency currency) {
        return balances.getOrDefault(currency, 0.0);
    }

    public void deposit(Currency currency, double amount) {
        balances.put(currency, balances.getOrDefault(currency, 0.0) + amount);
    }

    public void withdraw(Currency currency, double amount) {
        double currentBalance = balances.getOrDefault(currency, 0.0);
        if (currentBalance < amount) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        balances.put(currency, currentBalance - amount);
    }
}