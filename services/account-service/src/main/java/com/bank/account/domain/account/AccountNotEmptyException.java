package com.bank.account.domain.account;

import com.bank.common.identity.AccountId;
import com.bank.common.money.Money;

/** Closing an account that still holds funds would make that money unreachable. */
public class AccountNotEmptyException extends RuntimeException {

    public AccountNotEmptyException(AccountId accountId, Money balance) {
        super("Account %s still holds %s; balance must be zero to close".formatted(accountId, balance));
    }
}
