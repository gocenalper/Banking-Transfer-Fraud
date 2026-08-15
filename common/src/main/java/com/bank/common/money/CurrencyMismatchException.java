package com.bank.common.money;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String currency, String otherCurrency) {
        super("Currency mismatch: %s vs %s".formatted(currency, otherCurrency));
    }
}
