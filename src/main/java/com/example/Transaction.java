package com.example;

public class Transaction {
    private final ClientBalanceManager balanceManager;

    public Transaction(ClientBalanceManager balanceManager) {
        this.balanceManager = balanceManager;
    }

    /**
     * Частичное выполнение транзакции между двумя заявками.
     */
    public synchronized void execute(Order buyOrder, Order sellOrder, long tradeAmount, long tradeValue) {
        if (tradeAmount == 0) {
            return;
        }
        performTransaction(buyOrder, sellOrder, tradeAmount, tradeValue);

        System.out.println("transaction complete: " + tradeAmount + " "
                + sellOrder.getCurrencyPair().getBase() + " @ " + sellOrder.getPrice()
                + " between " + buyOrder.getClient() + " and " + sellOrder.getClient());
    }

    /**
     * Общая логика изменения балансов.
     */
    private void performTransaction(Order buyOrder, Order sellOrder, long tradeAmount, long tradeValue) {
        balanceManager.withdraw(sellOrder.getClient(), sellOrder.getCurrencyPair().getBase(), tradeAmount);  // Уменьшение EUR
        balanceManager.deposit(sellOrder.getClient(), sellOrder.getCurrencyPair().getQuote(), tradeValue);
        balanceManager.withdraw(buyOrder.getClient(), buyOrder.getCurrencyPair().getQuote(), tradeValue);  // Снимаем средства с покупателя (цитируемая валюта)
        balanceManager.deposit(buyOrder.getClient(), buyOrder.getCurrencyPair().getBase(), tradeAmount);   // Депозитируем средства на покупателя (основная валюта)
    }

}
