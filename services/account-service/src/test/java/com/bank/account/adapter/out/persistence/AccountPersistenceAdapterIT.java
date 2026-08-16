package com.bank.account.adapter.out.persistence;

import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.domain.Account;
import com.bank.account.domain.AccountStatus;
import com.bank.common.identity.AccountId;
import com.bank.common.identity.TransferId;
import com.bank.common.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip against a real PostgreSQL (Testcontainers): domain -> entity -> SQL insert
 * (two tables!) -> select -> entity -> restore() -> domain. Flyway migrations V1+V2 run
 * for real here — this test also proves the schema matches the entities.
 */
@SpringBootTest
class AccountPersistenceAdapterIT {

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
    AccountRepository accountRepository; // the port, not the adapter class

    private static Money lira(String amount) {
        return Money.of(new BigDecimal(amount), TRY);
    }

    @Test
    void savesAndRehydratesTheFullAggregateState() {
        Account account = Account.open(AccountId.newId(), "Alice", TRY);
        account.deposit(lira("100.00"));
        TransferId transferId = TransferId.newId();
        account.hold(transferId, lira("30.00"));

        accountRepository.save(account);

        Account reloaded = accountRepository.findById(account.accountId()).orElseThrow();

        assertThat(reloaded.ownerName()).isEqualTo("Alice");
        assertThat(reloaded.status()).isEqualTo(AccountStatus.OPEN);
        assertThat(reloaded.balance()).isEqualTo(lira("100.00"));
        // 70.00 proves the hold survived the trip through the account_hold table
        assertThat(reloaded.availableBalance()).isEqualTo(lira("70.00"));
    }

    @Test
    void findByIdReturnsEmptyForUnknownAccount() {
        assertThat(accountRepository.findById(AccountId.newId())).isEmpty();
    }
}
