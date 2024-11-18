package com.example;

public class ClientBalanceObserver implements BalanceObserver {
    @Override
    public void update(Client client, Currency currency, long newBalance, long changeAmount) {
        String changeType = changeAmount > 0 ? "deposited" : "withdrawn";
        System.out.println("Balance updated for client: " + client.getName() + ", Currency: " + currency +
                ", New Balance: " + newBalance + ", Amount " + changeType + ": " + Math.abs(changeAmount));
    }
}