package com.bank.account.adapter.in.web;

import com.bank.account.adapter.in.web.api.model.AccountResponse;
import com.bank.account.adapter.in.web.api.model.MoneyRequest;
import com.bank.account.adapter.in.web.api.model.OpenAccountRequest;
import com.bank.account.application.port.in.AccountView;
import com.bank.account.application.port.in.DepositCommand;
import com.bank.account.application.port.in.OpenAccountCommand;
import com.bank.account.application.port.in.WithdrawCommand;
import com.bank.common.identity.AccountId;
import com.bank.common.money.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

/**
 * All wire <-> application translation in one place, now speaking the GENERATED models.
 * Amounts travel as strings on the wire (no floating point on the wire, ever) and become
 * BigDecimal-backed Money here.
 */
@Component
class AccountWebMapper {

    OpenAccountCommand toCommand(OpenAccountRequest request) {
        return new OpenAccountCommand(request.getOwnerName(), Currency.getInstance(request.getCurrency()));
    }

    DepositCommand toDepositCommand(UUID accountId, MoneyRequest request) {
        return new DepositCommand(new AccountId(accountId), toMoney(request));
    }

    WithdrawCommand toWithdrawCommand(UUID accountId, MoneyRequest request) {
        return new WithdrawCommand(new AccountId(accountId), toMoney(request));
    }

    AccountResponse toResponse(AccountView view) {
        return new AccountResponse()
                .id(view.accountId().value())
                .ownerName(view.ownerName())
                .currency(view.balance().currency().getCurrencyCode())
                .status(AccountResponse.StatusEnum.valueOf(view.status().name()))
                .balance(view.balance().amount().toPlainString())
                .availableBalance(view.availableBalance().amount().toPlainString());
    }

    private static Money toMoney(MoneyRequest request) {
        return new Money(new BigDecimal(request.getAmount()), Currency.getInstance(request.getCurrency()));
    }
}
