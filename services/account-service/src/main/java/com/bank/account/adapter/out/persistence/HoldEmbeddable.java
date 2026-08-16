package com.bank.account.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HoldEmbeddable {

    public HoldEmbeddable(UUID transferId, BigDecimal amount) {
        this.transferId = transferId;
        this.amount = amount;
    }

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
}
