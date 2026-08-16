package com.bank.account.domain;

import com.bank.common.identity.AccountId;
import com.bank.common.identity.TransferId;
import com.bank.common.money.Money;

import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Account aggregate root (Evans, DDD ch. 6): the consistency boundary around a customer's
 * balance. Every state change goes through a behavior method that enforces the invariants —
 * balance never goes negative, closed accounts accept no operations. No setters, ever.
 */
public class Account {

    private final AccountId accountId;
    private final String ownerName;
    private final Currency currency;
    private Money balance;
    private AccountStatus status;
    private Long version;
    private final Map<TransferId, Hold> holds;

    private Account(AccountId accountId, String ownerName, Currency currency,
                    Money balance, AccountStatus status, Map<TransferId, Hold> holds, Long version) {
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.ownerName = Objects.requireNonNull(ownerName, "ownerName");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.status = Objects.requireNonNull(status, "status");
        this.holds = Objects.requireNonNull(holds, "holds");
        this.version = version; // null = never persisted; owned and incremented by the persistence layer

        if (ownerName.isBlank()) {
            throw new IllegalArgumentException("ownerName must not be blank");
        }
    }

    /** The only way to bring a new account into existence — born OPEN with a zero balance. */
    public static Account open(AccountId accountId, String ownerName, Currency currency) {
        return new Account(accountId, ownerName, currency, Money.zero(currency),
                AccountStatus.OPEN, new HashMap<>(), null);
    }

    /** Rebuilds an account from persisted state. For the persistence adapter only. */
    public static Account restore(AccountId accountId, String ownerName, Currency currency,
                                  Money balance, AccountStatus status, Map<TransferId, Hold> holds, Long version) {

        return new Account(accountId, ownerName, currency, balance, status, holds, version);
    }

    public Money availableBalance() {
        Money held = holds.values().stream()
                .map(Hold::amount)
                .reduce(Money.zero(currency), Money::add);
        return balance.subtract(held);
    }

    public void hold(TransferId transferId, Money amount) {
        requireOpen();
        requirePositive(amount);

        if (holds.containsKey(transferId)) {
            throw new DuplicateTransferAttemptException(transferId);
        }

        final Money availableBalance = availableBalance();

        if (availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountId, availableBalance, amount);
        }

        holds.put(transferId, new Hold(transferId, amount));
    }

    public void capture(TransferId transferId) {
        if (!holds.containsKey(transferId)) {
            throw new HoldNotFoundException(transferId);
        }

        final Hold hold = holds.remove(transferId);
        balance = balance.subtract(hold.amount());
    }

    public void release(TransferId transferId) {
        if (!holds.containsKey(transferId)) {
            throw new HoldNotFoundException(transferId);
        }

        holds.remove(transferId);
    }

    public void deposit(Money amount) {
        requireOpen();
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void withdraw(Money amount) {
        requireOpen();
        requirePositive(amount);

        final Money availableBalance = availableBalance();

        if (availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountId, availableBalance, amount);
        }
        balance = balance.subtract(amount);
    }

    /** An account may only be closed once it holds no money — funds cannot vanish. */
    public void close() {
        requireOpen();
        if (!balance.isZero() || !holds.isEmpty()) {
            throw new AccountNotEmptyException(accountId, balance);
        }
        status = AccountStatus.CLOSED;
    }

    private void requireOpen() {
        if (status != AccountStatus.OPEN) {
            throw new AccountClosedException(accountId);
        }
    }

    private void requirePositive(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive, was " + amount);
        }
    }

    /** Read-only snapshot for the persistence adapter; nobody can mutate holds through this. */
    public List<Hold> activeHolds() {
        return List.copyOf(holds.values());
    }

    public AccountId accountId() {
        return accountId;
    }

    public String ownerName() {
        return ownerName;
    }

    public Currency currency() {
        return currency;
    }

    public Long version() {
        return version;
    }

    public Money balance() {
        return balance;
    }

    public AccountStatus status() {
        return status;
    }

    // Entity equality (Evans, DDD ch. 5): identity only — two snapshots of the same account
    // are the same account even if the balance differs between them.

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        return accountId.equals(((Account) other).accountId);
    }

    @Override
    public int hashCode() {
        return accountId.hashCode();
    }

    @Override
    public String toString() {
        return "Account[%s, %s, %s]".formatted(accountId, status, balance);
    }
}
