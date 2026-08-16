package com.bank.account.application.port.in;

import com.bank.common.identity.AccountId;

public interface OpenAccountUseCase {

    AccountId openAccount(OpenAccountCommand command);
}
