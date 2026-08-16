package com.bank.account.application;

import com.bank.account.application.port.in.DepositCommand;
import com.bank.account.application.port.in.AccountView;
import com.bank.account.application.port.in.OpenAccountCommand;
import com.bank.account.application.port.in.WithdrawCommand;
import com.bank.account.domain.AccountStatus;
import com.bank.account.domain.InsufficientBalanceException;
import com.bank.common.identity.AccountId;
import com.bank.common.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Use case tests without Spring and without a database: the fake repository stands in
// for the persistence adapter through the output port.
class AccountServiceTest {

    private static final Currency TRY = Currency.getInstance("TRY");

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(new InMemoryAccountRepository());
    }

    private static Money lira(String amount) {
        return Money.of(new BigDecimal(amount), TRY);
    }

    @Test
    void opensAccountAndFindsItThroughTheQuery() {
        AccountId id = service.openAccount(new OpenAccountCommand("Alice", TRY));

        AccountView view = service.getAccount(id);

        assertThat(view.ownerName()).isEqualTo("Alice");
        assertThat(view.status()).isEqualTo(AccountStatus.OPEN);
        assertThat(view.balance()).isEqualTo(Money.zero(TRY));
    }

    @Test
    void depositIsVisibleInTheView() {
        AccountId id = service.openAccount(new OpenAccountCommand("Alice", TRY));

        service.deposit(new DepositCommand(id, lira("100.00")));

        AccountView view = service.getAccount(id);
        assertThat(view.balance()).isEqualTo(lira("100.00"));
        assertThat(view.availableBalance()).isEqualTo(lira("100.00"));
    }

    @Test
    void unknownAccountFailsWithNotFound() {
        AccountId unknown = AccountId.newId();

        assertThatThrownBy(() -> service.deposit(new DepositCommand(unknown, lira("10.00"))))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> service.getAccount(unknown))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void domainRulesSurfaceUnchangedThroughTheUseCase() {
        // the service must not swallow or translate domain exceptions
        AccountId id = service.openAccount(new OpenAccountCommand("Alice", TRY));
        service.deposit(new DepositCommand(id, lira("50.00")));

        assertThatThrownBy(() -> service.withdraw(new WithdrawCommand(id, lira("80.00"))))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(service.getAccount(id).balance()).isEqualTo(lira("50.00"));
    }
}
