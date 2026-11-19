-- Account details: a public username, an optional phone number, the avatar choice,
-- and a token version that lets the user log out of every device at once.
ALTER TABLE users
    ADD COLUMN username      VARCHAR(20),
    ADD COLUMN phone         VARCHAR(16),
    ADD COLUMN avatar_preset VARCHAR(30),
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

-- Existing users get a username made from their email (letters, digits, '.', '_') plus their id, so it is unique.
UPDATE users
SET username = LEFT(COALESCE(NULLIF(regexp_replace(lower(split_part(email, '@', 1)), '[^a-z0-9._]', '', 'g'), ''), 'user'), 12)
               || '_' || id;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL,
    ADD CONSTRAINT ck_users_username CHECK (username ~ '^[A-Za-z0-9._]{3,20}$'),
    ADD CONSTRAINT ck_users_phone CHECK (phone IS NULL OR phone ~ '^\+[1-9][0-9]{6,14}$');

-- "Fadi" and "fadi" are the same username.
CREATE UNIQUE INDEX uq_users_username_lower ON users (lower(username));

-- Uploaded avatar pictures live in their own table, so loading a user never loads the image bytes.
CREATE TABLE user_avatars (
    user_id      BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    content_type VARCHAR(20) NOT NULL CHECK (content_type IN ('image/png', 'image/jpeg', 'image/webp')),
    data         BYTEA       NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
