package com.bank.common.identity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountIdTest {

    @Test
    void ofAndToStringRoundTrip() {
        // the canonical string form must parse back to an equal id — ids travel
        // through URLs, event payloads and cache keys as bare strings
        AccountId id = AccountId.newId();

        assertThat(AccountId.of(id.toString())).isEqualTo(id);
    }

    @Test
    void toStringIsTheBareUuid() {
        UUID uuid = UUID.randomUUID();

        assertThat(new AccountId(uuid).toString()).isEqualTo(uuid.toString());
    }

    @Test
    void newIdGeneratesDistinctIds() {
        assertThat(AccountId.newId()).isNotEqualTo(AccountId.newId());
    }

    @Test
    void rejectsNullAndMalformedValues() {
        assertThatThrownBy(() -> new AccountId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("value");
        assertThatThrownBy(() -> AccountId.of("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
