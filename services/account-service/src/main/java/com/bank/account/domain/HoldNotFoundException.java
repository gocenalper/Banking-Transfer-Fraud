package com.bank.account.domain;

import com.bank.common.identity.TransferId;

public class HoldNotFoundException extends RuntimeException {

    public HoldNotFoundException(TransferId transferId) {
        super("Hold with id %s could not be found".formatted(transferId));
    }
}
