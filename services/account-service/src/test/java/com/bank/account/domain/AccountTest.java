package com.bank.account.domain;

import com.bank.common.identity.AccountId;
import com.bank.common.identity.TransferId;
import com.bank.common.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Pure domain test: no Spring, no mocks — the aggregate is exercised directly.
class AccountTest {

    private static final Currency TRY = Currency.getInstance("TRY");

    private static Money lira(String amount) {
        return Money.of(new BigDecimal(amount), TRY);
    }

    private static Account openAccount() {
        return Account.open(AccountId.newId(), "Alice", TRY);
    }

    // --- lifecycle happy path ---

    @Test
    void opensWithZeroBalanceAndDepositsAndWithdraws() {
        Account account = openAccount();

        assertThat(account.status()).isEqualTo(AccountStatus.OPEN);
        assertThat(account.balance()).isEqualTo(Money.zero(TRY));

        account.deposit(lira("100.00"));
        account.withdraw(lira("30.00"));

        assertThat(account.balance()).isEqualTo(lira("70.00"));
    }

    @Test
    void closesOnlyWhenBalanceIsZero() {
        Account account = openAccount();

        account.close();

        assertThat(account.status()).isEqualTo(AccountStatus.CLOSED);
    }

    // --- invariant: balance never goes negative ---

    @Test
    void withdrawRejectsWhenBalanceInsufficient() {
        Account account = openAccount();
        account.deposit(lira("100.00"));

        assertThatThrownBy(() -> account.withdraw(lira("150.00")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("100.00")
                .hasMessageContaining("150.00");

        // a failed withdrawal must not change state
        assertThat(account.balance()).isEqualTo(lira("100.00"));
    }

    // --- invariant: closed account is inert ---

    @Test
    void depositRejectsWhenAccountClosed() {
        Account account = openAccount();
        account.close();

        assertThatThrownBy(() -> account.deposit(lira("10.00")))
                .isInstanceOf(AccountClosedException.class);
    }

    // --- invariant: money cannot be stranded ---

    @Test
    void closeRejectsWhenBalanceRemains() {
        Account account = openAccount();
        account.deposit(lira("5.00"));

        assertThatThrownBy(account::close)
                .isInstanceOf(AccountNotEmptyException.class)
                .hasMessageContaining("5.00");

        assertThat(account.status()).isEqualTo(AccountStatus.OPEN);
    }

    // --- invariant: only positive amounts move money ---

    @Test
    void depositAndWithdrawRejectNonPositiveAmounts() {
        Account account = openAccount();

        assertThatThrownBy(() -> account.deposit(Money.zero(TRY)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.withdraw(lira("-5.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- reservation model: hold / capture / release (ADR-0002) ---

    @Test
    void holdReducesAvailableButNotBalance() {
        Account account = openAccount();
        account.deposit(lira("100.00"));

        account.hold(TransferId.newId(), lira("80.00"));

        assertThat(account.balance()).isEqualTo(lira("100.00"));       // money has not moved
        assertThat(account.availableBalance()).isEqualTo(lira("20.00")); // but it is promised
    }

    @Test
    void holdRejectsWhenAvailableInsufficient() {
        Account account = openAccount();
        account.deposit(lira("100.00"));
        account.hold(TransferId.newId(), lira("80.00"));

        // balance is still 100, but only 20 is unpromised
        assertThatThrownBy(() -> account.hold(TransferId.newId(), lira("30.00")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("20.00");
    }

    @Test
    void replayedHoldSignalsDuplicateNotInsufficient() {
        // A saga retry replays the same command; the answer must be "already done",
        // never "not enough money" — the two trigger opposite recovery paths.
        Account account = openAccount();
        account.deposit(lira("100.00"));
        TransferId transferId = TransferId.newId();
        account.hold(transferId, lira("80.00"));

        assertThatThrownBy(() -> account.hold(transferId, lira("80.00")))
                .isInstanceOf(DuplicateTransferAttemptException.class);
    }

    @Test
    void withdrawCannotTouchHeldFunds() {
        Account account = openAccount();
        account.deposit(lira("100.00"));
        account.hold(TransferId.newId(), lira("80.00"));

        assertThatThrownBy(() -> account.withdraw(lira("50.00")))
                .isInstanceOf(InsufficientBalanceException.class);

        account.withdraw(lira("20.00")); // the unpromised remainder is still spendable
        assertThat(account.balance()).isEqualTo(lira("80.00"));
    }

    @Test
    void captureMovesMoneyOutAndConsumesTheHold() {
        Account account = openAccount();
        account.deposit(lira("100.00"));
        TransferId transferId = TransferId.newId();
        account.hold(transferId, lira("80.00"));

        account.capture(transferId);

        assertThat(account.balance()).isEqualTo(lira("20.00"));          // now the money left
        assertThat(account.availableBalance()).isEqualTo(lira("20.00")); // available unchanged by capture

        // the hold is gone: capturing again must fail — compensation replay safety
        assertThatThrownBy(() -> account.capture(transferId))
                .isInstanceOf(HoldNotFoundException.class);
    }

    @Test
    void releaseRestoresAvailableWithoutTouchingBalance() {
        Account account = openAccount();
        account.deposit(lira("100.00"));
        TransferId transferId = TransferId.newId();
        account.hold(transferId, lira("80.00"));

        account.release(transferId);

        assertThat(account.balance()).isEqualTo(lira("100.00"));
        assertThat(account.availableBalance()).isEqualTo(lira("100.00"));
    }

    @Test
    void captureAndReleaseRejectUnknownTransfer() {
        Account account = openAccount();
        account.deposit(lira("100.00"));

        TransferId unknown = TransferId.newId();
        assertThatThrownBy(() -> account.capture(unknown)).isInstanceOf(HoldNotFoundException.class);
        assertThatThrownBy(() -> account.release(unknown)).isInstanceOf(HoldNotFoundException.class);
    }

    @Test
    void closeRejectsWhileMoneyIsPromised() {
        Account account = openAccount();
        account.deposit(lira("80.00"));
        account.hold(TransferId.newId(), lira("80.00"));

        assertThatThrownBy(account::close)
                .isInstanceOf(AccountNotEmptyException.class);
    }

    // --- entity identity (Evans ch. 5): same id = same account, whatever the state ---

    @Test
    void equalityIsByIdentityNotByState() {
        AccountId sharedId = AccountId.newId();
        Account a = Account.open(sharedId, "Alice", TRY);
        Account b = Account.open(sharedId, "Alice", TRY);
        a.deposit(lira("100.00")); // states now differ

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(openAccount()); // different id
    }
}
