package com.bank.common.identity;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value, "value");
    }

    public static AccountId newId() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(String value) {
        return new AccountId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
