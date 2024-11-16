package com.example;

import java.util.HashSet;
import java.util.Set;

public class CurrencyPairValidator {
    private static final Set<CurrencyPair> validPairs = new HashSet<>();

    static {
        validPairs.add(new CurrencyPair(Currency.USD, Currency.EUR));
        validPairs.add(new CurrencyPair(Currency.USD, Currency.RUB));
        validPairs.add(new CurrencyPair(Currency.USD, Currency.JPY));
        validPairs.add(new CurrencyPair(Currency.EUR, Currency.USD));
        validPairs.add(new CurrencyPair(Currency.EUR, Currency.RUB));
        validPairs.add(new CurrencyPair(Currency.EUR, Currency.JPY));
        validPairs.add(new CurrencyPair(Currency.RUB, Currency.USD));
        validPairs.add(new CurrencyPair(Currency.RUB, Currency.EUR));
        validPairs.add(new CurrencyPair(Currency.RUB, Currency.JPY));
        validPairs.add(new CurrencyPair(Currency.JPY, Currency.USD));
        validPairs.add(new CurrencyPair(Currency.JPY, Currency.EUR));
        validPairs.add(new CurrencyPair(Currency.JPY, Currency.RUB));

    }

    public static boolean isValidPair(CurrencyPair pair) {
        return validPairs.contains(pair);
    }
}