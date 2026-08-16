package com.bank.account.application.port.in;

import java.util.Currency;
import java.util.Objects;

public record OpenAccountCommand(String ownerName, Currency currency) {

    public OpenAccountCommand {
        Objects.requireNonNull(ownerName, "ownerName");
        Objects.requireNonNull(currency, "currency");
        if (ownerName.isBlank()) {
            throw new IllegalArgumentException("ownerName must not be blank");
        }
    }
}
