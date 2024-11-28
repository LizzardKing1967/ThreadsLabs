package com.example.entyties;

public class Order {
    private final Client client;
    private final OrderType type;
    private final CurrencyPair currencyPair;
    private volatile long price;
    private long amount;

    private OrderStatus status;

    public Order(Client client, OrderType type, CurrencyPair currencyPair, long price, long amount, OrderStatus status) {
        this.client = client;
        this.type = type;
        this.currencyPair = currencyPair;
        this.price = price;
        this.amount = amount;
        this.status = status;
    }

    public Client getClient() {
        return client;
    }

    public OrderType getType() {
        return type;
    }

    public OrderStatus getStatus() {return status;}

    public CurrencyPair getCurrencyPair() {
        return currencyPair;
    }

    public synchronized long getPrice() {
        return this.price;
    }

    public long getAmount() {
        return amount;
    }

    public long getTotalValue() {
        return price * amount;
    }

    public void setAmount(long l) {
        this.amount = l;
    }

    public void setOrderStatus(OrderStatus status) {
        this.status = status;
    }
}
