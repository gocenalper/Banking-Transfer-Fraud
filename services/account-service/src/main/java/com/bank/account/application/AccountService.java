package com.bank.account.application;

import com.bank.account.application.port.in.AccountView;
import com.bank.account.application.port.in.DepositCommand;
import com.bank.account.application.port.in.DepositUseCase;
import com.bank.account.application.port.in.GetAccountQuery;
import com.bank.account.application.port.in.OpenAccountCommand;
import com.bank.account.application.port.in.OpenAccountUseCase;
import com.bank.account.application.port.in.WithdrawCommand;
import com.bank.account.application.port.in.WithdrawUseCase;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.domain.Account;
import com.bank.common.identity.AccountId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case orchestration only — load, tell, save. Business rules live in the aggregate;
 * if an `if` about money appears here, it is in the wrong place (Tell, Don't Ask).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AccountService implements OpenAccountUseCase, DepositUseCase, WithdrawUseCase, GetAccountQuery {

    private final AccountRepository accountRepository;

    @Override
    public AccountId openAccount(OpenAccountCommand command) {
        Account account = Account.open(AccountId.newId(), command.ownerName(), command.currency());
        accountRepository.save(account);
        return account.accountId();
    }

    @Override
    public void deposit(DepositCommand command) {
        Account account = load(command.accountId());
        account.deposit(command.amount());
        accountRepository.save(account);
    }

    @Override
    public void withdraw(WithdrawCommand command) {
        Account account = load(command.accountId());
        account.withdraw(command.amount());
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView getAccount(AccountId accountId) {
        return AccountView.from(load(accountId));
    }

    private Account load(AccountId accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
