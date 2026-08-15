package com.bank.account.domain.account;

import com.bank.common.identity.AccountId;

/** A closed account is the end of its lifecycle: no deposits, withdrawals or re-closing. */
public class AccountClosedException extends RuntimeException {

    public AccountClosedException(AccountId accountId) {
        super("Account %s is not open".formatted(accountId));
    }
}
