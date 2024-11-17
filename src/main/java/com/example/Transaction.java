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
        // Покупатель
//        System.out.println("Performing transaction: Buy Order");
//        System.out.println("Before withdrawal - " + buyOrder.getClient().getName() + " balance of "
//                + buyOrder.getCurrencyPair().getQuote() + ": " + balanceManager.getBalance(buyOrder.getClient(), buyOrder.getCurrencyPair().getQuote()));
//
//        balanceManager.withdraw(buyOrder.getClient(), buyOrder.getCurrencyPair().getQuote(), tradeAmount);
//
//        System.out.println("After withdrawal - " + buyOrder.getClient().getName() + " balance of "
//                + buyOrder.getCurrencyPair().getQuote() + ": " + balanceManager.getBalance(buyOrder.getClient(), buyOrder.getCurrencyPair().getQuote()));
//
//        System.out.println("Before deposit - " + buyOrder.getClient().getName() + " balance of "
//                + buyOrder.getCurrencyPair().getBase() + ": " + balanceManager.getBalance(buyOrder.getClient(), buyOrder.getCurrencyPair().getBase()));
//
//        balanceManager.deposit(buyOrder.getClient(), buyOrder.getCurrencyPair().getBase(), tradeAmount);
//
//        System.out.println("After deposit - " + buyOrder.getClient().getName() + " balance of "
//                + buyOrder.getCurrencyPair().getBase() + ": " + balanceManager.getBalance(buyOrder.getClient(), buyOrder.getCurrencyPair().getBase()));
//
//        // Продавец
//        System.out.println("Performing transaction: Sell Order");
//        System.out.println("Before withdrawal - " + sellOrder.getClient().getName() + " balance of "
//                + sellOrder.getCurrencyPair().getBase() + ": " + balanceManager.getBalance(sellOrder.getClient(), sellOrder.getCurrencyPair().getBase()));
//
//        balanceManager.withdraw(sellOrder.getClient(), sellOrder.getCurrencyPair().getBase(), tradeAmount);
//
//        System.out.println("After withdrawal - " + sellOrder.getClient().getName() + " balance of "
//                + sellOrder.getCurrencyPair().getBase() + ": " + balanceManager.getBalance(sellOrder.getClient(), sellOrder.getCurrencyPair().getBase()));
//
//        System.out.println("Before deposit - " + sellOrder.getClient().getName() + " balance of "
//                + sellOrder.getCurrencyPair().getQuote() + ": " + balanceManager.getBalance(sellOrder.getClient(), sellOrder.getCurrencyPair().getQuote()));
//
//        balanceManager.deposit(sellOrder.getClient(), sellOrder.getCurrencyPair().getQuote(), tradeAmount);
//
//        System.out.println("After deposit - " + sellOrder.getClient().getName() + " balance of "
//                + sellOrder.getCurrencyPair().getQuote() + ": " + balanceManager.getBalance(sellOrder.getClient(), sellOrder.getCurrencyPair().getQuote()));
        balanceManager.withdraw(sellOrder.getClient(), sellOrder.getCurrencyPair().getBase(), tradeAmount);  // Уменьшение EUR
        balanceManager.deposit(sellOrder.getClient(), sellOrder.getCurrencyPair().getQuote(), tradeValue);
        balanceManager.withdraw(buyOrder.getClient(), buyOrder.getCurrencyPair().getQuote(), tradeValue);  // Снимаем средства с покупателя (цитируемая валюта)
        balanceManager.deposit(buyOrder.getClient(), buyOrder.getCurrencyPair().getBase(), tradeAmount);   // Депозитируем средства на покупателя (основная валюта)
    }

}
