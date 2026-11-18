CREATE TABLE body_weight_entries (
                                     id          BIGSERIAL    PRIMARY KEY,
                                     user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                     weight_kg   NUMERIC(5,2) NOT NULL CHECK (weight_kg BETWEEN 25 AND 400),
                                     recorded_on DATE         NOT NULL,
                                     created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                     CONSTRAINT uq_body_weight_user_day UNIQUE (user_id, recorded_on)
);