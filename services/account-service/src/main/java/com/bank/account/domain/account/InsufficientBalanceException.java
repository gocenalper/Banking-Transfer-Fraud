package com.bank.account.domain.account;

import com.bank.common.identity.AccountId;
import com.bank.common.money.Money;

/** Withdrawing more than the balance would create money out of nothing — a core invariant. */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(AccountId accountId, Money balance, Money requested) {
        super("Account %s holds %s, cannot withdraw %s".formatted(accountId, balance, requested));
    }
}
