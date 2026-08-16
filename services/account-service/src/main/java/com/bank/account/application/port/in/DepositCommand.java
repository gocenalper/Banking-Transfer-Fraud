package com.bank.account.application.port.in;

import com.bank.common.identity.AccountId;
import com.bank.common.money.Money;

import java.util.Objects;

public record DepositCommand(AccountId accountId, Money amount) {

    public DepositCommand {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(amount, "amount");
    }
}
