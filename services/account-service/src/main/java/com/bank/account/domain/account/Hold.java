package com.bank.account.domain.account;

import com.bank.common.identity.TransferId;
import com.bank.common.money.Money;

import java.util.Objects;


public record Hold(TransferId transferId, Money amount) {

    public Hold {
        Objects.requireNonNull(transferId, "transferId");
        Objects.requireNonNull(amount, "amount");

        if (amount.isNegative()) {throw new IllegalArgumentException("amount cannot be negative");}
    }
}
