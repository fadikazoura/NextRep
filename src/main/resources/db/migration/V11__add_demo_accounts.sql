-- Demo accounts are real users with sample data that are deleted automatically after a while.
-- NULL = a normal account that never expires.
ALTER TABLE users ADD COLUMN demo_expires_at TIMESTAMPTZ;

-- The cleanup job only ever looks at demo accounts, so a small partial index is enough.
CREATE INDEX idx_users_demo_expires_at ON users (demo_expires_at) WHERE demo_expires_at IS NOT NULL;
