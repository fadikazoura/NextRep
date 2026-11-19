CREATE TABLE workout_sessions (
    id           BIGSERIAL   PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_day_id  BIGINT      REFERENCES plan_days (id) ON DELETE SET NULL,
    started_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_workout_sessions_completed_after_start CHECK (completed_at IS NULL OR completed_at >= started_at)
);

-- At most one workout in progress per user.
CREATE UNIQUE INDEX uq_workout_sessions_one_open_per_user ON workout_sessions (user_id) WHERE completed_at IS NULL;
-- Workout history is always read per user, newest first.
CREATE INDEX idx_workout_sessions_user_started ON workout_sessions (user_id, started_at DESC);

CREATE TABLE workout_sets (
    id          BIGSERIAL    PRIMARY KEY,
    session_id  BIGINT       NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
    exercise_id BIGINT       NOT NULL REFERENCES exercises (id),
    set_number  SMALLINT     NOT NULL CHECK (set_number >= 1),
    weight_kg   NUMERIC(6,2) NOT NULL CHECK (weight_kg BETWEEN 0 AND 1000),
    reps        SMALLINT     NOT NULL CHECK (reps BETWEEN 0 AND 100),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_workout_sets_session_exercise_set UNIQUE (session_id, exercise_id, set_number)
);

-- Progression and personal records look up sets by exercise.
CREATE INDEX idx_workout_sets_exercise ON workout_sets (exercise_id);
