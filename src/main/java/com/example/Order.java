package com.example;

import java.math.BigDecimal;

public class Order {
    private final Client client;
    private final CurrencyPair pair;
    private final Long price;
    private Long amount;
    private final OrderType type;
    private boolean isFulfilled;

    public Order(Client client, CurrencyPair pair, Long price, Long amount, OrderType type) {
        this.client = client;
        this.pair = pair;
        this.price = price;
        this.amount = amount;
        this.type = type;
        this.isFulfilled = false;
    }

    public Client getClient() {
        return client;
    }

    public void setFulfilled(boolean fulfilled) {
        isFulfilled = fulfilled;
    }

    public CurrencyPair getPair() {
        return pair;
    }

    public Long getPrice() {
        return price;
    }

    public Long getAmount() {
        return amount;
    }

    public OrderType getType() {
        return type;
    }

    public boolean isFulfilled() {
        return isFulfilled;
    }

    @Override
    public String toString() {
        return "Order{" +
                "client=" + client.getName() +
                ", pair=" + pair.getBase() + "/" + pair.getQuote() +
                ", price=" + price +
                ", amount=" + amount +
                ", type=" + type +
                '}';
    }

    public synchronized void decreaseAmount(long tradeAmount) {
        this.amount-=tradeAmount;
        if (this.amount<=0) {
            this.isFulfilled = true;
        }
    }
}