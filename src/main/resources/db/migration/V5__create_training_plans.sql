CREATE TABLE training_plans (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    source     VARCHAR(10)  NOT NULL CHECK (source IN ('DEFAULT', 'AI')),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- At most one active plan per user; old plans stay as history.
CREATE UNIQUE INDEX uq_training_plans_one_active_per_user ON training_plans (user_id) WHERE active;

CREATE TABLE plan_days (
    id          BIGSERIAL   PRIMARY KEY,
    plan_id     BIGINT      NOT NULL REFERENCES training_plans (id) ON DELETE CASCADE,
    day_of_week VARCHAR(9)  NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
                                                            'FRIDAY', 'SATURDAY', 'SUNDAY')),
    name        VARCHAR(50) NOT NULL,
    CONSTRAINT uq_plan_days_plan_day UNIQUE (plan_id, day_of_week)
);

CREATE TABLE plan_exercises (
    id          BIGSERIAL PRIMARY KEY,
    plan_day_id BIGINT    NOT NULL REFERENCES plan_days (id) ON DELETE CASCADE,
    exercise_id BIGINT    NOT NULL REFERENCES exercises (id),
    position    SMALLINT  NOT NULL CHECK (position >= 1),
    sets        SMALLINT  NOT NULL CHECK (sets BETWEEN 1 AND 10),
    rep_min     SMALLINT  NOT NULL CHECK (rep_min BETWEEN 1 AND 50),
    rep_max     SMALLINT  NOT NULL CHECK (rep_max BETWEEN 1 AND 50),
    CONSTRAINT chk_plan_exercises_rep_range CHECK (rep_min <= rep_max),
    CONSTRAINT uq_plan_exercises_day_position UNIQUE (plan_day_id, position)
);
