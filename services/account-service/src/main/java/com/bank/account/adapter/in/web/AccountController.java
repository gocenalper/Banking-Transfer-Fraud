package com.bank.account.adapter.in.web;

import com.bank.account.adapter.in.web.api.AccountsApi;
import com.bank.account.adapter.in.web.api.model.AccountResponse;
import com.bank.account.adapter.in.web.api.model.MoneyRequest;
import com.bank.account.adapter.in.web.api.model.OpenAccountRequest;
import com.bank.account.application.port.in.DepositUseCase;
import com.bank.account.application.port.in.GetAccountQuery;
import com.bank.account.application.port.in.OpenAccountUseCase;
import com.bank.account.application.port.in.WithdrawUseCase;
import com.bank.common.identity.AccountId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Inbound web adapter implementing the GENERATED contract interface: if this class stops
 * matching the OpenAPI yaml, the build breaks — the contract is enforced by the compiler.
 * Routing only; translation lives in AccountWebMapper, business rules live far deeper.
 */
@RestController
@RequiredArgsConstructor
class AccountController implements AccountsApi {

    private final OpenAccountUseCase openAccount;
    private final DepositUseCase deposit;
    private final WithdrawUseCase withdraw;
    private final GetAccountQuery getAccount;
    private final AccountWebMapper mapper;

    @Override
    public ResponseEntity<AccountResponse> openAccount(OpenAccountRequest request) {
        AccountId id = openAccount.openAccount(mapper.toCommand(request));
        AccountResponse body = mapper.toResponse(getAccount.getAccount(id));
        return ResponseEntity.created(URI.create("/api/accounts/" + id)).body(body);
    }

    @Override
    public ResponseEntity<AccountResponse> getAccount(UUID id) {
        return ResponseEntity.ok(mapper.toResponse(getAccount.getAccount(new AccountId(id))));
    }

    @Override
    public ResponseEntity<AccountResponse> deposit(UUID id, MoneyRequest request) {
        deposit.deposit(mapper.toDepositCommand(id, request));
        return ResponseEntity.ok(mapper.toResponse(getAccount.getAccount(new AccountId(id))));
    }

    @Override
    public ResponseEntity<AccountResponse> withdraw(UUID id, MoneyRequest request) {
        withdraw.withdraw(mapper.toWithdrawCommand(id, request));
        return ResponseEntity.ok(mapper.toResponse(getAccount.getAccount(new AccountId(id))));
    }
}
