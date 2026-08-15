package com.bank.account.domain.account;

import com.bank.common.identity.TransferId;

public class DuplicateTransferAttemptException extends RuntimeException {

    public DuplicateTransferAttemptException(TransferId transferId) {
        super("A transfer with id: %s has already been requested".formatted(transferId));
    }
}
