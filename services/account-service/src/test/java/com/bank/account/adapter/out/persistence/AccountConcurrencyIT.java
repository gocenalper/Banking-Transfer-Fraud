package com.bank.account.adapter.out.persistence;

import com.bank.account.application.port.in.AccountView;
import com.bank.account.application.port.in.DepositCommand;
import com.bank.account.application.port.in.DepositUseCase;
import com.bank.account.application.port.in.GetAccountQuery;
import com.bank.account.application.port.in.OpenAccountCommand;
import com.bank.account.application.port.in.OpenAccountUseCase;
import com.bank.account.application.port.in.WithdrawCommand;
import com.bank.account.application.port.in.WithdrawUseCase;
import com.bank.account.domain.InsufficientBalanceException;
import com.bank.common.identity.AccountId;
import com.bank.common.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof of the full concurrency stack: two threads race the withdraw use case
 * over one account. History of this test:
 *  1. Before @Version it reliably lost money (success=2, 60.00 created out of thin air).
 *  2. With @Version the stale writer surfaced an optimistic-lock conflict.
 *  3. With the retry decorator (@Primary) the conflict is absorbed inside the
 *     application: the loser retries on fresh data and gets the honest business answer —
 *     insufficient balance. Callers see business outcomes, never locking mechanics.
 *
 * NO @Transactional on this test: each use case call must open its own transaction,
 * exactly as two concurrent HTTP requests would.
 */
@SpringBootTest
class AccountConcurrencyIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:17-alpine");
        }
    }

    private static final Currency TRY = Currency.getInstance("TRY");

    @Autowired
    OpenAccountUseCase openAccount;
    @Autowired
    DepositUseCase deposit;
    @Autowired
    WithdrawUseCase withdraw;
    @Autowired
    GetAccountQuery getAccount;

    private static Money lira(String amount) {
        return Money.of(new BigDecimal(amount), TRY);
    }

    @Test
    void concurrentWithdrawalsNeverLoseMoneyAndConflictsStayInternal() throws InterruptedException {
        int rounds = 10;

        for (int round = 1; round <= rounds; round++) {
            RoundResult result = runOneRound();
            System.out.printf("ROUND %2d: success=%d rejected=%d conflicts=%d remaining=%s accountedFor=%s%n",
                    round, result.successes(), result.rejections(), result.conflicts(),
                    result.remaining(), result.accountedFor());

            // The one invariant that must hold under EVERY interleaving:
            // money handed out + money remaining == money that ever existed.
            assertThat(result.accountedFor())
                    .as("no money created or destroyed in round %d", round)
                    .isEqualByComparingTo("100.00");
            // 100 can only fund one 60.00 withdrawal, whatever the timing.
            assertThat(result.successes()).as("exactly one withdrawal wins").isEqualTo(1);
            // The loser always ends with the honest business answer...
            assertThat(result.rejections()).as("loser is politely refused").isEqualTo(1);
            // ...because the retry decorator absorbs version conflicts internally.
            assertThat(result.conflicts())
                    .as("locking mechanics never leak to the caller")
                    .isZero();
        }
    }

    private record RoundResult(int successes, int rejections, int conflicts,
                               BigDecimal remaining, BigDecimal accountedFor) {
    }

    private RoundResult runOneRound() throws InterruptedException {
        AccountId accountId = openAccount.openAccount(new OpenAccountCommand("Alice", TRY));
        deposit.deposit(new DepositCommand(accountId, lira("100.00")));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // line up at the gate so both fire together
                    withdraw.withdraw(new WithdrawCommand(accountId, lira("60.00")));
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException e) {
                    rejectedCount.incrementAndGet(); // loser read fresh data and was refused
                } catch (OptimisticLockingFailureException e) {
                    conflictCount.incrementAndGet(); // loser wrote from a stale read and was caught
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(doneLatch.await(10, TimeUnit.SECONDS)).as("racers finished in time").isTrue();
        executor.shutdown();

        AccountView view = getAccount.getAccount(accountId);
        BigDecimal remaining = view.balance().amount();
        BigDecimal withdrawn = new BigDecimal("60.00").multiply(BigDecimal.valueOf(successCount.get()));
        return new RoundResult(successCount.get(), rejectedCount.get(), conflictCount.get(),
                remaining, withdrawn.add(remaining));
    }
}
