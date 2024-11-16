package com.example;

import java.util.Map;

public class ClientState {
    private String name;
    private Map<Currency, Long> balances;

    public ClientState(String name, Map<Currency, Long> balances) {
        this.name = name;
        this.balances = balances;
    }

    public String getName() {
        return name;
    }

    public Map<Currency, Long> getBalances() {
        return balances;
    }
}
