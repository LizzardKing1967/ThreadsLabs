package com.example;

import java.util.Map;

public class ClientState {
    private String name;
    private Map<Currency, Double> balances;

    public ClientState(String name, Map<Currency, Double> balances) {
        this.name = name;
        this.balances = balances;
    }

    public String getName() {
        return name;
    }

    public Map<Currency, Double> getBalances() {
        return balances;
    }
}
