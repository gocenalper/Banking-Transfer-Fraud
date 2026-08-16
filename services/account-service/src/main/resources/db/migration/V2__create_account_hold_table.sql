CREATE TABLE account_hold (
                              account_id  UUID          NOT NULL REFERENCES account (id),
                              transfer_id UUID          NOT NULL,
                              amount      NUMERIC(19,4) NOT NULL,
                              PRIMARY KEY (account_id, transfer_id)
);