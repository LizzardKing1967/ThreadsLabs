package com.example;

import java.util.Currency;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Exchange implements ExchangeInterface {
    private Map<String, Client> clients = new ConcurrentHashMap<>();
    private Map<CurrencyPair, Queue<Order>> buyOrders = new ConcurrentHashMap<>();
    private Map<CurrencyPair, Queue<Order>> sellOrders = new ConcurrentHashMap<>();

    @Override
    public synchronized Client createClient(String name) {
        if (clients.containsKey(name)) {
            throw new IllegalArgumentException("Client already exists");
        }
        Client client = new Client(name);
        clients.put(name, client);
        return client;
    }

    @Override
    public synchronized void deposit(Client client, com.example.Currency currency, double amount) {
        client.deposit(currency, amount);
    }

    @Override
    public synchronized void withdraw(Client client, com.example.Currency currency, double amount) {
        client.withdraw(currency, amount);
    }

    @Override
    public synchronized Order createBuyOrder(Client client, CurrencyPair pair, double price, double amount) {
        if (client.getBalance(pair.getQuote()) < amount * price) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        Order order = new Order(client, pair, price, amount, true);
        matchOrders(order);
        if (order.getAmount() > 0) {
            buyOrders.computeIfAbsent(pair, k -> new ConcurrentLinkedQueue<>()).add(order);
        }
        return order;
    }

    @Override
    public synchronized Order createSellOrder(Client client, CurrencyPair pair, double price, double amount) {
        if (client.getBalance(pair.getBase()) < amount) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        Order order = new Order(client, pair, price, amount, false);
        matchOrders(order);
        if (order.getAmount() > 0) {
            sellOrders.computeIfAbsent(pair, k -> new ConcurrentLinkedQueue<>()).add(order);
        }
        return order;
    }

    private void matchOrders(Order order) {
        Queue<Order> oppositeOrders = order.isBuyOrder() ? sellOrders.get(order.getPair()) : buyOrders.get(order.getPair());
        if (oppositeOrders == null) return;

        Iterator<Order> iterator = oppositeOrders.iterator();
        while (iterator.hasNext() && order.getAmount() > 0) {
            Order oppositeOrder = iterator.next();

            // Определение условий для выполнения сделки
            boolean canExecuteTrade = (order.isBuyOrder() && order.getPrice() >= oppositeOrder.getPrice()) ||
                    (!order.isBuyOrder() && order.getPrice() <= oppositeOrder.getPrice());

            if (canExecuteTrade) {
                double tradeAmount = Math.min(order.getAmount(), oppositeOrder.getAmount());

                // Передача покупателя и продавца в правильном порядке
                if (order.isBuyOrder()) {
                    executeTrade(order, oppositeOrder, tradeAmount); // order - покупатель
                } else {
                    executeTrade(oppositeOrder, order, tradeAmount); // oppositeOrder - покупатель
                }

                if (oppositeOrder.getAmount() == 0) {
                    iterator.remove();
                }
            }
        }
    }

    private void executeTrade(Order buyOrder, Order sellOrder, double amount) {
        double tradePrice = buyOrder.getPrice(); // Используем цену ордера покупателя для сделки

        // Проверка наличия достаточных средств у клиентов
        if (buyOrder.getClient().getBalance(buyOrder.getPair().getQuote()) < amount * tradePrice) {
            throw new IllegalArgumentException("Insufficient funds for buyer");
        }
        if (sellOrder.getClient().getBalance(sellOrder.getPair().getBase()) < amount) {
            throw new IllegalArgumentException("Insufficient funds for seller");
        }

        // Выполнение перевода: покупатель платит в котируемой валюте, продавец получает в базовой
        buyOrder.getClient().withdraw(buyOrder.getPair().getQuote(), amount * tradePrice);
        sellOrder.getClient().withdraw(sellOrder.getPair().getBase(), amount);

        // Покупатель получает базовую валюту, продавец получает котируемую валюту
        buyOrder.getClient().deposit(buyOrder.getPair().getBase(), amount);
        sellOrder.getClient().deposit(sellOrder.getPair().getQuote(), amount * tradePrice);

        // Обновление оставшихся объемов ордеров
        buyOrder.decreaseAmount(amount);
        sellOrder.decreaseAmount(amount);

        // Логирование сделки
        System.out.println("Trade executed: " + amount + " " + buyOrder.getPair().getBase() +
                " at price " + tradePrice + " " + buyOrder.getPair().getQuote() +
                " between " + buyOrder.getClient().getName() + " and " + sellOrder.getClient().getName());
    }

    @Override
    public synchronized List<Order> getOpenOrders() {
        List<Order> openOrders = new ArrayList<>();
        buyOrders.values().forEach(openOrders::addAll);
        sellOrders.values().forEach(openOrders::addAll);
        return openOrders;
    }

    @Override
    public synchronized ClientState getClientState(Client client) {
        return new ClientState(client.getName(), new HashMap<>(client.getBalances()));
    }
}