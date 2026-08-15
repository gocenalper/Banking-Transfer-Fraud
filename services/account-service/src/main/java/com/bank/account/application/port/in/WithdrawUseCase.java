package com.bank.account.application.port.in;

public interface WithdrawUseCase {

    void withdraw(WithdrawCommand command);
}
