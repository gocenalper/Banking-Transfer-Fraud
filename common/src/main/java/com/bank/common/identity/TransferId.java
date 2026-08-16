package com.bank.common.identity;

import java.util.Objects;
import java.util.UUID;

public record TransferId(UUID value) {

    public TransferId {
        Objects.requireNonNull(value, "value");
    }

    public static TransferId newId() {
        return new TransferId(UUID.randomUUID());
    }

    public static TransferId of(String value) {
        return new TransferId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
