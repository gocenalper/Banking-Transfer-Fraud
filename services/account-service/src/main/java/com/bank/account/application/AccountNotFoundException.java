package com.bank.account.application;

import com.bank.common.identity.AccountId;

/**
 * Lives in the application layer, not the domain: "not found" is a flow concern of
 * use cases (lookups), not a business rule of the aggregate.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(AccountId accountId) {
        super("Account %s not found".formatted(accountId));
    }
}
