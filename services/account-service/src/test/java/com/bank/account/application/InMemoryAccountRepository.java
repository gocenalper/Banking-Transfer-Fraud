package com.bank.account.application;

import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.domain.Account;
import com.bank.common.identity.AccountId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Hand-written fake, not a mock: it honors the port's contract (what you save you can
 * find), so tests exercise behaviour instead of interactions — refactoring the service
 * does not break them.
 */
class InMemoryAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> store = new HashMap<>();

    @Override
    public Optional<Account> findById(AccountId accountId) {
        return Optional.ofNullable(store.get(accountId));
    }

    @Override
    public void save(Account account) {
        store.put(account.accountId(), account);
    }
}
