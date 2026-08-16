package com.bank.account.application.port.in;

import com.bank.common.identity.AccountId;

public interface GetAccountQuery {

    AccountView getAccount(AccountId accountId);
}
