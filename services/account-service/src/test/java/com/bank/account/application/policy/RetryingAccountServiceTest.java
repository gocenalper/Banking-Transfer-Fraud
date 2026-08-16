package com.bank.account.application.policy;

import com.bank.account.application.AccountService;
import com.bank.account.application.port.in.DepositCommand;
import com.bank.account.application.port.in.OpenAccountCommand;
import com.bank.account.application.port.in.WithdrawCommand;
import com.bank.account.domain.InsufficientBalanceException;
import com.bank.common.identity.AccountId;
import com.bank.common.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the retry policy — no Spring, no database, no threads. The flaky fake
 * produces conflicts on demand, so every retry path is exercised deterministically.
 */
class RetryingAccountServiceTest {

    private static final Currency TRY = Currency.getInstance("TRY");

    private FlakyAccountRepository repository;
    private AccountService delegate;
    private RetryingAccountService retrying;
    private AccountId accountId;

    @BeforeEach
    void setUp() {
        repository = new FlakyAccountRepository();
        delegate = new AccountService(repository);
        retrying = new RetryingAccountService(delegate);

        // set the stage through the plain delegate — no failures armed yet
        accountId = delegate.openAccount(new OpenAccountCommand("Alice", TRY));
        delegate.deposit(new DepositCommand(accountId, lira("100.00")));
    }

    private static Money lira(String amount) {
        return Money.of(new BigDecimal(amount), TRY);
    }

    @Test
    void succeedsAfterTransientConflicts() {
        repository.failNextSaves(2); // attempts 1 and 2 conflict, attempt 3 wins

        retrying.withdraw(new WithdrawCommand(accountId, lira("60.00")));

        // the caller never saw a conflict, and the money moved exactly once
        assertThat(delegate.getAccount(accountId).balance()).isEqualTo(lira("40.00"));
    }

    @Test
    void givesUpAfterMaxAttemptsAndSurfacesTheConflict() {
        repository.failNextSaves(3); // every attempt conflicts

        assertThatThrownBy(() -> retrying.withdraw(new WithdrawCommand(accountId, lira("60.00"))))
                .isInstanceOf(OptimisticLockingFailureException.class);

        // nothing was persisted — the balance is untouched
        assertThat(delegate.getAccount(accountId).balance()).isEqualTo(lira("100.00"));
    }

    @Test
    void doesNotRetryBusinessRefusals() {
        int savesBefore = repository.saveAttempts();

        assertThatThrownBy(() -> retrying.withdraw(new WithdrawCommand(accountId, lira("500.00"))))
                .isInstanceOf(InsufficientBalanceException.class);

        // the refusal happened in the domain, before any save — and was NOT retried:
        // a deterministic business answer does not change on a second attempt
        assertThat(repository.saveAttempts()).isEqualTo(savesBefore);
    }
}
