package com.bank.account.application.port.in;

import com.bank.account.domain.Account;
import com.bank.account.domain.AccountStatus;
import com.bank.common.identity.AccountId;
import com.bank.common.money.Money;

/**
 * Immutable read model returned by queries. The aggregate itself never leaves the
 * application layer: leaking it would hand every caller a mutation door (account.close()),
 * turning a query into a command channel.
 */
public record AccountView(AccountId accountId, String ownerName, AccountStatus status,
                          Money balance, Money availableBalance) {

    public static AccountView from(Account account) {
        return new AccountView(account.accountId(), account.ownerName(), account.status(),
                account.balance(), account.availableBalance());
    }
}
