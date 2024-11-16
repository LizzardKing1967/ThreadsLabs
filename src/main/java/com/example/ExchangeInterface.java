package com.example;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ExchangeInterface {
    /**
     * Создает клиента с указанным именем.
     * @param name имя клиента
     * @return созданный объект клиента
     */
    Client createClient(String name);

    /**
     * Вносит депозит на счет клиента.
     * @param client клиент
     * @param currency валюта депозита
     * @param amount сумма депозита
     */
     CompletableFuture<Void> deposit(Client client, Currency currency, Long amount);

    /**
     * Снимает средства со счета клиента.
     * @param client клиент
     * @param currency валюта вывода
     * @param amount сумма вывода
     */
    CompletableFuture<Void> withdraw(Client client, Currency currency, Long amount);

    /**
     * Создает ордер на покупку.
     * @param client клиент
     * @param pair валютная пара
     * @param price цена покупки
     * @param amount количество базовой валюты
     * @return созданный ордер
     */
    CompletableFuture<Order> createBuyOrder(Client client, CurrencyPair pair, Long price, Long amount);

    /**
     * Создает ордер на продажу.
     * @param client клиент
     * @param pair валютная пара
     * @param price цена продажи
     * @param amount количество базовой валюты
     * @return созданный ордер
     */
    CompletableFuture<Order> createSellOrder(Client client, CurrencyPair pair, Long price, Long amount);

    /**
     * Возвращает список открытых ордеров.
     * @return список ордеров
     */
    CompletableFuture<List<Order>> getOpenOrders();

    /**
     * Получает текущее состояние клиента.
     * @param client клиент
     * @return объект состояния клиента
     */
    CompletableFuture<ClientState> getClientState(Client client);

    /**
     * Возвращает менеджер для работы с балансами клиентов.
     * @return объект менеджера балансов
     */
    ClientBalanceManager getClientBalanceManager();
}
