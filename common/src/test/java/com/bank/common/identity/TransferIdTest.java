package com.bank.common.identity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferIdTest {

    @Test
    void ofAndToStringRoundTrip() {
        TransferId id = TransferId.newId();

        assertThat(TransferId.of(id.toString())).isEqualTo(id);
    }

    @Test
    void toStringIsTheBareUuid() {
        UUID uuid = UUID.randomUUID();

        assertThat(new TransferId(uuid).toString()).isEqualTo(uuid.toString());
    }

    @Test
    void newIdGeneratesDistinctIds() {
        assertThat(TransferId.newId()).isNotEqualTo(TransferId.newId());
    }

    @Test
    void rejectsNullAndMalformedValues() {
        assertThatThrownBy(() -> new TransferId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
        assertThatThrownBy(() -> TransferId.of("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
