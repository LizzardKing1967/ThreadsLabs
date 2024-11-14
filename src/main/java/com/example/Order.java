package com.example;

public class Order {
    private Client client;
    private CurrencyPair pair;
    private double price;
    private double amount;
    private boolean isBuyOrder;

    public Order(Client client, CurrencyPair pair, double price, double amount, boolean isBuyOrder) {
        this.client = client;
        this.pair = pair;
        this.price = price;
        this.amount = amount;
        this.isBuyOrder = isBuyOrder;
    }

    public Client getClient() {
        return client;
    }

    public CurrencyPair getPair() {
        return pair;
    }

    public double getPrice() {
        return price;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isBuyOrder() {
        return isBuyOrder;
    }

    public void decreaseAmount(double amount) {
        this.amount -= amount;
    }
}
