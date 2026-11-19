-- "Rate us": one rating per user (they can change it later).
CREATE TABLE app_ratings (
    user_id    BIGINT        PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    stars      SMALLINT      NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment    VARCHAR(1000),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- "Contact us": messages to the NextRep team.
CREATE TABLE contact_messages (
    id         BIGSERIAL     PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    topic      VARCHAR(20)   NOT NULL CHECK (topic IN ('QUESTION', 'BUG', 'IDEA', 'OTHER')),
    message    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_contact_messages_user_created ON contact_messages (user_id, created_at DESC);
