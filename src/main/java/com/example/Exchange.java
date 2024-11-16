package com.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Exchange implements ExchangeInterface {
    private Map<String, Client> clients = new ConcurrentHashMap<>();
    private Map<CurrencyPair, List<Order>> buyOrders = new ConcurrentHashMap<>();
    private Map<CurrencyPair, List<Order>> sellOrders = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public Client createClient(String name) {
        lock.lock();
        try {
            if (clients.containsKey(name)) {
                throw new IllegalArgumentException("Client already exists");
            }
            Client client = new Client(name);
            clients.put(name, client);
            return client;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating client: " + e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deposit(Client client, com.example.Currency currency, double amount) {
        lock.lock();
        try {
            client.deposit(currency, amount);
        } catch (Exception e) {
            System.err.println("Error depositing funds: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void withdraw(Client client, com.example.Currency currency, double amount) {
        lock.lock();
        try {
            client.withdraw(currency, amount);
        } catch (Exception e) {
            System.err.println("Error withdrawing funds: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Order createBuyOrder(Client client, CurrencyPair pair, double price, double amount) {
        lock.lock();
        try {
            if (!CurrencyPairValidator.isValidPair(pair)) {
                throw new IllegalArgumentException("Invalid currency pair: " + pair);
            }
            if (client.getBalance(pair.getQuote()) < amount * price) {
                throw new IllegalArgumentException("Insufficient funds");
            }
            Order order = new Order(client, pair, price, amount, true);
            matchOrders(order);
            if (order.getAmount() > 0) {
                buyOrders.computeIfAbsent(pair, k -> new ArrayList<>()).add(order);
            }
            return order;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating buy order: " + e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Order createSellOrder(Client client, CurrencyPair pair, double price, double amount) {
        lock.lock();
        try {
            if (!CurrencyPairValidator.isValidPair(pair)) {
                throw new IllegalArgumentException("Invalid currency pair: " + pair);
            }
            if (client.getBalance(pair.getBase()) < amount) {
                throw new IllegalArgumentException("Insufficient funds");
            }
            Order order = new Order(client, pair, price, amount, false);
            matchOrders(order);
            if (order.getAmount() > 0) {
                sellOrders.computeIfAbsent(pair, k -> new ArrayList<>()).add(order);
            }
            return order;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating sell order: " + e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private void matchOrders(Order order) {
        lock.lock();
        try {
            List<Order> oppositeOrders = order.isBuyOrder() ? sellOrders.get(order.getPair()) : buyOrders.get(order.getPair());
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
        } catch (Exception e) {
            System.err.println("Error matching orders: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void executeTrade(Order buyOrder, Order sellOrder, double amount) {
        lock.lock();
        try {
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
        } catch (IllegalArgumentException e) {
            System.err.println("Error executing trade: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Order> getOpenOrders() {
        lock.lock();
        try {
            List<Order> openOrders = new ArrayList<>();
            buyOrders.values().forEach(openOrders::addAll);
            sellOrders.values().forEach(openOrders::addAll);
            return openOrders;
        } catch (Exception e) {
            System.err.println("Error getting open orders: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ClientState getClientState(Client client) {
        lock.lock();
        try {
            return new ClientState(client.getName(), new HashMap<>(client.getBalances()));
        } catch (Exception e) {
            System.err.println("Error getting client state: " + e.getMessage());
            return null;
        } finally {
            lock.unlock();
        }
    }
}