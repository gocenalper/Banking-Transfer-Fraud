package com.bank.account.application.policy;

import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.domain.Account;
import com.bank.account.domain.Hold;
import com.bank.common.identity.AccountId;
import com.bank.common.identity.TransferId;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Deterministic conflict factory: behaves like the in-memory fake, but can be armed to
 * fail the next N save() calls with an optimistic-lock conflict.
 *
 * Fidelity matters: findById returns a SNAPSHOT, not the stored instance. A real
 * database rolls back the aborted transaction's changes; if the fake handed out its
 * live object, a failed save would still leave the caller's mutation visible to the
 * next load — and retry tests would see state that never got persisted.
 */
class FlakyAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> store = new HashMap<>();
    private int failuresRemaining = 0;
    private int saveAttempts = 0;

    void failNextSaves(int count) {
        this.failuresRemaining = count;
    }

    int saveAttempts() {
        return saveAttempts;
    }

    @Override
    public Optional<Account> findById(AccountId accountId) {
        return Optional.ofNullable(store.get(accountId)).map(FlakyAccountRepository::snapshot);
    }

    @Override
    public void save(Account account) {
        saveAttempts++;
        if (failuresRemaining > 0) {
            failuresRemaining--;
            throw new OptimisticLockingFailureException("simulated stale write on " + account.accountId());
        }
        store.put(account.accountId(), snapshot(account));
    }

    private static Account snapshot(Account account) {
        Map<TransferId, Hold> holds = account.activeHolds().stream()
                .collect(HashMap::new, (map, hold) -> map.put(hold.transferId(), hold), HashMap::putAll);
        return Account.restore(account.accountId(), account.ownerName(), account.currency(),
                account.balance(), account.status(), holds, account.version());
    }
}
