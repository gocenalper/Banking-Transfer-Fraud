CREATE TABLE account (
                         id         UUID          PRIMARY KEY,
                         owner_name TEXT          NOT NULL,
                         currency   VARCHAR(3)    NOT NULL,
                         balance    NUMERIC(19,4) NOT NULL,
                         status     VARCHAR(16)   NOT NULL
);