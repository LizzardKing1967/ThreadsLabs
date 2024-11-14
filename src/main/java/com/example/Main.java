package com.example;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Exchange exchange = new Exchange();
        Random random = new Random();

        // Создаем 50 клиентов
        Client[] clients = new Client[50];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = exchange.createClient("Client" + (i + 1));
            // Пополняем балансы клиентов случайными суммами
            exchange.deposit(clients[i], Currency.USD, 10000 + random.nextInt(5000));
            exchange.deposit(clients[i], Currency.EUR, 5000 + random.nextInt(5000));
        }

        // Создаем пул потоков
        ExecutorService executorService = Executors.newFixedThreadPool(50);

        // Каждый клиент создает случайные заявки на покупку и продажу
        for (Client client : clients) {
            executorService.submit(() -> {
                for (int i = 0; i < 10; i++) {
                    try {
                        CurrencyPair pair = new CurrencyPair(Currency.EUR, Currency.USD);
                        double price = 1.2 + random.nextDouble() * 0.1; // Случайная цена от 1.2 до 1.3
                        double amount = 50 + random.nextInt(100); // Случайное количество от 50 до 150

                        if (random.nextBoolean()) {
                            Order order = exchange.createBuyOrder(client, pair, price, amount);
                            System.out.println(client.getName() + " created buy order: " + order);
                        } else {
                            Order order = exchange.createSellOrder(client, pair, price, amount);
                            System.out.println(client.getName() + " created sell order: " + order);
                        }

                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // Завершаем выполнение потоков
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // Выводим состояние клиентов после всех сделок
        for (Client client : clients) {
            System.out.println(client.getName() + " state: " + exchange.getClientState(client));
        }

        // Выводим открытые заявки
        System.out.println("Open orders: " + exchange.getOpenOrders());
    }
}