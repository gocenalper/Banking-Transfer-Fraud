package com.bank.account.application.policy;

import com.bank.account.application.AccountService;
import com.bank.account.application.port.in.DepositCommand;
import com.bank.account.application.port.in.DepositUseCase;
import com.bank.account.application.port.in.WithdrawCommand;
import com.bank.account.application.port.in.WithdrawUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@Primary
@RequiredArgsConstructor
public class RetryingAccountService implements DepositUseCase, WithdrawUseCase {

    private final AccountService delegate;
    private static final int MAX_RETRIES = 3;

    @Override
    public void deposit(DepositCommand command) {
        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            try {
                delegate.deposit(command);
                return;
            } catch (OptimisticLockingFailureException ex) {
                attempts++;
                if (attempts == MAX_RETRIES) {
                    throw ex;
                }

                sleepWithJitter(ex);
            }
        }
    }

    @Override
    public void withdraw(WithdrawCommand command) {
        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            try {
                delegate.withdraw(command);
                return;
            } catch (OptimisticLockingFailureException ex) {
                attempts++;
                if (attempts == MAX_RETRIES) {
                    throw ex;
                }

                sleepWithJitter(ex);
            }
        }
    }

    private static void sleepWithJitter(OptimisticLockingFailureException ex) {
        try {
            final long jitter = ThreadLocalRandom.current().nextLong(20, 40);
            Thread.sleep(jitter);
        } catch (InterruptedException intEx) {
            Thread.currentThread().interrupt();
            throw ex;
        }
    }
}
