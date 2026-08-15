package com.bank.account.application.port.out;

import com.bank.account.domain.Account;
import com.bank.common.identity.AccountId;

import java.util.Optional;

/**
 * Output port (Clean Architecture ch. 22): the application layer owns this contract;
 * the persistence adapter implements it. The dependency arrow points inward.
 */
public interface AccountRepository {

    Optional<Account> findById(AccountId accountId);

    void save(Account account);
}
