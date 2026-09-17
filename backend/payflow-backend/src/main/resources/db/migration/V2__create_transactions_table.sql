CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);