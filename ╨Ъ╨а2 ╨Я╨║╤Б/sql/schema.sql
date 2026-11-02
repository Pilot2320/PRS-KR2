-- Электронный кошелёк: клиенты, кошельки, операции

DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS wallets;
DROP TABLE IF EXISTS clients;

CREATE TABLE clients (
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(150)  NOT NULL,
    email         VARCHAR(150)  NOT NULL UNIQUE,
    phone         VARCHAR(20)   NOT NULL,
    registered_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallets (
    id             BIGSERIAL PRIMARY KEY,
    client_id      BIGINT         NOT NULL,
    wallet_number  VARCHAR(20)    NOT NULL UNIQUE,
    card_number    VARCHAR(16)    NOT NULL UNIQUE,
    holder_name    VARCHAR(150)   NOT NULL,
    payment_system VARCHAR(20)    NOT NULL,
    expiry_date    DATE           NOT NULL,
    currency       VARCHAR(3)     NOT NULL,
    balance        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status         VARCHAR(20)    NOT NULL,
    daily_limit    NUMERIC(15, 2) NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallets_client
        FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT uq_wallets_client
        UNIQUE (client_id),
    CONSTRAINT chk_wallets_payment_system
        CHECK (payment_system IN ('MIR')),
    CONSTRAINT chk_wallets_currency
        CHECK (currency IN ('RUB')),
    CONSTRAINT chk_wallets_balance
        CHECK (balance >= 0),
    CONSTRAINT chk_wallets_status
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT chk_wallets_daily_limit
        CHECK (daily_limit = 100000)
);

CREATE TABLE transactions (
    id           BIGSERIAL PRIMARY KEY,
    wallet_id    BIGINT         NOT NULL,
    type         VARCHAR(20)    NOT NULL,
    amount       NUMERIC(15, 2) NOT NULL,
    description  VARCHAR(500)   NOT NULL,
    counterparty VARCHAR(200)   NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_transactions_type
        CHECK (type IN ('TOP_UP', 'TRANSFER', 'WITHDRAWAL')),
    CONSTRAINT chk_transactions_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_transactions_wallet_id ON transactions (wallet_id);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
