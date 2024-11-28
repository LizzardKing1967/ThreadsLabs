package com.example.entyties;

public class CurrencyPair {
    private Currency base;
    private Currency quote;

    public CurrencyPair(Currency base, Currency quote) {
        this.base = base;
        this.quote = quote;
    }

    public Currency getBase() {
        return base;
    }

    public Currency getQuote() {
        return quote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyPair that = (CurrencyPair) o;
        return base == that.base && quote == that.quote;
    }

    @Override
    public int hashCode() {
        return 31 * base.hashCode() + quote.hashCode();
    }
}
