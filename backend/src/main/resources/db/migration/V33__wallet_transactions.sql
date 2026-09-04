-- Append-only ledger for the per-business virtual wallet system. Escobar is a middleman between
-- businesses and creators: each BUSINESS user gets its own isolated balance, derived on read as
-- sum(CREDIT, CONFIRMED) - sum(DEBIT, CONFIRMED) over this table (no stored balance column, same
-- idiom as Campaign.maxBudgetInr's committed/remaining, which are also always computed on read).
-- Rows are never edited after insert except the status/confirmed_at/confirmed_by_user_id trio
-- (a PENDING top-up being confirmed/rejected) - amount/type/business/funding_source never change.
-- Corrections are always a brand new REVERSAL row referencing the original, never an edit to it.
CREATE TABLE wallet_transactions (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_id              BIGINT NOT NULL,
    type                     VARCHAR(10) NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    funding_source           VARCHAR(30) NOT NULL,
    amount_inr               DECIMAL(12,2) NOT NULL,
    note                     VARCHAR(500) NULL,
    performed_by_user_id     BIGINT NOT NULL,
    payout_id                BIGINT NULL,
    reversed_transaction_id  BIGINT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at             TIMESTAMP NULL,
    confirmed_by_user_id     BIGINT NULL,
    CONSTRAINT fk_wallet_tx_business FOREIGN KEY (business_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wallet_tx_performed_by FOREIGN KEY (performed_by_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wallet_tx_confirmed_by FOREIGN KEY (confirmed_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_wallet_tx_payout FOREIGN KEY (payout_id) REFERENCES payouts (id) ON DELETE SET NULL,
    CONSTRAINT fk_wallet_tx_reversed FOREIGN KEY (reversed_transaction_id) REFERENCES wallet_transactions (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_wallet_tx_business ON wallet_transactions (business_id);
CREATE INDEX idx_wallet_tx_business_status ON wallet_transactions (business_id, status);
CREATE INDEX idx_wallet_tx_status ON wallet_transactions (status);
CREATE INDEX idx_wallet_tx_created_at ON wallet_transactions (created_at);
