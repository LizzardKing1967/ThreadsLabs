package com.example;

import java.util.concurrent.BlockingQueue;

public class OrderConsumer extends Thread {
    private final BlockingQueue<Order> buyQueue;
    private final BlockingQueue<Order> sellQueue;
    private final ClientBalanceManager balanceManager;
    private volatile boolean running = true;
    private long lastOrderTime; // Время последней обработки заявки

    public OrderConsumer(BlockingQueue<Order> buyQueue, BlockingQueue<Order> sellQueue, ClientBalanceManager balanceManager) {
        this.buyQueue = buyQueue;
        this.sellQueue = sellQueue;
        this.balanceManager = balanceManager;
        this.lastOrderTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (running) {
            try {
                long currentTime = System.currentTimeMillis();

                // Проверяем, если с последней обработки заявки прошло больше 100 мс, завершаем поток
                if (currentTime - lastOrderTime > 100) {
                    stopProcessing();
                    break;
                }

                // Если одна из очередей пуста, засыпаем
                if (buyQueue.isEmpty() || sellQueue.isEmpty()) {
                    Thread.sleep(10);
                    continue;
                }

                Order buyOrder = buyQueue.take();
                Order sellOrder = findMatchingOrder(buyOrder);

                if (sellOrder == null) {
                    // Если подходящей заявки нет, возвращаем заявку в конец очереди
                    buyQueue.add(buyOrder);
                } else {
                    // Выполняем частичную или полную транзакцию
                    handlePartialOrFullTransaction(buyOrder, sellOrder);
                }

                // Обновляем время последней обработки


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private synchronized void handlePartialOrFullTransaction(Order buyOrder, Order sellOrder) {
        // Рассчитываем объем сделки (минимум между покупаемым и продаваемым количеством)
        long tradeAmount = Math.min(buyOrder.getAmount(), sellOrder.getAmount());
        long tradePrice = sellOrder.getPrice();
        long tradeValue = tradeAmount * tradePrice;

        // Проверяем, достаточно ли средств у покупателя
        if (balanceManager.hasSufficientFunds(buyOrder.getClient(), buyOrder.getCurrencyPair().getQuote(), tradeValue) &&
                (balanceManager.hasSufficientFunds(sellOrder.getClient(), sellOrder.getCurrencyPair().getBase(), tradeAmount))) {
            // Выполняем транзакциюы
            Transaction transaction = new Transaction(balanceManager);
            transaction.execute(buyOrder, sellOrder, tradeAmount, tradeValue);
            lastOrderTime = System.currentTimeMillis();

            // Уменьшаем количество в заявках
            buyOrder.setAmount(buyOrder.getAmount() - tradeAmount);
            sellOrder.setAmount(sellOrder.getAmount() - tradeAmount);

            // Если ордер частично выполнен, возвращаем остаток в очередь
            if (buyOrder.getAmount() > 0) {
                buyQueue.add(buyOrder);
            }
            else buyQueue.remove(buyOrder);
            if (sellOrder.getAmount() > 0) {
                sellQueue.add(sellOrder);
            }
            else sellQueue.remove(sellOrder);

        } else {
            // Если недостаточно средств, возвращаем заявки обратно в очереди
            buyQueue.add(buyOrder);
            sellQueue.add(sellOrder);
        }
    }

    private Order findMatchingOrder(Order buyOrder) throws InterruptedException {
        for (Order sellOrder : sellQueue) {
            if (isMatchingOrder(buyOrder, sellOrder)) {
                return sellOrder;
            }
        }
        return null;
    }

    private boolean isMatchingOrder(Order buyOrder, Order sellOrder) {
        return buyOrder.getCurrencyPair().equals(sellOrder.getCurrencyPair())
                && buyOrder.getPrice() >= sellOrder.getPrice();
    }

    public void stopProcessing() {
        running = false;
        this.interrupt();
    }

    public void waitForCompletion() throws InterruptedException {
        this.join();  // Ожидаем завершения потока
    }
}
