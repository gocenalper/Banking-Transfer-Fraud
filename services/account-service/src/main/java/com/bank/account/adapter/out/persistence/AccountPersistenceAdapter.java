package com.bank.account.adapter.out.persistence;

import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.domain.Account;
import com.bank.account.domain.AccountStatus;
import com.bank.account.domain.Hold;
import com.bank.common.identity.AccountId;
import com.bank.common.identity.TransferId;
import com.bank.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implements the application's output port on top of Spring Data JPA and translates
 * between the two worlds: entity rows in, rehydrated aggregates out — and back.
 */
@Component
@RequiredArgsConstructor
class AccountPersistenceAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    @Override
    public Optional<Account> findById(AccountId accountId) {
        return jpaRepository.findById(accountId.value()).map(this::toDomain);
    }

    @Override
    public void save(Account account) {
        jpaRepository.save(toEntity(account));
    }

    private Account toDomain(AccountEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        Map<TransferId, Hold> holds = new HashMap<>();
        for (HoldEmbeddable hold : entity.getHolds()) {
            TransferId transferId = new TransferId(hold.getTransferId());
            // Currency lives on the account row only; every hold shares it by design.
            holds.put(transferId, new Hold(transferId, new Money(hold.getAmount(), currency)));
        }
        return Account.restore(
                new AccountId(entity.getId()),
                entity.getOwnerName(),
                currency,
                new Money(entity.getBalance(), currency),
                AccountStatus.valueOf(entity.getStatus()),
                holds);
    }

    private AccountEntity toEntity(Account account) {
        List<HoldEmbeddable> holds = account.activeHolds().stream()
                .map(hold -> new HoldEmbeddable(hold.transferId().value(), hold.amount().amount()))
                .toList();
        return new AccountEntity(
                account.accountId().value(),
                account.ownerName(),
                account.currency().getCurrencyCode(),
                account.balance().amount(),
                account.status().name(),
                holds);
    }
}
